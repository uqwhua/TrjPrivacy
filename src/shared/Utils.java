package shared;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class Utils {

    public static String getCurrentTime(){
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd-HH-mm-ss");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        return sdf.format(date);
    }

    public static Properties springUtil() {
        Properties props = new Properties();
        String path = System.getProperty("user.dir") + "/src/resources/config.properties";
        try (InputStream input = new FileInputStream(path)) {
            System.out.println("[PROGRESS] reading parameters from " + path);
            props.load(input);
            Map<String, String> key2value = new TreeMap<>();
            for (Object key : props.keySet()) {
                key2value.put(key.toString(), props.get(key).toString());
//                System.out.print(key + ": ");
//                System.out.println(props.get(key));
            }
            System.out.println("\n[Parameters]");
            for(String key: key2value.keySet()) {
                System.out.print(key + ": ");
                System.out.println(key2value.get(key));
            }
            System.out.println("------------------------------------------------------\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return props;
    }

    public static void showProgress(int idx, int total, String function) {
        final int progressSlice = 20;
        if(idx == 0){
            System.out.print("[PROGRESS] " + function + ": ");
        }

        double progressInd = Math.ceil(total * 1.0 / progressSlice);

        if (idx % progressInd == 0) {
            System.out.print((int)(100 * idx / (float) total) + "%..");
        }
    }

    public static String double2str(final double sc_radius) {
        String str = Double.toString(sc_radius).replace(".", "-");
        int idx = str.lastIndexOf("-");
        return str.substring(0, idx + 2);
    }
}
