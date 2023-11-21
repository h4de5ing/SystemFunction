package java.io2;

import android.content.Context;
import android.content.Intent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class AndroidTest {
    private String path = "测试";

    public void testFile() {
        if (path != null) {
            try {
                String packageName = "";
                try {
                    List<String> packages = null;
                    try {
                        Path file = Paths.get(J2.packagePath);
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
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                String context = System.currentTimeMillis() + "," + packageName + ",createNewFile=" + path + "\n";
                Files.write(Paths.get("/sdcard/log"), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    public void testURL() {
        try {
            if (this.toString().startsWith("http")) {
                String packageName = Thread.currentThread().getStackTrace()[5].getClassName();
                String context = System.currentTimeMillis() + "," + packageName + ",openConnection=" + this + "\n";
                Files.write(Paths.get("/sdcard/log"), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void testSocket() {
        try {
            String packageName = Thread.currentThread().getStackTrace()[5].getClassName();
            //String context = System.currentTimeMillis() + "," + packageName + ",SocketgetInputStream=" + getRemoteSocketAddress() + "\n";
            //Files.write(Paths.get("/sdcard/log"), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private String mCallingPackage = "com.android.test";
    private String permissionGroupName = "aaa";
    private int grantResult = 0;

    //./vendor/mediatek/proprietary/packages/apps/PermissionController/src/com/android/permissioncontroller/permission/ui/GrantPermissionsActivity.java
    public void testOps(Context context2) {
        try {
            Intent intent = new Intent("com.android.permissioncontroller.ops");
            intent.putExtra("packageName", mCallingPackage);
            intent.putExtra("perminsss", permissionGroupName);
            intent.putExtra("status", grantResult);
            context2.sendBroadcast(intent);
            //String context = System.currentTimeMillis() + ",ops=" + mCallingPackage + "," + permissionGroupName + "," + grantResult + "\n";
            //Files.write(Paths.get("/sdcard/log"), context.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
