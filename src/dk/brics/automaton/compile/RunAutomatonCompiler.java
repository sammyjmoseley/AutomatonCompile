package dk.brics.automaton.compile;

import dk.brics.automaton.CompiledRunAutomaton;
import dk.brics.automaton.RunAutomaton;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.*;

import static dk.brics.automaton.compile.ASMUtils.getFullyQualifiedName;
import static org.objectweb.asm.Opcodes.*;

public class RunAutomatonCompiler {
    private static int classNum = 0;

    public static CompiledRunAutomaton compile(RunAutomaton runAutomaton) {

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

        Integer[] states = new Integer[][numStates];

        for (int i = 0; i < numStates; i++) {
            states[i] = i;
        }

        String charAtDesc = new ASMMethodSignatureBuilder().addParam(int.class).addRet(char.class).toString();

        for (int i = 0; i < numStates; i++) {
            rm.visitLabel(stateToLabel.get(i));

            @SuppressWarnings("unchecked") final List<Character>[] inverseTransitions = (List<Character>[]) new List[numStates];

            for (int j = 0; j < numStates; j++) {
                inverseTransitions[j] = new ArrayList<>();
            }

            for (int j = Character.MIN_VALUE; j <= Character.MAX_VALUE; j++) {
                inverseTransitions[runAutomaton.step(i, j)].add(j);
            }

            Comparator<Integer> comp = Comparator.comparingInt(a -> inverseTransitions[a].size());

            Arrays.sort(states, comp);

            rm.visitInsn(IINC);
            // Need to check if over lenght of stirng, if is check if current state final, if is return true, else return false
            rm.visitInsn(DUP);
            rm.visitVarInsn(ALOAD, 1);
            rm.visitInsn(SWAP);
            rm.visitMethodInsn(INVOKESPECIAL, getFullyQualifiedName(String.class), "charAt", charAtDesc, false);

            for (Integer state : states) {
                for (Character c : inverseTransitions[state]) {
                    rm.visitInsn(DUP);
                    rm.visitLdcInsn(c);
                    rm.visitJumpInsn(IF_ICMPEQ, stateToLabel.get(state));
                }
            }

        }

    }
}
