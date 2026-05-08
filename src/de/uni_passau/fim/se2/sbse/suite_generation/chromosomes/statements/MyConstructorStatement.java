package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import java.lang.reflect.Constructor;

public class MyConstructorStatement implements Statement {
     private final Constructor<?> constructor;
     private final Object[] parameters;
     private Object result;

    public MyConstructorStatement(Constructor<?> constructor, Object[] parameterValues) {
        this.constructor = constructor;
        this.parameters = parameterValues;
    }

    @Override
    public void run() {
        try {
            constructor.setAccessible(true); // Be sure the constructor is accessible
            this.result = constructor.newInstance(parameters);
        } catch (Exception e) {
            throw new RuntimeException("constructor statement error: ", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String className = constructor.getDeclaringClass().getSimpleName();
        sb.append(className).append(" obj = new ").append(className).append("(");

        for (int i = 0; i < parameters.length; i++) {
            Object val = parameters[i];

            // ADD THIS LOGIC:
            if (val == null) {
                sb.append("null");
            } else if (val instanceof String) {
                String escaped = val.toString().replace("\\", "\\\\").replace("\"", "\\\"");
                sb.append("\"").append(escaped).append("\"");
            } else if (val instanceof Double) {
                sb.append(val).append("d");
            } else if (val instanceof Long) {
                sb.append(val).append("L");
            } else {
                sb.append(val);
            }

            if (i < parameters.length - 1) sb.append(", ");
        }



        sb.append(");");
        return sb.toString();
    }


    public Object getResult() {
        return result;
    }
}
