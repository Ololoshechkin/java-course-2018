package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class for generation implementations of given abstract classes or interfaces
 * public Methods
 * <ul>
 * <li>{@link Implementor#implement(Class, Path)} generates implementation source code and outputs it to .java file</li>
 * <li>{@link Implementor#implement(Class, Path)} generates implementation source code and packs it to .jar archive </li>
 * </ul>
 * <p>
 * implements {@link JarImpler}
 *
 * @author Vadim Brilyantov
 * @version 1.0
 * @see JarImpler
 * @since 1.0
 */
public class Implementor implements JarImpler {

    /**
     * Helper class to output every given string in Unicode encoding
     * <p>
     * Wraps {@link BufferedWriter}
     * <p>
     * implements {@link Closeable}
     */
    static class UnicodePrinter implements Closeable {
        /**
         * Wrapped <tt>BufferedWriter</tt>
         */
        private BufferedWriter output;

        /**
         * Creates new instance of {@link UnicodePrinter} wrapping given {@link BufferedWriter}
         *
         * @param output BufferedWriter to be wrapped
         */
        UnicodePrinter(BufferedWriter output) {
            this.output = output;
        }

        /**
         * Appends given <tt>text</tt> to <tt>output</tt>
         *
         * @param text text to be appended to wrapped <tt>output</tt>
         * @return reference to <tt>this</tt> object
         * @throws IOException if <tt>output</tt> throws it
         */
        UnicodePrinter append(String text) throws IOException {
            output.append(unicodify(text));
            return this;
        }

        /**
         * Converts given <tt>text</tt> to Unicode
         *
         * @param text String to be converted
         * @return Unicode-escaped representation of given <tt>text</tt>
         */
        private String unicodify(String text) {
            StringBuilder builder = new StringBuilder();
            for (char c : text.toCharArray()) {
                builder.append(c >= 127
                        ? String.format("\\u%04X", (int) c)
                        : c
                );
            }
            return builder.toString();
        }

        /**
         * Override of {@link Closeable#close()} method
         * <p>
         * This method delegates to <tt>output</tt>'s <tt>close</tt> method
         *
         * @throws IOException if wrapped <tt>output</tt> throws it
         */
        @Override
        public void close() throws IOException {
            output.close();
        }
    }

    /**
     * Instantiates a new {@link Implementor} object
     */
    public Implementor() {
    }

    /**
     * Creates a {@link String} consisting of given <tt>list</tt>'s elements with <tt>transform</tt> function applied
     * joined by ", "
     *
     * @param list      list to be joined
     * @param <T>       type of elements in <tt>list</tt>
     * @param transform function to be applied to all elements of the given <tt>list</tt>
     * @return joined sequence of transformed elements separated by ", "
     */
    private static <T> String joinToString(List<T> list, Function<T, String> transform) {
        return list
                .stream()
                .map(transform)
                .collect(Collectors.joining(", "));
    }

    /**
     * Generates a throw-statement for given method or constructor
     *
     * @param method executable that can be either {@link Method} or {@link Constructor}
     * @return String containing text of throw-statement for the given <tt>method</tt> that consists of
     * <ul>
     * <li>throws keyword</li>
     * <li> list of fully-qualified names of all the Exceptions that can be thrown by given <tt>method</tt> separated by ", " </li>
     * </ul>
     */
    private static String getThrows(Executable method) {
        List<Class<?>> exceptions = Arrays.asList(method.getExceptionTypes());
        return exceptions.isEmpty() ? "" : "throws " + joinToString(exceptions, Class::getCanonicalName) + " ";
    }

    /**
     * Returns the argument list of the given method. Method supports 2 modes
     * <ul>
     * <li> typed mode : arguments are following their types</li>
     * <li> untyped mode : arguments are printed without any types </li>
     * </ul>
     *
     * @param method Executable that can be either {@link Method} or {@link Constructor} - method to generate arguments list of
     * @param typed  indicates whether a mode should be typed or not
     * @return String containing text of arguments list of the given <tt>method</tt> that consists of
     * <ul>
     * <li> list of <tt>method</tt>'s argument names separated by ", " for untyped mode </li>
     * <li> list of <tt>method</tt>'s argument types fully-qualified names followed by <tt>method</tt>'s argument names separated by ", " for untyped mode  </li>
     * </ul>
     */
    private static String getArguments(Executable method, boolean typed) {
        return joinToString(
                Arrays.stream(method.getParameters())
                        .map(arg -> (typed ? arg.getType().getCanonicalName() + " " : "") + arg.getName())
                        .collect(Collectors.toList()),
                Function.identity()
        );
    }

    /**
     * Generates default implementation source code of the given <tt>method</tt> assuming that it is a method or a constructor
     * of a class with name = <tt>newClassName</tt>.
     * Implementation is generated to be correct implementation of the given method and formated using Oracle's java code style
     *
     * @param method       method or constructor to generate implementation of
     * @param newClassName name of class containing the given <tt>method</tt>
     * @return <tt>method</tt>'s default implementation.
     * If the given <tt>method</tt> is a method then the body of it consists of a single return-statement.
     * Return value is the default value of <tt>method</tt>'s return type. If <tt>method</tt>'s return type is void
     * then return statement has no return value
     * If the given <tt>method</tt> is a constructor then the body of it consists of a single super constructor invocation statement.
     * Current <tt>method</tt>'s arguments are delegated to <tt>super</tt>
     */
    private static String getMethodImpl(Executable method, String newClassName) {
        String returnTypeAndName;
        Class<?> returnType;
        if (method instanceof Method) {
            Method mtd = ((Method) method);
            returnType = mtd.getReturnType();
            returnTypeAndName = returnType.getCanonicalName() + " " + mtd.getName();
        } else {
            returnType = null;
            returnTypeAndName = newClassName;
        }
        String returnTypeRetValue = "";
        if (returnType != null) {
            returnTypeRetValue = returnType.isPrimitive()
                    ? (returnType.equals(boolean.class) ? " false" : returnType.equals(void.class) ? "" : " 0")
                    : " null";
        }
        int modifiersMask = method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT & ~Modifier.NATIVE;
        String modifiers = Modifier.toString(modifiersMask) + (modifiersMask != 0 ? " " : "");
        String arguments = getArguments(method, true);
        String throwsStatement = getThrows(method);
        String body = method instanceof Method
                ? "return" + returnTypeRetValue + ";"
                : "super(" + getArguments(method, false) + ");";

        return modifiers + "   " + returnTypeAndName + "(" + arguments + ") " + throwsStatement +
                "{\n" + "        " + body + "\n   }\n";
    }

    /**
     * Wrapper class for standard {@link Method} with overridden {@link MethodSignature#hashCode()} and {@link MethodSignature#equals(Object)}
     */
    class MethodSignature {

        /**
         * method to be wrapped
         */
        Method method;

        /**
         * instantiates new {@link MethodSignature} wrapping the given <tt>method</tt>
         *
         * @param method method to weap
         */
        MethodSignature(Method method) {
            this.method = method;
        }

        /**
         * get's name of wrapped <tt>method</tt>
         *
         * @return delegated to {@link Method#getName()} of <tt>method</tt>
         */
        private String getName() {
            return method.getName();
        }

        /**
         * get's arguments of wrapped <tt>method</tt>
         *
         * @return delegated to {@link Method#getParameterTypes()} of <tt>method</tt>
         */
        private Class<?>[] getArgs() {
            return method.getParameterTypes();
        }

        /**
         * Generates the default implementation source code of wrapped <tt>method</tt>
         *
         * @return delegated to static {@link Implementor#getMethodImpl(Executable, String)}
         */
        @Override
        public String toString() {
            return Implementor.getMethodImpl(method, null);
        }

        /**
         * Overrides {@link Object#hashCode()}
         *
         * @return hash code value for current {@link MethodSignature}
         */
        @Override
        public int hashCode() {
            return getName().hashCode() * 31
                    + Arrays.hashCode(getArgs());
        }

        /**
         * Overrides {@link Object#equals(Object)}
         *
         * @param obj object to be compared with
         * @return <ul>
         * <li> <tt>true</tt> if <tt>obj</tt> is an instance of {@link MethodSignature} and values returned by
         * {@link MethodSignature#getName()} and {@link MethodSignature#getArgs()}
         * of both <tt>this</tt> and <tt>obj</tt> equals. </li>
         * <li> <tt>false</tt>, otherwise </li>
         * </ul>
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodSignature)) {
                return false;
            }
            MethodSignature other = (MethodSignature) obj;
            return Objects.equals(getName(), other.getName())
                    && Arrays.equals(getArgs(), other.getArgs());
        }

    }

    /**
     * Adds signatures of given <tt>methods</tt> to specified <tt>distination</tt>
     *
     * @param methods     array of {@link Method} to be added to distination
     * @param distination {@link HashSet} of {@link MethodSignature} to add signatures of <tt>methods</tt> to
     */
    private void addMethodsToSet(Method[] methods, HashSet<MethodSignature> distination) {
        Arrays
                .stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodSignature::new)
                .collect(Collectors.toCollection(() -> distination));
    }

    /**
     * Finds methods of given <tt>currentClass</tt> that are needed to be implemented in it's clildren.
     *
     * @param currentClass {@link Class} to retreive unimplemented methods
     * @return signatures of all unimplemented methods of <tt>currentClass</tt>
     */
    private HashSet<MethodSignature> getMethodSignatures(Class<?> currentClass) {
        HashSet<MethodSignature> methods = new HashSet<>();
        addMethodsToSet(currentClass.getMethods(), methods);
        while (currentClass != null) {
            addMethodsToSet(currentClass.getDeclaredMethods(), methods);
            currentClass = currentClass.getSuperclass();
        }
        return methods;
    }

    /**
     * Produces the implementation source code of each given {@link Executable} to <tt>output</tt> assuming them
     * as implementations inside <tt>newClassName</tt> class
     *
     * @param executables  array of {@link Executable} to be print
     * @param newClassName name of the current class
     * @param output       the {@link UnicodePrinter} to print all output data
     * @throws ImplerException if <tt>output</tt> fails to append any method implementation
     * @see Implementor#getMethodImpl(Executable, String)
     */
    private void printMissingExecutables(Executable[] executables, UnicodePrinter output, String newClassName) throws ImplerException {
        for (Executable exec : executables) {
            try {
                output.append(getMethodImpl(exec, newClassName));
            } catch (IOException e) {
                throw new ImplerException("failed to print method/constructor to output file");
            }
        }
    }

    /**
     * Produces the implementation source code of each given {@link Constructor} to <tt>output</tt> assuming them
     * as implementations of constructors of class with name <tt>newClassName</tt>
     *
     * @param construcors  array of {@link Executable} to be print
     * @param newClassName name of the current class
     * @param output       the {@link UnicodePrinter} to print all output data
     * @throws ImplerException either if <tt>output</tt> fails to append any constructor implementation
     *                         or if the given <tt>construcors</tt> list contains no non-private constructors
     * @see Implementor#getMethodImpl(Executable, String)
     * @see Implementor#printMissingExecutables(Executable[], UnicodePrinter, String)
     */
    private void addMissingConstructors(
            Constructor[] construcors,
            String newClassName,
            UnicodePrinter output
    ) throws ImplerException {
        Constructor[] publicConstructors = Arrays
                .stream(construcors)
                .filter(constr -> !Modifier.isPrivate(constr.getModifiers()))
                .toArray(Constructor[]::new);
        if (publicConstructors.length == 0) {
            throw new ImplerException("superclass has no accessible constructors");
        }
        printMissingExecutables(publicConstructors, output, newClassName);
    }

    /**
     * Returns package name of given class
     *
     * @param token Class to get package
     * @return {@code token.getPackage().getName()} if {@code token.getPackage() != null} or empty string otherwise
     */
    private static String packageNameFor(Class<?> token) {
        return token.getPackage() != null ? token.getPackage().getName() : "";
    }

    /**
     * Returns name of default implementer name of given class or interface <tt>token</tt>
     *
     * @param token Class to get impl-name
     * @return simple name of given <tt>tt</tt> token followed by "Impl" suffix
     */
    private static String implNameFor(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Generates correct implementation source code of given <tt>token</tt> and produces coresponding .java file to given <tt>root</tt>
     * Produced implementation consists of single class with name <tt>token</tt>'s name + "Impl" suffix.
     * Impl-class has all default single-statement implementations of all required methods and constructors to be implemented.
     *
     * @param token {@link Class} to be implemented
     * @param root  path to directory for output
     * @throws ImplerException in the following situations:
     *                         <ul>
     *                         <li> <tt>token</tt> is <tt>null</tt> or <tt>root</tt> is <tt>null</tt> </li>
     *                         <li> <tt>token</tt> represents either a primitive type, final class, enum or array
     *                         (i.e. <tt>token</tt>) cannot be implemented </li>
     *                         <li> <tt>root</tt> is incorrect path </li>
     *                         <li> Error occurs while either creating of output file (with corresponding directories)
     *                         or writing anything to output file </li>
     *                         </ul>
     * @see Implementor#getMethodImpl(Executable, String)
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null) {
            throw new ImplerException("null value is invalid argument");
        }
        if (token.isPrimitive()
                || token.isArray()
                || Modifier.isFinal(token.getModifiers())
                || token == Enum.class) {
            throw new ImplerException("Expected abstract class or an interface as a token");
        }
        String packageName = packageNameFor(token);
        String newClassName = implNameFor(token);
        Path resultPath = getOutputClassPath(packageName, newClassName, root);
        try (UnicodePrinter output = new UnicodePrinter(Files.newBufferedWriter(resultPath))) {
            if (!packageName.isEmpty()) {
                output
                        .append("package ")
                        .append(packageName)
                        .append(";\n");
            }
            output
                    .append("class ")
                    .append(newClassName)
                    .append(token.isInterface() ? " implements " : " extends ")
                    .append(token.getCanonicalName())
                    .append(" {\n");
            printMissingExecutables(
                    getMethodSignatures(token)
                            .stream()
                            .map(it -> it.method)
                            .toArray(Executable[]::new),
                    output,
                    newClassName
            );
            if (!token.isInterface()) {
                addMissingConstructors(token.getDeclaredConstructors(), newClassName, output);
            }
            output.append("}");
        } catch (IOException e) {
            System.out.println("failed to output result to expected file");
        }
    }

    /**
     * Compiles given <tt>file</tt> and produces .class file to given <tt>root</tt> path
     *
     * @param root path to output
     * @param file file name to be compiled
     * @throws ImplerException if either failed to find system {@link JavaCompiler} or compiler returned non-null exit code
     */
    private void compileFiles(Path root, String file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler not found");
        }
        final List<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(root + File.pathSeparator + System.getProperty("java.class.path"));
        args.add("-encoding");
        args.add("UTF-8");
        args.add(file);
        int exitCode = compiler.run(null, null, null, args.toArray(new String[args.size()]));
        if (exitCode != 0) {
            throw new ImplerException("Compilation ended with non-zero code " + exitCode);
        }
    }

    /**
     * Produces jar file from given .class file
     *
     * @param jarFile output jar file path
     * @param file    input .class file path
     * @param root    root directory
     * @throws IOException if fails to write in jar file
     */
    private void jarWrite(Path jarFile, Path file, String root) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        String fileName = file.toString();
        String rootlessFileName = fileName.substring(root.length() + 1, fileName.length());
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            out.putNextEntry(new ZipEntry(rootlessFileName));
            Files.copy(file, out);
        }
    }

    /**
     * Returns output .java/.class file path for class with name <tt>newClassName</tt>, of package <tt>packageName</tt>
     * starting with <tt>root</tt> Path
     *
     * @param packageName  name of package
     * @param newClassName desired name of class
     * @param root         start path
     * @return path to output .java file (including package name)
     * @throws ImplerException if fails to create containing directory of desired class file
     */
    private Path getOutputClassPath(String packageName, String newClassName, Path root) throws ImplerException {
        Path containingDirectory = root.resolve(packageName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(containingDirectory);
        } catch (IOException e) {
            throw new ImplerException("failed to create parent directory for output java-file", e);
        }
        return containingDirectory.resolve(newClassName + ".java");
    }

    /**
     * Returns output .jar file path for class with name <tt>newClassName</tt>, of package <tt>packageName</tt>
     * starting with <tt>root</tt> Path
     *
     *
     * @param packageName  name of package
     * @param newClassName desired name of class
     * @param root         start path
     * @return path to output .java file (including package name)
     * @throws ImplerException if {@link Implementor#getOutputClassPath(String, String, Path)} fails with arguments
     *                         <tt>packageName</tt>, <tt>newClassName</tt>, <tt>root</tt>
     */
    private Path getOutputJarPath(String packageName, String newClassName, Path root) throws ImplerException {
        String classPath = getOutputClassPath(packageName, newClassName, root).toString();
        Path result;
        try {
            result = Paths.get(classPath.substring(0, classPath.length() - ".java".length()) + ".class");
        } catch (InvalidPathException e) {
            throw new ImplerException("Output jar path is invalid (failed to create path for .class file)");
        }
        return result;
    }

    /**
     * Clears all subdirectories in ierarchy starting in given <tt>root</tt> folder
     *
     * @param root folder to start cleaning from
     * @throws IOException if {@link Files#walkFileTree(Path, FileVisitor)} fails
     */
    public static void clearDirs(Path root) throws IOException {
        Files.walkFileTree(
                root,
                new SimpleFileVisitor<Path>() {
                    private FileVisitResult erase(Path file) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        return erase(file);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return erase(dir);

                    }
                }
        );
    }

    /**
     * Generates correct implementation source code of given <tt>token</tt> and produces a jar archive to given <tt>jarFile</tt> path
     * Produced implementation consists of single class with name <tt>token</tt>'s name + "Impl" suffix.
     * Impl-class has all default single-statement implementations of all required methods and constructors to be implemented.
     *
     * @param token   {@link Class} to be implemented
     * @param jarFile path to directory for output
     * @throws ImplerException in the following situations:
     *                         <ul>
     *                         <li> <tt>token</tt> is <tt>null</tt> or <tt>root</tt> is <tt>null</tt> </li>
     *                         <li> <tt>token</tt> represents either a primitive type, final class, enum or array
     *                         (i.e. <tt>token</tt>) cannot be implemented </li>
     *                         <li> <tt>root</tt> is incorrect path </li>
     *                         <li> Error occurs while either creating of output file (with corresponding directories)
     *                         or writing anything to output file </li>
     *                         </ul>
     * @see Implementor#getMethodImpl(Executable, String)
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path root = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp_production");
            implement(token, root);
            Path javaFilePath = getOutputClassPath(packageNameFor(token), implNameFor(token), root);
            Path classFilePath = getOutputJarPath(packageNameFor(token), implNameFor(token), root);
            compileFiles(root, javaFilePath.toString());
            jarWrite(jarFile, classFilePath, root.toString());
            clearDirs(root);
        } catch (IOException e) {
            throw new ImplerException("failed to output to jar file");
        }
    }

    /**
     * Provides comand line interface for <tt>Implementor</tt> class.
     * Available methods: {@link Implementor#implement(Class, Path)} and {@link Implementor#implementJar(Class, Path)}
     * <p>
     *
     * @param args :
     *             <ul>
     *             <li> (1) class name, output path </li>
     *             <li> (2) "-jar", class name, output path </li>
     *             </ul>
     *             <p>
     *             Whether if {@link Class} for given class name cannot be loaded or if given an incorrect <tt>args</tt> array
     *             or if invoked method (<tt>implement</tt> or <tt>implementJar</tt>) fails
     *             then prints corresponding output messages
     *             <p>
     *             When given arguments(1) invokes {@link Implementor#implement(Class, Path)}
     *             When given arguments(2) invokes {@link Implementor#implementJar(Class, Path)}
     */
    public static void main(String[] args) {
        if (args == null ||
                args.length != 2 && args.length != 3 ||
                args.length == 3 && (!args[0].equals("-jar") || !args[2].endsWith(".jar"))) {
            System.out.println(
                    "Expected single argument(class name to implement) " +
                            "or 3 arguments (\"-jar\", class to implement and output file name .jar)"
            );
            return;
        }
        boolean isJar = args.length == 3;
        Class<?> token;
        try {
            String className = isJar ? args[1] : args[0];
            token = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.out.println("Specified class cannot be found or loaded");
            return;
        }
        Path root;
        try {
            root = Paths.get(isJar ? args[2] : args[1]);
        } catch (InvalidPathException e) {
            System.out.println("Specified path is not a system-correct path");
            return;
        }
        Implementor impler = new Implementor();
        try {
            if (isJar) {
                impler.implementJar(token, root);
            } else {
                impler.implement(token, root);
            }
        } catch (ImplerException e) {
            System.out.println("Failed to generate implementation of the given class: " + e.getMessage());
        }
    }

}

