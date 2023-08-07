package moe.lyniko.hiderecent.app;

import static android.content.Context.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import androidx.core.content.ContextCompat;
import moe.lyniko.hiderecent.R;

@SuppressLint({"ApplySharedPref", "WorldReadableFiles"})
public class AppSettings {

    private static final String TAG = "hide_recent_";
    private static final String config_name = "config";
    private static final String configFileName = "app_config";
    public final static int SYSTEM_VIEW = 0;
    public final static int USER_VIEW = 1;
    public final static int MODE = 1;
    public final static int NO_SET = -1;
    private String content;

    public static SharedPreferences configData(Context context){
        return context.getSharedPreferences(config_name, MODE_WORLD_READABLE);
    }

    public static void deleteSelection(Context context, String pkgn){
        SharedPreferences sharedPreferences = configData(context);
        String Mode = sharedPreferences.getString("Mode", "");
        putMode(sharedPreferences, remove(Mode, pkgn));
    }

    private static String remove(String contentText, String packageName){
        String package_Name = String.format("#%s#", packageName);
        if (!contentText.contains(package_Name)) return contentText;
        return contentText.replace(package_Name, "");
    }

    private static String getMode(SharedPreferences sharedPreferences){
        return sharedPreferences.getString("Mode", "");
    }

    private static void putMode(SharedPreferences sharedPreferences, String content){
        sharedPreferences.edit().putString("Mode", content).apply();
    }

    public static void saveMode(Context context, String packageName){
        SharedPreferences sharedPreferences = configData(context);
        String ContentText = sharedPreferences.getString("ode", "");
        String package_Name = String.format("#%s#", packageName);
        if (ContentText.contains(package_Name)) return;
        ContentText = String.format("%s%s", ContentText, package_Name);
        putMode(sharedPreferences, ContentText);
    }

    public static int getSetMode(Context context, String packageName){
        SharedPreferences sharedPreferences = configData(context);
        String Mode = sharedPreferences.getString("Mode", "");
        String package_Name = String.format("#%s#", packageName);
        if (Mode.contains(package_Name)) return MODE;
        return NO_SET;
    }

    public static int getOnSwitchListView(Context context) {
        return configData(context).getInt("onSwitchListView", USER_VIEW);
    }

    public static void savonSwitch(Context context,int value) {
        configData(context).edit().putInt("onSwitchListView", value).apply();
    }

    public static void setStatusBarColor(Activity activity, int color){
        activity.getWindow().setStatusBarColor(
                ContextCompat.getColor(activity.getApplicationContext(), color));
    }

    public static void setActivityStatusBar(Activity activity){
        setStatusBarColor(activity, R.color.toolbar);
        if(activity.getApplicationContext().getResources().getConfiguration().uiMode == 0x11)
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    public static String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
