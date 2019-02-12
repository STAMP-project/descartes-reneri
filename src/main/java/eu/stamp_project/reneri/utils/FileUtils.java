package eu.stamp_project.reneri.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

public class FileUtils {

    public static void createEmptyDirectory(Path root) throws IOException {
        if (Files.exists(root)) {

            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
        Files.createDirectories(root);
    }


    public static void copyDirectory(final Path source, final Path target) throws IOException {
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes sourceBasic) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                if(Files.notExists(targetFile)) {
                    Files.copy(file, targetFile);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult
            visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void write(Path path, byte[] content) throws IOException {
        try(FileOutputStream output = new FileOutputStream(path.toFile())) {
            output.write(content);
        }
    }

    public static void write(Path path, InputStream stream) throws IOException {
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            copy(stream, output);
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[0x1000];
        int length;

        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

    }

    public static File[] getChildrenDirectories(Path root) {
        File rootDirectory = root.toFile();
        File[] directories = rootDirectory.listFiles((file) ->  file.isDirectory() && file.canRead());
        if(directories == null) {
            return new File[0];
        }
        return directories;
    }

}
