package moe.lyniko.hiderecent;

import static moe.lyniko.hiderecent.app.AppSettings.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import moe.lyniko.hiderecent.app.AppInfoAdapter;
import moe.lyniko.hiderecent.app.AppSettings;

@SuppressLint({"UseSwitchCompatOrMaterialCode","UseCompatLoadingForDrawables","SetTextI18n"})
public class MainActivity extends Activity {

    private static final String TAG = "hide_recent_";
    private List<PackageInfo> systemAppList;
    private List<PackageInfo> userAppList;
    private boolean EditSearchInit = false;
    private Context mContext;

    private void initApplicationList() {
        List<PackageInfo> allAppList = getPackageManager().getInstalledPackages(0);
        systemAppList = new ArrayList<>();
        userAppList = new ArrayList<>();
        for (PackageInfo app : allAppList){
            if ((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                userAppList.add(app);
            else systemAppList.add(app);
        }
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityStatusBar(this);
        setContentView(R.layout.main_layout);
        mContext = getApplicationContext();
        initApplicationList();
        initMainActivity();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initMainActivity() {
        int AppViewList = getOnSwitchListView(mContext);
        TextView appListName = findViewById(R.id.app_list_name);
        appListName.setText(String.format("(%s)",getResources().getString(
                AppViewList == USER_VIEW ? R.string.list_user : R.string.list_system)));
        initMainActivityListView(AppViewList == USER_VIEW ? userAppList : systemAppList);
        EditText edit_insearch = findViewById(R.id.edit_insearch);
        initEditaTextAction(edit_insearch);
        edit_insearch.setOnEditorActionListener((textView, actionId, keyEvent)
        -> {
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(textView.getWindowToken(), 0);
            return true;
        });
    }

    public void initMainActivityListView(List<PackageInfo> appList){
        ListView listView = findViewById(R.id.app_items);
        listView.setAdapter(new AppInfoAdapter(appList, mContext));
        listView.setOnItemClickListener((adapterView, view, i, l)
        -> {
            TextView app_pkgn = view.findViewById(R.id.app_pkgn);
            String pkgn = app_pkgn.getText().toString();
            LinearLayout item_bac = view.findViewById(R.id.item_root_view);
//            TextView mode_text = view.findViewById(R.id.mode_state);
            Switch onSwitch = view.findViewById(R.id.set_switch);
            if(!onSwitch.isChecked()){
                onSwitch.setChecked(true);
//                item_bac.setBackground(mContext.getResources().getDrawable(R.drawable.button_background2, mContext.getTheme()));
                saveMode(mContext, pkgn);
            }else {
                onSwitch.setChecked(false);
                deleteSelection(mContext, pkgn);
            }
        });
    }

    public void onSwitchListView(View view) {
        EditText edit_insearch = findViewById(R.id.edit_insearch);
        String inText = edit_insearch.getText().toString();
        TextView appListName = findViewById(R.id.app_list_name);
        if(getOnSwitchListView(mContext) == USER_VIEW){
            savonSwitch(mContext ,SYSTEM_VIEW);
            initMainActivityListView(searchAppView(inText));
            appListName.setText(String.format("(%s)", getResources().getString(R.string.list_system)));
        }else {
            savonSwitch(mContext, USER_VIEW);
            initMainActivityListView(searchAppView(inText));
            appListName.setText(String.format("(%s)", getResources().getString(R.string.list_user)));
        }
    }

    private void initEditaTextAction(EditText edit_insearch) {
        if (EditSearchInit) return;
        edit_insearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                String inText = editable.toString();
                initMainActivityListView(searchAppView(inText));
            }
        });
        EditSearchInit = true;
    }

    private List<PackageInfo> searchAppView(String intext) {
        List<PackageInfo> appList = getOnSwitchListView(mContext) == USER_VIEW ? userAppList : systemAppList;
        if (intext.length() < 1) return appList;
        intext = intext.toLowerCase();
        List<PackageInfo> result = new ArrayList<>();
        for (PackageInfo info : appList)
            if (info.packageName.toLowerCase().contains(intext) || getPackageManager().getApplicationLabel(info.applicationInfo).toString().toLowerCase().contains(intext))
                result.add(info);
        return result;
    }
}
