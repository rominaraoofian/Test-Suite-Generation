package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyAssignmentStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyMethodStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyChromosomeGenerator implements ChromosomeGenerator<MyChromosome> {
    private final Class<?> cut;
    private final Random random = Randomness.random();
    private final Mutation<MyChromosome> mutation;
    private final Crossover<MyChromosome> crossover;

    public MyChromosomeGenerator(Class<?> cut, Mutation<MyChromosome> mutation, Crossover<MyChromosome> crossover) {
        this.cut = cut;
        this.mutation = mutation;
        this.crossover = crossover;
    }

    @Override
    public MyChromosome get() {
        List<Statement> statements = new ArrayList<>();
        Constructor<?>[] constructors = cut.getConstructors();
        Constructor<?> constructor = constructors[random.nextInt(constructors.length)];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            params[i] = generateRandomSingleParameter(parameterTypes[i]);
        }
        MyConstructorStatement myConstructorStatement = new MyConstructorStatement(constructor, params);
        statements.add(myConstructorStatement);
        myConstructorStatement.run();
        Object target = myConstructorStatement.getResult();
        int numberOfStatements = 20+ random.nextInt(30);
        Method[] methods = cut.getDeclaredMethods();
        Field[] fields = cut.getDeclaredFields();
        for (int i = 0; i < numberOfStatements; i++) {
            if (random.nextBoolean() && methods.length > 0) {
                Statement statementTemp = generateRandomMethodCall(target);
                if (statementTemp != null) {
                    statements.add(statementTemp);
                }
            } else if (fields.length > 0) {
                Statement statementTemp = generateRandomFieldAssignment(target);
                if (statementTemp != null) {
                    statements.add(statementTemp);
                }
            }
        }

        return new MyChromosome(statements, mutation, crossover);

    }

    public Object generateRandomSingleParameter(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            if (type == Integer.class && random.nextDouble() < 0.05) {
                return null;
            }
            return random.nextInt(Randomness.MAX_INT - Randomness.MIN_INT + 1) + Randomness.MIN_INT;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (type == Boolean.class  && random.nextDouble() < 0.05) {
                return null;
            }
            return random.nextBoolean();
        }
        if (type == String.class) {
            if (random.nextDouble() < 0.05) {
                return null;
            }
            return generateRandomString();
        }
        if (type == double.class || type == Double.class) {
            if (type == Double.class && random.nextDouble() < 0.05) {
                return null;
            }

            return random.nextDouble() * (Randomness.MAX_INT - Randomness.MIN_INT) + Randomness.MIN_INT;

        }

        if (type == float.class || type == Float.class) {
            if (type == Float.class && random.nextDouble() < 0.05) return null;
            return (float) (random.nextDouble() * (Randomness.MAX_INT - Randomness.MIN_INT) + Randomness.MIN_INT);
        }

        if (type == long.class || type == Long.class) {
            if (type == Long.class && random.nextDouble() < 0.05) return null;
            return (long) (random.nextInt(Randomness.MAX_INT - Randomness.MIN_INT + 1) + Randomness.MIN_INT);
        }
        if (!(type.isPrimitive())) {
            return null;
        }

        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return ' ';
        return 0;
    }

    public String generateRandomString() {
        int length = random.nextInt(20) + 1;
        StringBuilder createdString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int asciiCode;
            do {
                asciiCode = random.nextInt(126 - 32 + 1) + 32;
            } while (asciiCode == 34 || asciiCode == 92);
            createdString.append((char) asciiCode);
        }
        return createdString.toString();
    }

    public Statement generateRandomFieldAssignment(Object targetInstance) {
        Field[] fields = cut.getDeclaredFields();
        if (fields.length == 0) {
            return null;
        }
        for (int i = 0; i < 10; i++) {
            Field field = fields[random.nextInt(fields.length)];
            int modifiers = field.getModifiers();
            if (!Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)) {
                Object value = generateRandomSingleParameter(field.getType());
                return new MyAssignmentStatement(field, targetInstance, value);
            }
        }
        return null;
    }

    public Statement generateRandomMethodCall(Object targetInstance) {
        Method[] methods = cut.getDeclaredMethods();
        if (methods.length == 0) {
            return null;
        }
        for (int i = 0; i < 10; i++) {
            Method method = methods[random.nextInt(methods.length)];
            if (!Modifier.isPrivate(method.getModifiers())) {
                Class<?>[] parameters = method.getParameterTypes();
                Object[] params = new Object[parameters.length];

                for (int j = 0; j < parameters.length; j++) {
                    if (parameters[j].equals(cut)) {
                        params[j] = targetInstance;
                    } else {
                        params[j] = generateRandomSingleParameter(parameters[j]);
                    }
                }
                return new MyMethodStatement(method, targetInstance, params);
            }
        }
        return null;
    }
}