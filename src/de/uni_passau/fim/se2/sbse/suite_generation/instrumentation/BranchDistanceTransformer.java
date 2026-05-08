package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;

/**
 * Modifies the byte-codes of the methods in an instrumented class with the aim of facilitating the
 * measurement of branching distances.
 *
 * @author Sebastian Schweikl
 */
final class BranchDistanceTransformer implements ClassFileTransformer {

    /**
     * Name of the directory where instrumented class files should be put.
     */
    private static final String outDirName = "./class-files";

    /**
     * The fully qualified name of the class that should be instrumented. Class names are encoded in
     * internal form, i.e., using slashes "{@code /}" instead of dots "{@code .}". For example,
     * "{@code de/uni_passau/fim/se2/examples/Feature}".
     */
    private final String targetClass;

    /**
     * Whether to run this agent in debug mode.
     */
    private final boolean debug;

    /**
     * Creates a new transformer that instruments the byte codes of the specified class to measure
     * branching distance. When {@code debug} is set to {@code true} the transformer is run in debug
     * mode, which means that it also writes the instrumented byte codes to the system's default
     * temporary-file directory as {@code .class} files.
     *
     * @param targetClass the fully qualified name of the class to instrument
     * @param debug       {@code true} if the instrumented class files should be written to disk,
     *                    {@code false} otherwise
     */
    BranchDistanceTransformer(final String targetClass, final boolean debug) {
        this.targetClass = targetClass.replace('.', '/');
        this.debug = debug;
    }

    /**
     * Writes the given byte code to a temporary file whose name is derived from the specified
     * string.
     *
     * @param byteCode the contents of the {@code .class} file
     * @param fileName the name of the {@code .class} file
     */
    private static void writeClassFile(final byte[] byteCode, final String fileName) {
        final Path outDir = Paths.get(outDirName);

        if (!Files.exists(outDir)) {
            try {
                Files.createDirectory(outDir);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else if (!Files.isDirectory(outDir)) {
            System.err.printf("Cannot create directory %s because file with same name already "
                    + "exists", outDir);
            return;
        }

        final Path classFile;
        try {
            classFile = Files.createTempFile(outDir, fileName, ".class");
            Files.write(classFile, byteCode);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.printf("%s written to %s", classFile.getFileName(), outDir.getFileName());
    }

    /**
     * Transforms the given class file by inserting instructions into the byte codes that measure
     * the branching distance of all the branches in the class file. The result of the
     * transformation is an instrumented class file, which is to be used as a replacement for the
     * original one, or {@code null} if no transformation was performed. The returned class file
     * must be a valid, well-formed class file buffer.
     *
     * @param loader              the defining loader of the class to be transformed, may be null if
     *                            the bootstrap loader
     * @param className           the name of the class in the internal form of fully qualified
     *                            class and interface names as defined in The Java Virtual Machine
     *                            Specification (for example, "{@code java/util/List}")
     * @param classBeingRedefined if the invocation of this {@code transform()} method is triggered
     *                            by a redefinition or retransformation, the class being
     *                            redefined or retransformed; if this is a class load, {@code null}
     * @param protectionDomain    the protection domain of the class being defined or redefined
     * @param classFileBuffer     the input byte buffer in class file format - must not be modified
     * @return a well-formed class file buffer (the result of the transform) containing additional
     * instructions to measure branching distance, or null if no transform is performed
     */
    @Override
    public byte[] transform(
            final ClassLoader loader,
            final String className,
            final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain,
            final byte[] classFileBuffer) {
        // Performs the transformation if the given class is the target of the instrumentation.
        if (isTargetClass(className)) {
            final var reader = new ClassReader(classFileBuffer);
            final var writer = new ClassWriter(reader, COMPUTE_FRAMES);
            final var visitor = new BranchDistanceClassVisitor(className, writer);
            reader.accept(visitor, SKIP_FRAMES);
            final byte[] instrumented = writer.toByteArray();

            if (debug) {
                writeClassFile(instrumented, className.replace('/', '.'));
            }

            return instrumented;
        } else {
            return null;
        }
    }

    /**
     * Determines if the currently loaded class (identified by the given fully qualified class name)
     * is the target class, or in other words, whether the class should be instrumented.
     *
     * @param className the name of the class to check
     * @return {@code true} if the class should be instrumented, {@code false} otherwise
     */
    private boolean isTargetClass(final String className) {
        return className.equals(targetClass);
    }
}
