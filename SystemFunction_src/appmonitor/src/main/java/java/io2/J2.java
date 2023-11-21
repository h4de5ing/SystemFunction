package java.io2;

import com.android.appmonitor.SystemLogKt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class J2 {
    public static final String packagePath = "/data/local/tmp/packages";

    public void test2() {
        SystemLogKt.setAppPermissionRequestListener((s, s2, integer) -> {
            System.out.println("广播:包名=" + s + ",权限=" + s2 + ",状态=" + integer);
            return null;
        });
    }

    public void getPackageName() {
        String packageName = "";
        try {
            List<String> packages = null;
            try {
                Path file = Paths.get(packagePath);
                packages = Files.readAllLines(file);
            } catch (Exception e) {
                //e.printStackTrace();
            }
            List<String> classNames = new ArrayList<>();
            try {
                for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                    classNames.add(stackTraceElement.getClassName());
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (packages != null) {
                for (String className : classNames) {
                    String[] splits = className.split("\\.");
                    for (int i = 2; i < splits.length; i++) {
                        StringBuilder builder = new StringBuilder();
                        for (int j = 0; j < i; j++) {
                            builder.append(splits[j]);
                            if (j + 1 < i) builder.append(".");
                        }
                        if (packages.contains(builder.toString())) {
                            packageName = builder.toString();
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
