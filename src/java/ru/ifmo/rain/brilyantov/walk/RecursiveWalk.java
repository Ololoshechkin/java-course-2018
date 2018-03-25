package ru.ifmo.rain.brilyantov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("expected exactly 2 not-null program arguments (input path, output path)");
            return;
        }
        try {
            Paths.get(args[0]);
            Path out = Paths.get(args[1]);
            Path par = out.getParent();
            if (par != null)
                Files.createDirectories(par);
        } catch (IOException | SecurityException e) {
            System.out.println("Failed to create destination directories for output (" + e.getMessage() + ")");
        } catch (InvalidPathException e) {
            System.out.println("input paths are not paths (" + e.getMessage() + ")");
        }
        try (
                BufferedReader input = new BufferedReader(new InputStreamReader(
                        new FileInputStream(args[0]),
                        StandardCharsets.UTF_8
                ));
                PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(args[1]),
                        StandardCharsets.UTF_8
                )))
        ) {
            final int[] line = {0};
            input.lines().forEach(curPath -> {
                line[0]++;
                try {
                    Files.walkFileTree(Paths.get(curPath), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            output.printf(
                                    "%08x %s" + System.lineSeparator(),
                                    HashCounter.getFNV1Hash(file),
                                    file.toString()
                            );
                            if (output.checkError()) {
                                System.out.println("Output failed");
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            output.printf("%08x %s" + System.lineSeparator(), 0, file.toString());
                            if (output.checkError()) {
                                System.out.println("Output failed");
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException | SecurityException e) {
                    System.out.println("failed to read file from file " + curPath + " at line " + line[0]);
                } catch (InvalidPathException e) {
                    System.out.println("invalid path (at file " + curPath + " on line " + line[0] + ")");
                }
            });
        } catch (IOException | SecurityException e) {
            System.out.println("Failed to open input/output files" + e.getMessage() + ")");
        }
    }
}
