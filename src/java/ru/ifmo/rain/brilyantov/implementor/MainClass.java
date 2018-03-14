package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Path;
import java.nio.file.Paths;

interface I {
    void i();
}

interface J {
    void j();
    default void j_impled() {

    }
}

abstract class IJ {
    abstract void ij();
    void ij_impled() {

    }
}

abstract class C extends IJ {
    void f() {

    }
    public abstract String c(int a, String b);
}


public class MainClass {
    public static void main(String[] args) {
        Implementor impl = new Implementor();
        try {
            impl.implement(C.class, Paths.get("Users", "Vadim", "Documents", "coding", "java", "java-advanced-2018"));
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }
}
