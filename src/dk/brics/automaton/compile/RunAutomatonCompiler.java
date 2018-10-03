package dk.brics.automaton.compile;

import dk.brics.automaton.CompiledRunAutomaton;
import dk.brics.automaton.RunAutomaton;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static dk.brics.automaton.compile.ASMUtils.getFullyQualifiedName;
import static org.objectweb.asm.Opcodes.*;

public class RunAutomatonCompiler {
    private static int classNum = 0;
    private static final DynamicClassLoader classLoader = new DynamicClassLoader();

    private static String charAtDesc = new ASMMethodSignatureBuilder().addParam(int.class).addRet(char.class).toString();
    private static String lengthDesc = new ASMMethodSignatureBuilder().addRet(int.class).toString();

    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);

    private static String insnToString(AbstractInsnNode insn){
        insn.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    private static void printClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode,0);
        @SuppressWarnings("unchecked")
        final List<MethodNode> methods = classNode.methods;
        for(MethodNode m: methods){
            InsnList inList = m.instructions;
            System.out.println(m.name);
            for(int i = 0; i< inList.size(); i++){
                System.out.print(insnToString(inList.get(i)));
            }
        }
    }


    public static CompiledRunAutomaton compile(RunAutomaton runAutomaton) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        ClassWriter classVisitor = new ClassWriter(0) {
        };

        CheckClassAdapter checkClass = new CheckClassAdapter(classVisitor, true);

        ClassVisitor cv = checkClass;

        String className = "CompiledPredicate" + (classNum++);

        cv.visit(V1_8, ACC_PUBLIC, className, null, getFullyQualifiedName(Object.class),
                new String[]{getFullyQualifiedName(CompiledRunAutomaton.class)});

        MethodVisitor constructor = cv.visitMethod(ACC_PUBLIC, "<init>",
                new ASMMethodSignatureBuilder().toString(), null, null);

        constructor.visitCode();
        //super()
        constructor.visitVarInsn(ALOAD, 0);
        ASMMethodSignatureBuilder superConstructorDesc = new ASMMethodSignatureBuilder();
        constructor.visitMethodInsn(INVOKESPECIAL, getFullyQualifiedName(Object.class), "<init>", superConstructorDesc.toString(), false);
        constructor.visitInsn(RETURN);

        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        final int numStates = runAutomaton.getSize() + 1;

        Map<Integer, Label> stateToLabel = new HashMap<>();
        for (int i = 0; i < numStates; i++) {
            stateToLabel.put(i, new Label());
        }

        String runMethodDesc = new ASMMethodSignatureBuilder().addParam(String.class).addRet(boolean.class).toString();
        MethodVisitor rm = cv.visitMethod(ACC_PUBLIC,
                "run", runMethodDesc, null, null);

        rm.visitCode();
        //stack: Current Character -> Current Character| String -> Current Character | Character

        rm.visitInsn(ICONST_M1);
        rm.visitInsn(ICONST_0);
        rm.visitJumpInsn(GOTO, stateToLabel.get(runAutomaton.getInitialState()));

        Integer[] states = new Integer[numStates];

        for (int i = 0; i < numStates; i++) {
            states[i] = i;
        }

        for (int i = 0; i < numStates; i++) {
            rm.visitLabel(stateToLabel.get(i));
            rm.visitFrame(F_FULL, 2, new String[]{className, getFullyQualifiedName(String.class)}, 2, new Object[]{INTEGER, INTEGER});

            if (i == numStates - 1) {
                rm.visitInsn(ICONST_0);
                rm.visitInsn(IRETURN);
                break;
            }

            @SuppressWarnings("unchecked") final List<Character>[] inverseTransitions = (List<Character>[]) new List[numStates];

            for (int j = 0; j < numStates; j++) {
                inverseTransitions[j] = new ArrayList<>();
            }

            for (int j = Character.MIN_VALUE; j <= Character.MAX_VALUE; j++) {
                int nextState = runAutomaton.step(i, (char)j);
                if (nextState == -1) {
                    nextState = numStates - 1;
                }
                inverseTransitions[nextState].add((char)j);
            }

            Comparator<Integer> comp = Comparator.comparingInt(a -> inverseTransitions[a].size());

            Arrays.sort(states, comp);



            rm.visitInsn(POP);
            rm.visitInsn(ICONST_1);
            rm.visitInsn(IADD);

            // Need to check if over length of stirng, if is check if current state final, if is return true, else return false
            Label passLength = new Label();
            rm.visitInsn(DUP);
            rm.visitVarInsn(ALOAD, 1);
            rm.visitMethodInsn(INVOKEVIRTUAL, getFullyQualifiedName(String.class), "length", lengthDesc, false);
            rm.visitJumpInsn(IF_ICMPLT, passLength);
            if (runAutomaton.isAccept(i)) {
                rm.visitInsn(ICONST_1);
            } else {
                rm.visitInsn(ICONST_0);
            }
            rm.visitInsn(IRETURN);
            rm.visitLabel(passLength);
            rm.visitFrame(F_FULL, 2, new String[]{className, getFullyQualifiedName(String.class)}, 1, new Object[]{INTEGER});

            rm.visitInsn(DUP);
            rm.visitVarInsn(ALOAD, 1);
            rm.visitInsn(SWAP);
            rm.visitMethodInsn(INVOKEVIRTUAL, getFullyQualifiedName(String.class), "charAt", charAtDesc, false);


            for (int j = 0; j < states.length; j++) {
                int state = states[j];
                if (j < states.length - 1) {
                    for (Character c : inverseTransitions[state]) {
                        rm.visitInsn(DUP);
                        rm.visitLdcInsn((int) c);
                        rm.visitJumpInsn(IF_ICMPEQ, stateToLabel.get(state));
                    }
                } else {
                    rm.visitJumpInsn(GOTO, stateToLabel.get(state));
                }
            }

        }

        rm.visitMaxs(4, 2);
        rm.visitEnd();

        @SuppressWarnings("unchecked")
        Class<CompiledRunAutomaton> clazz = classLoader.defineClass(className, classVisitor.toByteArray());
//        printClass(classVisitor.toByteArray());
//        System.out.flush();
        return clazz.getConstructor(new Class[0]).newInstance();

    }
}
