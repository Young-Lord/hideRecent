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
import com.android.server.wm.Task;
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
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Task task = (Task) param.args[0];
                String packageName = task.getBaseIntent().getComponent().getPackageName();
                if (Mode.contains(packageName)){
                    param.setResult(false);
                }
            }
        };
        try{
            findAndHookMethod("com.android.server.wm.RecentTasks", lpparam.classLoader,
                    "isVisibleRecentTask","com.android.server.wm.Task",
                    MethodHook
            );
        }catch (Throwable ignored){}
    }
}
