package dk.brics.automaton.compile;

import java.util.ArrayList;
import java.util.List;

class ASMMethodSignatureBuilder {
    List<Class<?>> params;
    List<Class<?>> rets;

    public ASMMethodSignatureBuilder() {
        this.params = new ArrayList<>();
        this.rets = new ArrayList<>();
    }

    public static final String EQUALS_METHOD_SIG =
            new ASMMethodSignatureBuilder()
                .addParam(Object.class)
                .addRet(boolean.class)
                .toString();

    public static String compareToSig(Class<?> clazz) {
        return new ASMMethodSignatureBuilder()
                .addParam(clazz)
                .addRet(int.class)
                .toString();
    }

    public ASMMethodSignatureBuilder addParam(Class<?> clazz) {
        this.params.add(clazz);
        return this;
    }

    public int addParamGetIdx(Class<?> clazz) {
        this.params.add(clazz);
        return this.params.size();//return size now because 0th param is this
    }

    public ASMMethodSignatureBuilder addRet(Class<?> clazz) {
        this.rets.add(clazz);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        for (Class clazz : params) {
            sb.append(ASMUtils.getTypeName(clazz));
        }

        sb.append(")");

        for (Class clazz : rets) {
            sb.append(ASMUtils.getTypeName(clazz));
        }

        if (rets.size() == 0) {
            sb.append("V");
        }

        return sb.toString();
    }

    public int getNumLocals() {
        return this.params.size() + 1;
    }
}
