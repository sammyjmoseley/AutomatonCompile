package dk.brics.automaton.compile;

import dk.brics.automaton.CompiledRunAutomaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

import java.lang.reflect.InvocationTargetException;

public class Benchmark {
    private static final int SIZE = 30000000;

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String regex = ".*a..b.*e.*f";
        String[] data = makeStringData(SIZE);

        RunAutomaton runAutomaton = new RunAutomaton(new RegExp(regex).toAutomaton());
        CompiledRunAutomaton compiledRunAutomaton = RunAutomatonCompiler.compile(runAutomaton);

        int c1, c2;
        c1 = run(runAutomaton, data);
        c1 += run(runAutomaton, data);
        c1 += run(runAutomaton, data);
        c2 = run(compiledRunAutomaton, data);
        c2 += run(compiledRunAutomaton, data);
        c2 += run(compiledRunAutomaton, data);
        if (c1 != c2) {
            throw new RuntimeException(c1 + " != " + c2);
        }

        long time;

        time = System.currentTimeMillis();
        c1 = run(runAutomaton, data);
        c1 += run(runAutomaton, data);
        c1 += run(runAutomaton, data);
        time = System.currentTimeMillis() - time;
        System.out.println("un-compiled: " + time);

        time = System.currentTimeMillis();
        c2 = run(compiledRunAutomaton, data);
        c2 += run(compiledRunAutomaton, data);
        c2 += run(compiledRunAutomaton, data);
        time = System.currentTimeMillis() - time;
        System.out.println("compiled: " + time);

        if (c1 != c2) {
            throw new RuntimeException(c1 + " != " + c2);
        }
    }

    private static int run(RunAutomaton runAutomaton, String[] data) {
        int count = 0;

        for (String str : data) {
            if (runAutomaton.run(str)) {
                count++;
            }
        }

        return count;
    }

    private static int run(CompiledRunAutomaton runAutomaton, String[] data) {
        int count = 0;

        for (String str : data) {
            if (runAutomaton.run(str)) {
                count++;
            }
        }

        return count;
    }

    private static String[] makeStringData(int num) {
        String[] strings = new String[num];
        StringBuilder sb = new StringBuilder();
        sb.append("aaaaaaaaaaaaaaaaaaaaaaaaaaa");
        strings[0] = sb.toString();
        for (int i = 1; i < num; i++) {
            boolean carry = true;
            int j = sb.length() - 1;
            while (carry) {
                if (j < 0) {
                    sb.insert(0, 'a');
                    break;
                }
                char lastChar = sb.charAt(j);
                if (lastChar == 'z') {
                    lastChar = 'a';
                } else {
                    lastChar = (char)(lastChar + 1);
                    carry = false;
                }
                sb.setCharAt(j, lastChar);
                j--;
            }
            strings[i] = sb.toString();
        }
        return strings;
    }
}
