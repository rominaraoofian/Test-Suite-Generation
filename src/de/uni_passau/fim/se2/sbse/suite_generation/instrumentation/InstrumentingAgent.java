package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.StringJoiner;

/**
 * A so called <em>agent</em> class for the JVM which can be used to instrument Java programs by
 * manipulating the byte-codes of methods. This particular agent has to be started explicitly by
 * specifying a JVM option on the command-line interface:
 * <pre>{@code
 * -javaagent:<jarpath>[=<options>]
 * }</pre>
 * where {@code <jarpath>} is the path to the agent JAR file and {@code <options>} is the optional
 * agent options (similar to the {@code args} options of a Java application's {@code main} method).
 * {@code options} are passed as a single string (unlike the {@code args} options of a {@code main}
 * method), and any additional parsing must be performed by the agent itself. Agents can be deployed
 * as separate JAR files. However, this agent will be packaged as part of the application which we
 * want to instrument. The manifest file of the agent JAR must contain the attribute {@code
 * Premain-Class}, and its value must be set to the name of this agent class.
 *
 * @author Sebastian Schweikl
 * @see java.lang.instrument
 */
public final class InstrumentingAgent {

    /**
     * Name of the agent JAR.
     */
    private static final String jarName = "testsuite-generation-1.0-SNAPSHOT.jar";

    /**
     * How to invoke the {@code InstrumentingAgent} on the command line.
     */
    private static final String usage = new StringJoiner("\n")
            .add(f("agent usage: java -javaagent %s=<cut>:[debug]", jarName))
            .add(f("                  -jar %s <jar_args>", jarName))
            .add(f(" <cut>       fully-qualified name of the class under test"))
            .add(f(" [debug]     whether to write instrumented class files to disk (optional)"))
            .add(f(" <jar_args>  the arguments of the application JAR"))
            .toString();

    /**
     * Whether the agent has already been started. There seems to be a strange bug when executing
     * the program on Linux using OpenJDK 11: for some reason, the agent happens to be started
     * twice, and thus the Java byte-code is also instrumented twice. At this point, we cannot
     * prevent the agent from being started multiple times. However, we can make sure that
     * instrumentation is performed only once by checking whether this attribute is still {@code
     * false} when the {@code premain} method is executed. After we instrumented the byte-code, we
     * set this attribute to {@code true}.
     */
    private static boolean instrumented = false;

    /**
     * The name of the instrumentation target.
     */
    private static String targetClass;

    private InstrumentingAgent() {
        // Private constructor to prevent instantiation of this class.
    }

    /**
     * Tells whether the agent has been invoked and instrumentation has been performed.
     *
     * @return {@code true} if instrumentation was performed, {@code false} otherwise
     */
    public static boolean isInstrumented() {
        return instrumented;
    }

    /**
     * The {@code premain} method of this agent, which is responsible for toggling the
     * instrumentation process. {@code premain} represents the main entry point of this agent,
     * similar in principle to the main entry point {@code main} of the "real" Java application.
     * With an agent and a {@code premain} method in place, the JVM starts up as follows:
     * <ol>
     *     <li>The JVM is initialized,</li>
     *     <li>the agent class is loaded and its {@code premain} method is called, and</li>
     *     <li>the "real" application {@code main} method is called.</li>
     * </ol>
     * Note that {@code premain} must return in order for JVM startup to proceed. If {@code premain}
     * terminates abnormally (e.g., by throwing an uncaught exception), the JVM will abort without
     * running the actual {@code main} method of the Java application.
     * <p>
     * The parameter {@code agentArgs} represents the command line options passed to this agent as a
     * string (see {@link InstrumentingAgent}). It must be of the following structure:
     * <pre>{@code
     *     <target class>[:debug]
     * }</pre>
     * where the substring {@code <target class>} represents the fully qualified name of the
     * target class that should be instrumented. For example, {@code com.example.foo.Bar}. When the
     * optional substring {@code :debug} is appended to {@code agentArgs} the agent will write the
     * instrumented class file to the hard disk (by default, in the folder {@code class-files}).
     * These class files can be disassembled and inspected using the tool {@code javap}, e.g.,
     * <pre>{@code
     *     javap -c -p -l -s Foo.class
     * }</pre>
     * <p>
     * An instance {@code inst} of the {@code Instrumentation} interface is passed automatically by
     * the JVM. It provides services needed to instrument Java programming language code.
     *
     * @param agentArgs the target package for instrumentation (user-specified)
     * @param inst      an instance of the {@code Instrumentation} interface (given by the JVM via
     *                  dependency injection)
     */
    public static void premain(final String agentArgs, final Instrumentation inst) {
        if (agentArgs == null || agentArgs.isBlank()) {
            System.err.println("Agent: missing instrumentation target");
            printHelp();
            System.exit(0);
        }

        if (!instrumented) {
            // Options parsing.
            final String[] args = agentArgs.split(":");
            final String targetClass = args[0];
            InstrumentingAgent.targetClass = targetClass;
            final boolean debug = args.length == 2 && args[1].equals("debug");

            // Create a new transformer and register it. Subsequently, the transformer's transform()
            // method will be called whenever a class is loaded, and before it is defined by the
            // JVM.
            System.out.println("Instrumenting " + targetClass);
            inst.addTransformer(new BranchDistanceTransformer(targetClass, debug));
            System.out.println("Done.");

            // To prevent the agent from instrumenting the byte codes a second time.
            instrumented = true;
        }
    }

    /**
     * Returns the name of the instrumented class.
     *
     * @return name of the instrumented class
     */
    public static String getTargetClass() {
        return targetClass;
    }

    /**
     * Alias for {@link String#format(String, Object...)}.
     */
    private static String f(final String format, final Object... args) {
        return String.format(format, args);
    }

    /**
     * Prints a help message detailing the command line syntax and available command line options.
     */
    private static void printHelp() {
        System.out.println(usage);
    }
}
