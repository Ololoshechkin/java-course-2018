package ru.ifmo.rain.brilyantov.walk;

        import java.nio.file.InvalidPathException;
        import java.nio.file.Paths;

public class MyTester {
    public static void main(String[] args) {
        String p = "?";
        try {
            Paths.get(p);
        } catch (InvalidPathException e) {
            System.out.println("EXC");
            System.out.println(e.getMessage());
        }
    }
}
