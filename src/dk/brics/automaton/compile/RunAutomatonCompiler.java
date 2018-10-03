package dk.brics.automaton.compile;

import dk.brics.automaton.CompiledRunAutomaton;
import dk.brics.automaton.RunAutomaton;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static dk.brics.automaton.compile.ASMUtils.getFullyQualifiedName;
import static org.objectweb.asm.Opcodes.*;

public class RunAutomatonCompiler {
    private static int classNum = 0;
    private static final DynamicClassLoader classLoader = new DynamicClassLoader();

    private static String charAtDesc = new ASMMethodSignatureBuilder().addParam(int.class).addRet(char.class).toString();
    private static String lengthDesc = new ASMMethodSignatureBuilder().addRet(int.class).toString();

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

        final int numStates = runAutomaton.getSize();

        Map<Integer, Label> stateToLabel = new HashMap<>();
        for (int i = 0; i < numStates; i++) {
            stateToLabel.put(i, new Label());
        }

        String runMethodDesc = new ASMMethodSignatureBuilder().addParam(String.class).addParam(boolean.class).toString();
        MethodVisitor rm = cv.visitMethod(ACC_PUBLIC,
                "run", runMethodDesc, null, null);



        //stack: Current Character -> Current Character| String -> Current Character | Character

        rm.visitInsn(ICONST_M1);
        rm.visitJumpInsn(GOTO, stateToLabel.get(runAutomaton.getInitialState()));

        Integer[] states = new Integer[numStates];

        for (int i = 0; i < numStates; i++) {
            states[i] = i;
        }

        for (int i = 0; i < numStates; i++) {
            rm.visitLabel(stateToLabel.get(i));
            rm.visitFrame(F_FULL, 2, new String[]{className, getFullyQualifiedName(String.class)}, 1, new Object[]{INTEGER});

            @SuppressWarnings("unchecked") final List<Character>[] inverseTransitions = (List<Character>[]) new List[numStates];

            for (int j = 0; j < numStates; j++) {
                inverseTransitions[j] = new ArrayList<>();
            }

            for (int j = Character.MIN_VALUE; j <= Character.MAX_VALUE; j++) {
                inverseTransitions[runAutomaton.step(i, (char)j)].add((char)j);
            }

            Comparator<Integer> comp = Comparator.comparingInt(a -> inverseTransitions[a].size());

            Arrays.sort(states, comp);

            Label passLength = new Label();

            rm.visitInsn(IINC);

            // Need to check if over length of stirng, if is check if current state final, if is return true, else return false
            rm.visitInsn(DUP);
            rm.visitVarInsn(ALOAD, 1);
            rm.visitMethodInsn(INVOKESPECIAL, getFullyQualifiedName(String.class), "length", lengthDesc, false);
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
            rm.visitMethodInsn(INVOKESPECIAL, getFullyQualifiedName(String.class), "charAt", charAtDesc, false);

            for (int j = 0; j < states.length; j++) {
                int state = states[j];
                if (j <= states.length - 1) {
                    for (Character c : inverseTransitions[state]) {
                        rm.visitInsn(DUP);
                        rm.visitLdcInsn(c);
                        rm.visitJumpInsn(IF_ICMPEQ, stateToLabel.get(state));
                    }
                } else {
                    rm.visitJumpInsn(GOTO, stateToLabel.get(state));
                }
            }

        }

        rm.visitMaxs(3, 2);
        rm.visitEnd();

        @SuppressWarnings("unchecked")
        Class<CompiledRunAutomaton> clazz = classLoader.defineClass(className, classVisitor.toByteArray());
        return clazz.getConstructor(new Class[0]).newInstance();

    }
}
