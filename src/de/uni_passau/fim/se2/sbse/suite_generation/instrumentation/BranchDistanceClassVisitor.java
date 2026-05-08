package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ASM7;

/**
 * A visitor for Java classes that invokes a {@link BranchDistanceMethodVisitor} for every method it
 * encounters.
 *
 * @author Sebastian Schweikl
 */
final class BranchDistanceClassVisitor extends ClassVisitor {

    /**
     * The fully qualified name of the currently visited class in internal form.
     */
    private final String className;

    /**
     * Creates a new {@code BranchDistanceClassVisitor} for the class specified by the given
     * non-{@code null} fully qualified class name. The visitor delegates method calls to the given
     * class visitor.
     *
     * @param className the name of the visited class
     * @param cv        the class visitor to which method calls are delegated
     */
    public BranchDistanceClassVisitor(final String className, final ClassVisitor cv) {
        super(ASM7, cv);
        this.className = requireNonNull(className);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String methodName,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        final var mv = super.visitMethod(access, methodName, descriptor, signature, exceptions);
        return new BranchDistanceMethodVisitor(mv, className, methodName, descriptor);
    }
}
