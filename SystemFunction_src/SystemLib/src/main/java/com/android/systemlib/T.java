package com.android.systemlib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class T {
    public static void copyFiles(String inputname, String outputname) throws IOException {
        File f = new File(inputname);
        if ((!f.exists()) || (!f.isDirectory())) {
            return;
        }
        // create output folder.
        if (!outputname.endsWith(File.separator)) {
            outputname = outputname + File.separator;
        }
        (new File(outputname)).mkdirs();
        // copy file.
        File[] file = f.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isFile()) {
                file[i].toString();
                FileInputStream input = new FileInputStream(file[i]);
                // mkdir if destination does not exist
                File outtest = new File(outputname);
                if (!outtest.exists()) outtest.mkdir();
                FileOutputStream output = new FileOutputStream(outputname + (file[i].getName()).toString());
                byte[] b = new byte[1024 * 5];
                int len;
                while ((len = input.read(b)) != -1) {
                    output.write(b, 0, len);
                }
                output.flush();
                output.close();
                input.close();
            } else if (file[i].isDirectory()) {
                System.out.println(file[i].toString() + " -> " + outputname + file[i].getName());
                copyFiles(file[i].toString(), outputname + file[i].getName());
            }
        }
    }
}
