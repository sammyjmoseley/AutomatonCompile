package dk.brics.automaton.compile;


class ASMUtils {

    /**
     * The easiest way to remember whether to use {@code getFullyQualifiedName} or {@code getTypeName}, is whether the {@code clazz} could be primitive.
     * If it could be a primitive then you have to use {@code getTypeName}, otherwise {@code getFullyQualifiedName} works fine.
     * @param clazz
     * @return
     */
    public static String getFullyQualifiedName(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "double":
                    return "D";
                case "float":
                    return "F";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "short":
                    return "S";
                default:
                    throw new IllegalArgumentException("Got an unknown primitive type " + clazz.getName());

            }
        }
        return clazz.getName().replace('.','/');
    }

    public static String getTypeName(Class<?> clazz) {
        String name = getFullyQualifiedName(clazz);
        if (!clazz.isPrimitive() && !clazz.isArray()) {
            return "L" + name + ";";
        }

        return name;
    }
}
