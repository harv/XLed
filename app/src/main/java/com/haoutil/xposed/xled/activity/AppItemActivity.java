package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.haoutil.xposed.xled.R;
import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.SettingsHelper;

public class AppItemActivity extends Activity {
    private static Context mContext;

    private static SettingsHelper settingsHelper;

    private static CheckBoxPreference prefEnable;
    private static CheckBoxPreference prefDisableDefault;
    private static CheckBoxPreference prefForceEnable;
    private static Preference prefColor;
    private static EditTextPreference prefOnms;
    private static EditTextPreference prefOffms;
    private static Preference prefReset;

    private static String appName;
    private static String packageName;

    private static boolean enable;
    private static boolean disableDefault;
    private static boolean forceEnable;
    private static int color;
    private static int onms;
    private static int offms;

    private static OnPreferenceClickListener onColorClickListener;
    private static OnPreferenceClickListener onResetClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        }

        mContext = getApplicationContext();

        settingsHelper = new SettingsHelper(AppItemActivity.this.getApplicationContext());

        appName = getIntent().getStringExtra("appName");
        setTitle(getString(R.string.app_config_name) + "[" + appName + "]");

        packageName = getIntent().getStringExtra("packageName");

        enable = settingsHelper.getBoolean("pref_app_enable_" + packageName, false);
        disableDefault = settingsHelper.getBoolean("pref_app_disableled_" + packageName, false);
        forceEnable = settingsHelper.getBoolean("pref_app_forceenable_" + packageName, false);
        color = settingsHelper.getInt("pref_app_color_" + packageName, Constant.INVALID_COLOR);
        onms = settingsHelper.getInt("pref_app_onms_" + packageName, Constant.INVALID_ONMS);
        offms = settingsHelper.getInt("pref_app_offms_" + packageName, Constant.INVALID_OFFMS);

        onColorClickListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(AppItemActivity.this.getApplicationContext(), ColorPickerActivity.class);
                intent.putExtra("originColor", settingsHelper.getInt("pref_app_color_" + packageName, Constant.DEFAULT_COLOR));
                AppItemActivity.this.startActivityForResult(intent, 0);

                return false;
            }
        };

        onResetClickListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                color = Constant.INVALID_COLOR;
                onms = Constant.INVALID_ONMS;
                offms = Constant.INVALID_OFFMS;

                settingsHelper.remove("pref_app_color_" + packageName);
                settingsHelper.remove("pref_app_onms_" + packageName);
                settingsHelper.remove("pref_app_offms_" + packageName);

                prefOnms.setText("");
                prefOffms.setText("");

                Toast.makeText(AppItemActivity.this, getString(R.string.tip_reset), Toast.LENGTH_SHORT).show();

                return false;
            }
        };
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("enable", settingsHelper.getBoolean("pref_app_enable_" + packageName, false));
        intent.putExtra("disableLED", settingsHelper.getBoolean("pref_app_disableled_" + packageName, false));
        intent.putExtra("color", settingsHelper.getInt("pref_app_color_" + packageName, Constant.DEFAULT_COLOR));
        AppItemActivity.this.setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String sColor = data.getStringExtra("color");
            if (TextUtils.isEmpty(sColor)) {    // leave blank to use default value
                settingsHelper.remove("pref_app_color_" + packageName);
            } else {
                try {
                    color = Color.parseColor(sColor);
                    settingsHelper.setInt("pref_app_color_" + packageName, color);
                } catch (Exception e) {
                    Toast.makeText(mContext, getString(R.string.tip_incorrect_colorformat), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.appitem);

            prefEnable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_enable");
            prefEnable.setChecked(enable);
            prefEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enable = (Boolean) newValue;
                    settingsHelper.setBoolean("pref_app_enable_" + packageName, enable);
                    prefEnable.setChecked(enable);
                    prefDisableDefault.setEnabled(enable);

                    return false;
                }
            });

            prefDisableDefault = (CheckBoxPreference) getPreferenceManager().findPreference("pref_disable_led");
            prefDisableDefault.setChecked(disableDefault);
            prefDisableDefault.setEnabled(enable);
            prefDisableDefault.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enable = (Boolean) newValue;
                    settingsHelper.setBoolean("pref_app_disableled_" + packageName, enable);
                    prefDisableDefault.setChecked(enable);

                    return false;
                }
            });

            prefForceEnable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_forceenable");
            prefForceEnable.setChecked(forceEnable);
            prefForceEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enable = (Boolean) newValue;
                    settingsHelper.setBoolean("pref_app_forceenable_" + packageName, enable);
                    prefForceEnable.setChecked(enable);

                    return false;
                }
            });

            prefColor = getPreferenceManager().findPreference("pref_color");
            prefColor.setOnPreferenceClickListener(onColorClickListener);

            prefOnms = (EditTextPreference) getPreferenceManager().findPreference("pref_onms");
            if (onms != Constant.INVALID_ONMS) {
                prefOnms.setText(String.valueOf(onms));
            }
            prefOnms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String onms = (String) newValue;
                    if (TextUtils.isEmpty(onms)) {  // leave blank to use default value
                        settingsHelper.remove("pref_app_onms_" + packageName);
                        prefOnms.setText("");
                    } else {
                        try {
                            settingsHelper.setInt("pref_app_onms_" + packageName, Integer.parseInt(onms));
                            prefOnms.setText(onms);
                        } catch (Exception e) {
                            Toast.makeText(mContext, getString(R.string.tip_incorrect_onmsformat), Toast.LENGTH_SHORT).show();
                        }
                    }
                    return false;
                }
            });

            prefOffms = (EditTextPreference) getPreferenceManager().findPreference("pref_offms");
            if (offms != Constant.INVALID_OFFMS) {
                prefOffms.setText(String.valueOf(offms));
            }
            prefOffms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String offms = (String) newValue;
                    if (TextUtils.isEmpty(offms)) { // leave blank to use default value
                        settingsHelper.remove("pref_app_offms_" + packageName);
                        prefOffms.setText("");
                    } else {
                        try {
                            settingsHelper.setInt("pref_app_offms_" + packageName, Integer.parseInt(offms));
                            prefOffms.setText(offms);
                        } catch (Exception e) {
                            Toast.makeText(mContext, getString(R.string.tip_incorrect_offmsformat), Toast.LENGTH_SHORT).show();
                        }
                    }
                    return false;
                }
            });

            prefReset = getPreferenceScreen().findPreference("pref_reset");
            prefReset.setOnPreferenceClickListener(onResetClickListener);
        }
    }
}
