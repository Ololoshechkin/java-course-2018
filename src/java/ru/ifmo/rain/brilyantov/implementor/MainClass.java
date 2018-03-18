package ru.ifmo.rain.brilyantov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Paths;


public class MainClass {
    interface I {
        void i();
        default void common() {}
    }

    interface J {
        void j();
        default void common() {}
        default void j_impled() {

        }
    }

    abstract class IJ implements I, J {
        @Override
        public void common() {}
        abstract void ij();
        void ij_impled() {

        }
    }

    abstract class C extends IJ {
        void f() {

        }

        C(String s) {

        }

        public abstract String c(int a, String[] b, Object[] c);
    }

    public static void main(String[] args) {
        Implementor impl = new Implementor();
        try {
            impl.implement(
                    C.class,
                    Paths.get("/","Users","Vadim", "Documents", "coding", "java", "java-advanced-2018", "4_out")
            );
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }
}
