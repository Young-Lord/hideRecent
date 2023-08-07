package ss.colytitse.setappfull;

import static android.view.WindowManager.LayoutParams.*;
import static de.robv.android.xposed.XposedHelpers.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.RequiresApi;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "test_";
    private static final String Mode;
    static {
        XSharedPreferences xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID,"config");
        xsp.makeWorldReadable();
        Mode = xsp.getString("Mode", "");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android"))
            onSystemMode(lpparam);
    }

    private void onSystemMode(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook MethodHook = new XC_MethodHook() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) getObjectField(param.args[0], "mAttrs");
                if (attrs.type > WindowManager.LayoutParams.LAST_APPLICATION_WINDOW)
                    return;
                if (SystemMode.contains(attrs.packageName))
                    attrs.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        };
        try{
            findAndHookMethod("com.android.server.wm.DisplayPolicy", lpparam.classLoader,
                    "layoutWindowLw","com.android.server.wm.WindowState",
                    "com.android.server.wm.WindowState", "com.android.server.wm.DisplayFrames",
                    MethodHook
            );
        }catch (Throwable ignored){}
    }
}
