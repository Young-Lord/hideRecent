package ss.colytitse.setappfull;

import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SuperUser {

    private static final String TAG = "test_";

    public static String execShell(String cmd) {
        StringBuilder result = new StringBuilder();
        Process process = null;
        String line;
        try {
            process = Runtime.getRuntime().exec("su -c " + cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null)
                result.append(line).append("\n");
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void copyConfigFile(boolean is){
        String user_path = Environment.getDataDirectory() + "/data/" + BuildConfig.APPLICATION_ID + "/shared_prefs/";
        String sys_path = "/data/system/shared_prefs/";
        String file = "config.xml";
        if (is) {
            execShell("mkdir " + user_path);
            execShell(String.format("\\cp %s%s %s%s", sys_path, file, user_path, file));
        }else {
            execShell("mkdir " + sys_path);
            execShell(String.format("\\cp %s%s %s%s", user_path, file, sys_path, file));
        }
        execShell(String.format("chmod 644 %s%s", sys_path,file));
    }

    public static void delete(){
        execShell(String.format("rm -f %s/data/%s/shared_prefs/config.xml",
                Environment.getDataDirectory(),BuildConfig.APPLICATION_ID));
    }
}