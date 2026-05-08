package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import java.lang.reflect.Field;

public class MyAssignmentStatement implements Statement {
    private final Field field;
    private Object target;
    private final Object value;

    public MyAssignmentStatement(Field field, Object target, Object value) {
        this.field = field;
        this.target = target;
        this.value = value;
    }

    @Override
    public void run() {
        try {

            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("assignment statement error: ", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("obj.").append(field.getName()).append(" = ");
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            String escaped = value.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            sb.append("\"").append(escaped).append("\"");
        } else if (value instanceof Double) {
            sb.append(value).append("d");
        } else if (value instanceof Long) {
            sb.append(value).append("L");
        } else {
            sb.append(value);
        }
        sb.append(";");
        return sb.toString();
    }
    public void setTarget(Object target) {
        this.target = target;
    }
}