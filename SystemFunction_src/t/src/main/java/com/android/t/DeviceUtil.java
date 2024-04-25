package com.android.t;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DeviceUtil {
    public static float getCpuTemp() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return Float.parseFloat(line) / 1000.0f;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }
}
