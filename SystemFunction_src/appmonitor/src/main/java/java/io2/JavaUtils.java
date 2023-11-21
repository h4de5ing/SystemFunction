package java.io2;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JavaUtils {

    public static void write(String path, String context) {
        try {
            Files.write(Paths.get(path), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void append(String path, String context) {
        try {
            Files.write(Paths.get(path), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
