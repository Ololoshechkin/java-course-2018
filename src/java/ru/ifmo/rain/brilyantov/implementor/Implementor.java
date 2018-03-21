package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Implementor implements Impler {

    private static <T> String joinToString(List<T> list, Function<T, String> transform) {
        return list
                .stream()
                .map(transform)
                .collect(Collectors.joining(", "));
    }

    private static String getThrows(Executable method) {
        List<Class<?>> exceptions = Arrays.asList(method.getExceptionTypes());
        return exceptions.isEmpty() ? "" : "throws " + joinToString(exceptions, Class::getCanonicalName);
    }

    private static String getArguments(Executable method, boolean typed) {
        final int[] varName = {0};
        return joinToString(
                Arrays.stream(method.getParameters())
                        .map(arg -> (typed ? arg.getType().getCanonicalName() + " " : "") + arg.getName())
                        .collect(Collectors.toList()),
                Function.identity()
        );
    }

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

    class MethodSignature {

        Method method;

        MethodSignature(Method method) {
            this.method = method;
        }

        private String getName() {
            return method.getName();
        }

        private Class<?> getReturnType() {
            return method.getReturnType();
        }

        private Class<?>[] getArgs() {
            return method.getParameterTypes();
        }

        @Override
        public String toString() {
            return Implementor.getMethodImpl(method, null);
        }

        @Override
        public int hashCode() {
            return getName().hashCode() * 31 * 31
                    + Arrays.hashCode(getArgs()) * 31
                    + getReturnType().hashCode();
        }

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

    enum Implemented {
        NEVER, IN_CLASS, IN_INTERFACE;

        boolean inClass() {
            return this == IN_CLASS;
        }

        boolean inInterface() {
            return this == IN_INTERFACE;
        }

        boolean neverImplemented() {
            return this == NEVER;
        }
    }

    private void addMethod(
            HashMap<MethodSignature, Implemented> result,
            MethodSignature method,
            Implemented methodImplemented
    ) {
        Implemented resultImplemented = result.getOrDefault(method, Implemented.NEVER);
        if (resultImplemented.neverImplemented() || methodImplemented.neverImplemented()) {
            result.put(method, resultImplemented.neverImplemented() ? methodImplemented : resultImplemented);
        } else {
            if (resultImplemented.inInterface() || methodImplemented.inInterface()) {
                result.put(method, Implemented.NEVER);
            } else if (resultImplemented.inClass() && methodImplemented.inClass()) { // new override
                result.put(method, methodImplemented);
            } else {
                throw new IllegalStateException("-_-");
            }
        }
    }

    private HashSet<MethodSignature> getMethodSignatures(Class<?> currentClass) {
        HashSet<MethodSignature> methods = new HashSet<>();
        Arrays
                .stream(currentClass.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodSignature::new)
                .collect(Collectors.toCollection(() -> methods));
        while (currentClass != null) {
            Arrays
                    .stream(currentClass.getDeclaredMethods())
                    .filter(method -> Modifier.isAbstract(method.getModifiers()))
                    .map(MethodSignature::new)
                    .collect(Collectors.toCollection(() -> methods));
            currentClass = currentClass.getSuperclass();
        }
        return methods;
    }

    private void compileFiles(Path root, String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root + File.pathSeparator + System.getProperty("java.class.path"));
        compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    private class FileDeleter extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
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

    private static String getImplInterfacePath() {

    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path root = Paths.get(".");
            JarImpler implementor = new Implementor();
            implementor.implement(token, root);
            Path javaFilePath = getImplInterfacePath(token, root);
            Path classFilePath = getImplInterfaceJarPath(token, root);
            compileFiles(root, javaFilePath.toString());
            jarWrite(jarFile, classFilePath);
            getImplInterfaceJarPath(token, root).toFile().deleteOnExit();
        } catch (IOException e) {
            System.out.print("ERROR: can't");
        }
    }

    private void addMissingConstructors(Constructor[] construcors, String newClassName, StringBuffer output) throws ImplerException {
        List<Constructor> constructors = Arrays.stream(construcors).filter(constr -> !Modifier.isPrivate(constr.getModifiers())).collect(Collectors.toList());
        if (constructors.size() == 0) {
            throw new ImplerException("superclass has no accessible constructors");
        }
        constructors.forEach(constr -> output.append(getMethodImpl(constr, newClassName)));
    }

    private static String packageNameFor(Class<?> token) {
        return token.getPackage() != null ? token.getPackage().getName() : "";
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
        String newClassName = token.getSimpleName() + "Impl";
        Path containingDirectory = root.resolve(packageName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(containingDirectory);
        } catch (IOException e) {
            throw new ImplerException("failed to create parent directory for output java-file", e);
        }
        Path resultPath = containingDirectory.resolve(newClassName + ".java");
        try (BufferedWriter output = Files.newBufferedWriter(resultPath)) {
            StringBuffer resultBuffer = new StringBuffer();
            if (!packageName.isEmpty()) {
                resultBuffer
                        .append("package ")
                        .append(packageName)
                        .append(";\n");//.append(File.separator);
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

    public static void main(String[] args) throws ImplerException {
        if (args.length != 1 && args.length != 3
                || args.length == 3 && (!args[0].equals("-jar") || !args[2].endsWith(".jar"))) {
            System.out.println(
                    "Expected single argument(class name to implement) " +
                            "or 3 arguments (\"-jar\", class to implement and output file name .jar)"
            );
        }
        Implementor impler = new Implementor();
        Class<?> token;
        try {
            token = Class.forName(args.length == 1 ? args[0] : args[1]);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Specified class cannot be found or loaded");
        }
        impler.implement(token, Paths.get(""));
    }

}
