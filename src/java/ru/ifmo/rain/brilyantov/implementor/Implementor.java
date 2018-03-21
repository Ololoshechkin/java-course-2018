package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Creates a {@link String} consisting of given <tt><list/tt>'s elements with <tt>transform</tt> function applied
     * joined by ", "
     *
     * @param list      {@link List} to be joined
     * @param transform {@link Function} to be applied to all elements of the given <tt>list</>
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
     * @param method {@link Executable} that can be either {@link Method} or {@link Constructor}
     * @return {@link String} containing text of throw-statement for the given <tt>method</tt> that consists of
     * <ul>
     * <li>throws keyword</li>
     * <li> list of fully-qualified names of all the Exceptions that can be thrown by given <tt>method</tt> separated by ", " </li>
     * </ul>
     */
    private static String getThrows(Executable method) {
        List<Class<?>> exceptions = Arrays.asList(method.getExceptionTypes());
        return exceptions.isEmpty() ? "" : "throws " + joinToString(exceptions, Class::getCanonicalName);
    }

    /**
     * Returns the argument list of the given method. Method supports 2 modes
     * <ul>
     * <li> typed mode : arguments are following their types</li>
     * <li> untyped mode : arguments are printed without any types </li>
     * </ul>
     *
     * @param method {@link Executable} that can be either {@link Method} or {@link Constructor} - method to generate arguments list of
     * @param typed  indicates whether a mode should be typed or not
     * @return {@link String} containing text of arguments list of the given <tt>method</tt> that consists of
     * <ul>
     * <li> list of <tt>method</tt>'s argument names separated by ", " for untyped mode </li>
     * <li> list of <tt>method</tt>'s argument types fully-qualified names followed by <tt>method</tt>'s argument names separated by ", " for untyped mode  </li>
     * </ul>
     */
    private static String getArguments(Executable method, boolean typed) {
        final int[] varName = {0};
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
        String returnTypeName;
        Class<?> returnType;
        if (method instanceof Method) {
            Method mtd = ((Method) method);
            returnType = mtd.getReturnType();
            returnTypeName = returnType.getCanonicalName() + " " + mtd.getName();
        } else {
            returnType = null;
            returnTypeName = newClassName;
        }
        String returnTypeRetValue = "";
        if (returnType != null) {
            returnTypeRetValue = returnType.isPrimitive()
                    ? (returnType.equals(boolean.class) ? " false" : returnType.equals(void.class) ? "" : " 0")
                    : " null";
        }
        return String.format(
                "%n   %s %s %s(%s) %s {%n        %s;%n   }%n",
                Arrays
                        .stream(method.getAnnotations())
                        .map(Annotation::toString)
                        .collect(Collectors.joining("%n")),
                Modifier.toString(method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT & ~Modifier.NATIVE),
                returnTypeName,
                getArguments(method, true),
                getThrows(method),
                method instanceof Method
                        ? "return" + returnTypeRetValue
                        : "super(" + getArguments(method, false) + ")"
        );
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
         * get's return type of wrapped <tt>method</tt>
         *
         * @return delegated to {@link Method#getReturnType()} of <tt>method</tt>
         */
        private Class<?> getReturnType() {
            return method.getReturnType();
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
            return getName().hashCode() * 31 * 31
                    + Arrays.hashCode(getArgs()) * 31
                    + getReturnType().hashCode();
        }

        /**
         * Overrides {@link Object#equals(Object)}
         *
         * @param obj object to be compared with
         * @return <ul>
         * <li> <tt>true</tt> if <tt>obj</tt> is an instance of {@link MethodSignature} and values returned by
         * {@link MethodSignature#getName()}, {@link MethodSignature#getReturnType()} and {@link MethodSignature#getArgs()}
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
                    && Objects.equals(getReturnType(), other.getReturnType())
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
     * @return signatures ({@link MethodSignature}) of all unimplemented methods of <tt>currentClass</tt>
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
     * Gets all constructors to be implemented for given !interface! <tt>newClassName</tt> and produces the implementation text to <tt>output</tt>
     *
     * @param construcors  list of constructors to be implemented
     * @param newClassName name of the new class
     * @param output       the
     * @throws ImplerException if the given <tt>construcors</tt> list contains no non-private constructors
     */
    private void addMissingConstructors(Constructor[] construcors, String newClassName, StringBuffer output) throws ImplerException {
        List<Constructor> constructors = Arrays
                .stream(construcors)
                .filter(constr -> !Modifier.isPrivate(constr.getModifiers()))
                .collect(Collectors.toList());
        if (constructors.size() == 0) {
            throw new ImplerException("superclass has no accessible constructors");
        }
        constructors.forEach(constr -> output.append(getMethodImpl(constr, newClassName)));
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
        try (BufferedWriter output = Files.newBufferedWriter(resultPath)) {
            StringBuffer resultBuffer = new StringBuffer();
            if (!packageName.isEmpty()) {
                resultBuffer
                        .append("package ")
                        .append(packageName)
                        .append(";\n");
            }
            resultBuffer
                    .append("class ")
                    .append(newClassName)
                    .append(token.isInterface() ? " implements " : " extends ")
                    .append(token.getSimpleName())
                    .append(" {\n");
            getMethodSignatures(token).forEach(method -> resultBuffer.append(method.toString()));
            if (!token.isInterface()) {
                addMissingConstructors(token.getDeclaredConstructors(), newClassName, resultBuffer);
            }
            resultBuffer.append("}");
            String result = resultBuffer.toString();
            output.write(result);
        } catch (IOException e) {
            System.out.println("failed to output result to expected file");
        }
    }

    private void compileFiles(Path root, String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root + File.pathSeparator + System.getProperty("java.class.path"));
        compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    private void jarWrite(Path jarFile, Path file) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            out.putNextEntry(new ZipEntry(file.normalize().toString()));
            Files.copy(file, out);
            out.closeEntry();
        }
    }

    private Path getOutputClassPath(String packageName, String newClassName, Path root) throws ImplerException {
        Path containingDirectory = root.resolve(packageName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(containingDirectory);
        } catch (IOException e) {
            throw new ImplerException("failed to create parent directory for output java-file", e);
        }
        return containingDirectory.resolve(newClassName + ".java");
    }

    private Path getOutputJarPath(String packageName, String newClassName, Path root) throws ImplerException {
        String classPath = getOutputClassPath(packageName, newClassName, root).toString();
        if (classPath.endsWith(".java")) {
            return Paths.get(classPath.substring(0, classPath.length() - ".java".length()) + ".class");
        } else {
            throw new IllegalArgumentException("Token is not a java file");
        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path root = Paths.get(".");
            JarImpler implementor = new Implementor();
            implementor.implement(token, root);
            Path javaFilePath = getOutputClassPath(packageNameFor(token), implNameFor(token), root);
            Path classFilePath = getOutputJarPath(packageNameFor(token), implNameFor(token), root);
            compileFiles(root, javaFilePath.toString());
            jarWrite(jarFile, classFilePath);
            classFilePath.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new ImplerException("failed to output to jar file");
        }
    }

    public static void main(String[] args) {
        if (args == null ||
                args.length != 1 && args.length != 3 ||
                args.length == 3 && (!args[0].equals("-jar") || !args[2].endsWith(".jar"))) {
            System.out.println(
                    "Expected single argument(class name to implement) " +
                            "or 3 arguments (\"-jar\", class to implement and output file name .jar)"
            );
            return;
        }
        Implementor implementor = new Implementor();
        String className;
        String rootPath;

        boolean implementJar = args[0].equals("-jar");

        if (implementJar) {
            if (args.length != 3 || args[1] == null || args[2] == null) {
                System.out.println("2 arguments after -jar required: <full name of class to implement> " +
                        "<path to jar file>");
                return;
            }
            className = args[1];
            rootPath = args[2];
        } else {
            if (args.length != 2 || args[1] == null) {
                System.out.println("First argument must me -jar, otherwise, two arguments must be given " +
                        "<full name of class to implement> <path to root directory>");
            }
            className = args[0];
            rootPath = args[1];
        }
        try {
            if (implementJar) {
                implementor.implementJar(Class.forName(className), Paths.get(rootPath));
            } else {
                implementor.implement(Class.forName(className), Paths.get(rootPath));
            }
        } catch (InvalidPathException e) {
            System.out.println("Path to output file is invalid " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find class to implement " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("Error implementing class: " + e.getMessage());
        }
    }

//    public static void main(String[] args) throws ImplerException {
//        if (args.length != 1 && args.length != 3
//                || args.length == 3 && (!args[0].equals("-jar") || !args[2].endsWith(".jar"))) {
//            System.out.println(
//                    "Expected single argument(class name to implement) " +
//                            "or 3 arguments (\"-jar\", class to implement and output file name .jar)"
//            );
//        }
//        Implementor impler = new Implementor();
//        Class<?> token;
//        try {
//            token = Class.forName(args.length == 1 ? args[0] : args[1]);
//        } catch (ClassNotFoundException e) {
//            System.out.println("Specified class cannot be found or loaded");
//            return;
//        }
//        if (args.length == 3) {
//            Path outPath;
//            try {
//                outPath = Paths.get(args[2]);
//            } catch (InvalidPathException e) {
//                System.out.println("jar file path is not actually a system-correct path");
//                return;
//            }
//            impler.implementJar(token, outPath);
//        } else {
//            impler.implementJar(token, Paths.get(""));
//        }
//    }

}
