package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Decision;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Entry;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Node;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ASM7;

/**
 * A visitor for Java methods that inserts instructions to compute the branching distance when
 * conditional jump instructions (also called "control transfer instructions" in the JVM
 * specification) and root branches of non-abstract methods are encountered.
 *
 * @author Sebastian Schweikl
 */
final class BranchDistanceMethodVisitor extends MethodVisitor {

    /**
     * The name of the static constructor of a class.
     */
    private static final String STATIC_CONSTRUCTOR = "<clinit>";

    /**
     * The number of branches already visited. Can be used to create unique identifiers for the
     * branches this visitor encounters.
     */
    private static int branchCounter = 0;

    /**
     * The fully qualified name of the currently visited class in internal form (i.e., using slashes
     * "{@code /}" instead of dots "{@code .}").
     */
    private final String className;

    /**
     * The name of the visited method.
     */
    private final String methodName;

    /**
     * The descriptor of the visited method.
     */
    private final String descriptor;

    /**
     * The currently visited line number declaration, corresponding to the line number of the
     * original Java source code from which the visited class was compiled. (Note: this requires
     * that line number tables are present in the class file.) Can be used to locate a given branch
     * node in the original Java source file. This works because the visitor invokes {@link
     * #visitLineNumber} before {@link #visitJumpInsn}, as detailed in the documentation for {@link
     * MethodVisitor}.
     */
    private int currentLine;

    /**
     * Constructs a new method visitor for the method specified by the given non-{@code null} fully
     * qualified class name, non-{@code null} method name and non-{@code null} method descriptor.
     * The visitor delegates method calls to the supplied method visitor {@code mv}.
     *
     * @param mv         the method visitor to which method calls are delegated
     * @param className  the name of the visited class
     * @param methodName the name of the visited method
     * @param descriptor the descriptor of the visited method
     * @throws NullPointerException if an argument is {@code null}
     */
    BranchDistanceMethodVisitor(final MethodVisitor mv, final String className,
            final String methodName, final String descriptor) {
        super(ASM7, mv);
        this.className = requireNonNull(className);
        this.methodName = requireNonNull(methodName);
        this.descriptor = requireNonNull(descriptor);
        this.currentLine = 0;
    }

    /**
     * Visits a line number declaration (referring to the Java source file from which the visited
     * class was compiled) and records it in this visitor. By default, (and unless explicitly
     * disabled via {@code -g:none} or similar flags), the Java compiler includes a so-called line
     * number table in the compiled byte code. This allows us to map byte code instructions to the
     * corresponding line numbers in the Java source file the byte code was compiled from.
     *
     * @param line  the line number in the original Java source file
     * @param start the label referring to the first instruction in this line
     */
    @Override
    public void visitLineNumber(final int line, final Label start) {
        currentLine = line;
        mv.visitLineNumber(line, start);
    }

    /**
     * Visits a jump instruction and, given that it is a conditional jump instruction, injects
     * additional instructions into the byte code with the aim of measuring branching distance for
     * the jump instruction at hand. Note that the original jump instruction is still kept in the
     * instrumented code in order not to modify the intended behavior as given by the original
     * program.
     *
     * @param opcode the opcode of the visited jump instruction
     * @param label  the target where the jump instruction may jump to (if the jump condition is
     *               satisfied)
     */
    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        final var node = new Node(currentLine, className);
        final var trueBranch = new Decision(branchCounter++, node, true);
        final var falseBranch = new Decision(branchCounter++, node, false);
        BranchTracer.getInstance().instrumentBranchNode(this, trueBranch, falseBranch, opcode);

        // We have to invoke the super method to re-insert the original jump instruction into
        // the instrumented byte code (otherwise, we would be deleting it from the byte code).
        super.visitJumpInsn(opcode, label);
    }

    /**
     * Visits the root branch of the current method and injects additional instructions into the
     * byte code with the aim of tracking whether the method has been called. The original code of
     * the method is retained in order not to modify the behavior of the original program.
     */
    @Override
    public void visitCode() {
        // Excludes the static constructor as coverage goal.
        if (!methodName.equals(STATIC_CONSTRUCTOR)) {
            final var rootBranch = new Entry(branchCounter++, className, methodName, descriptor);
            BranchTracer.getInstance().instrumentMethodEntry(this, rootBranch);
        }

        super.visitCode();
    }
}
