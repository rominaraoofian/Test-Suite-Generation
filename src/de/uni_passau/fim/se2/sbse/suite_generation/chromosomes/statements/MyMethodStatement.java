package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import java.lang.reflect.Method;

public class MyMethodStatement implements Statement {
    private final Method method;
    private Object target;
    private final Object[] parameters;

    public MyMethodStatement(Method method, Object target, Object[] parameters) {
        this.method = method;
        this.target = target;
        this.parameters = parameters;
    }

    @Override
    public void run() {
        try {
            method.setAccessible(true);
            method.invoke(target, parameters);
        } catch (Exception e) {
            throw new RuntimeException("method statement error: ", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("obj.").append(method.getName()).append("(");

        for (int i = 0; i < parameters.length; i++) {
            Object val = parameters[i];

            if (val == null) {
                sb.append("null");
            } else if (val instanceof String) {
                // Escape special characters to ensure valid Java code
                String escaped = val.toString().replace("\\", "\\\\").replace("\"", "\\\"");
                sb.append("\"").append(escaped).append("\"");
            } else if (target != null && val.getClass().equals(target.getClass())) {
                sb.append("obj");
            } else if (val instanceof Double) {
                sb.append(val).append("d");
            } else if (val instanceof Long) {
                sb.append(val).append("L");
            } else {
                sb.append(val);
            }

            if (i < parameters.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(");");
        return sb.toString();
    }
    public void setTarget(Object target) {
        this.target = target;
    }
}
