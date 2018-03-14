package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    class MethodSignature {

        Method method;

        MethodSignature(Method method) {
            this.method = method;
        }

        String getName() {
            return method.getName();
        }

        Class<?> getReturnType() {
            return method.getReturnType();
        }

        List<Class<?>> getArgs() {
            return Arrays.asList(method.getParameterTypes());
        }

        private String joinToString(List<Class<?>> list) {
            return list
                    .stream()
                    .map(Class::toString)
                    .collect(Collectors.joining(", "));
        }

        private String getModifiers() {
            return Modifier.toString(method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
        }

        private String getThrows() {
            List<Class<?>> exceptions = Arrays.asList(method.getExceptionTypes());
            return exceptions.isEmpty() ? "" : "throws " + joinToString(exceptions);
        }

        @Override
        public String toString(){
            Class<?> returnType = getReturnType();
            return String.format(
                    "%s %s %s(%s) %s { return %s; }%n",
                    getModifiers(),
                    returnType,
                    getName(),
                    joinToString(getArgs()),
                    getThrows(),
                    returnType.isPrimitive()
                            ? (returnType.equals(boolean.class) ? "false" : "0")
                            : returnType.equals(void.class) ? "" : "null"
            );
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodSignature)) {
                return false;
            }
            MethodSignature other = (MethodSignature) obj;
            return getName().equals(other.getName())
                    && getReturnType().equals(other.getReturnType())
                    && getArgs().equals(other.getArgs());
        }


    }

    enum Implemented {
        NEVER, IN_CLASS, IN_INTERFACE, IN_CLASS_AND_IN_INTERFACE;

        boolean inClass() {
            return this == IN_CLASS || this == IN_CLASS_AND_IN_INTERFACE;
        }

        boolean inInterface() {
            return this == IN_INTERFACE || this == IN_CLASS_AND_IN_INTERFACE;
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

    private HashMap<MethodSignature, Implemented> getMethodsInfo(Class<?> c) {
        if (c == null) {
            return new HashMap<>();
        }
        HashMap<MethodSignature, Implemented> result = getMethodsInfo(c.getSuperclass());
        Arrays.stream(c.getInterfaces())
                .map(this::getMethodsInfo)
                .forEach(methods ->
                        methods.forEach((key, value) ->
                                addMethod(result, key, value)
                        )
                );
        Arrays.stream(c.getDeclaredMethods()).forEach(method -> {
            addMethod(
                    result,
                    new MethodSignature(method),
                    !Modifier.isAbstract(method.getModifiers())
                            ? (c.isInterface() ? Implemented.IN_INTERFACE : Implemented.IN_CLASS)
                            : Implemented.NEVER
            );
        });
        return result;
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(
                        root.toString() + "/" + token.getPackage().toString().replace('.', '/')
                ),
                StandardCharsets.UTF_8
        ))) {
            output.write("class " + token.getSimpleName() + "Impl " + (token.isInterface() ? "implements " : "extends ") + token.getSimpleName() + " {");
            getMethodsInfo(token)
                    .entrySet()
                    .stream()
                    .filter(it -> it.getValue().neverImplemented())
                    .map(Map.Entry::getKey)
                    .forEach(method -> {
                        try {
                            output.write(method.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            output.write("}");
        } catch (IOException ignored) {

        }
    }
}
