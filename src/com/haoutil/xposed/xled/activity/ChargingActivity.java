package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.haoutil.xposed.xled.R;
import com.haoutil.xposed.xled.XposedMod;
import com.haoutil.xposed.xled.model.IntEditTextPreference;
import com.haoutil.xposed.xled.util.SettingsHelper;

public class ChargingActivity extends Activity {
    private static CheckBoxPreference lowDisable;
    private static CheckBoxPreference chargingLowDisable;
    private static CheckBoxPreference chargingMediumDisable;
    private static CheckBoxPreference chargingFullDisable;

    private static SettingsHelper settingsHelper;

    private static Preference.OnPreferenceClickListener onPreferenceClickListener;
    private static Preference.OnPreferenceChangeListener onPreferenceChangeListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ChargingActivity.this.sendBroadcast(new Intent("com.haoutil.xposed.xled.UPDATE_CHARGING_LED"));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        }

        settingsHelper = new SettingsHelper(ChargingActivity.this);

        onPreferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(ChargingActivity.this.getApplicationContext(), ColorPickerActivity.class);
                String key = preference.getKey();
                if (key.equals("pref_charging_low_color")) {
                    intent.putExtra("originColor", settingsHelper.getInt(key, XposedMod.DEFAULT_COLOR));
                    intent.putExtra("requestCode", 0);
                    ChargingActivity.this.startActivityForResult(intent, 0);
                } else if (key.equals("pref_charging_medium_color")) {
                    intent.putExtra("originColor", settingsHelper.getInt(key, XposedMod.DEFAULT_COLOR));
                    intent.putExtra("requestCode", 1);
                    ChargingActivity.this.startActivityForResult(intent, 1);
                } else if (key.equals("pref_charging_full_color")) {
                    intent.putExtra("originColor", settingsHelper.getInt(key, XposedMod.DEFAULT_COLOR));
                    intent.putExtra("requestCode", 2);
                    ChargingActivity.this.startActivityForResult(intent, 2);
                }

                return false;
            }
        };

        onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Message message = new Message();
                String key = preference.getKey();
                if (key.equals("pref_charging_enable")) {
                    boolean enable = (Boolean) o;

                    lowDisable.setEnabled(enable);
                    chargingLowDisable.setEnabled(enable);
                    chargingMediumDisable.setEnabled(enable);
                    chargingFullDisable.setEnabled(enable);

                    message.what = 10;
                } else if (key.equals("pref_low_disable")) {
                    message.what = 11;
                } else if (key.equals("pref_charging_low_disable")) {
                    message.what = 12;
                } else if (key.equals("pref_charging_medium_disable")) {
                    message.what = 13;
                } else if (key.equals("pref_charging_full_disable")) {
                    message.what = 14;
                } else if (key.equals("pref_charging_onms")) {
                    if (TextUtils.isEmpty(String.valueOf(o))) { // leave blank to use default value
                        settingsHelper.remove(key);
                        ((IntEditTextPreference) preference).setText("");
                        return false;
                    }
                    message.what = 3;
                } else if (key.equals("pref_charging_offms")) {
                    if (TextUtils.isEmpty(String.valueOf(o))) { // leave blank to use default value
                        settingsHelper.remove(key);
                        ((IntEditTextPreference) preference).setText("");
                        return false;
                    }
                    message.what = 4;
                }

                mHandler.sendMessageDelayed(message, 200);

                return true;
            }
        };
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                Message message = new Message();
                switch (data.getIntExtra("requestCode", -1)) {
                    case 0:
                        if (TextUtils.isEmpty(data.getStringExtra("color"))) {  // leave blank to use default value
                            settingsHelper.remove("pref_charging_low_color");
                        } else {
                            settingsHelper.setInt("pref_charging_low_color", Color.parseColor(data.getStringExtra("color")));
                        }
                        message.what = 0;
                        break;
                    case 1:
                        if (TextUtils.isEmpty(data.getStringExtra("color"))) {  // leave blank to use default value
                            settingsHelper.remove("pref_charging_medium_color");
                        } else {
                            settingsHelper.setInt("pref_charging_medium_color", Color.parseColor(data.getStringExtra("color")));
                        }
                        message.what = 1;
                        break;
                    case 2:
                        if (TextUtils.isEmpty(data.getStringExtra("color"))) {  // leave blank to use default value
                            settingsHelper.remove("pref_charging_full_color");
                        } else {
                            settingsHelper.setInt("pref_charging_full_color", Color.parseColor(data.getStringExtra("color")));
                        }
                        message.what = 2;
                        break;
                }

                mHandler.sendMessage(message);
            } catch (Exception e) {
                Toast.makeText(ChargingActivity.this, getString(R.string.tip_incorrect_colorformat), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.chargingled);

            final CheckBoxPreference enable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_charging_enable");
            enable.setOnPreferenceChangeListener(onPreferenceChangeListener);

            lowDisable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_low_disable");
            lowDisable.setOnPreferenceChangeListener(onPreferenceChangeListener);
            lowDisable.setEnabled(enable.isChecked());

            chargingLowDisable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_charging_low_disable");
            chargingLowDisable.setOnPreferenceChangeListener(onPreferenceChangeListener);
            chargingLowDisable.setEnabled(enable.isChecked());

            chargingMediumDisable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_charging_medium_disable");
            chargingMediumDisable.setOnPreferenceChangeListener(onPreferenceChangeListener);
            chargingMediumDisable.setEnabled(enable.isChecked());

            chargingFullDisable = (CheckBoxPreference) getPreferenceManager().findPreference("pref_charging_full_disable");
            chargingFullDisable.setOnPreferenceChangeListener(onPreferenceChangeListener);
            chargingFullDisable.setEnabled(enable.isChecked());

            final Preference chargingLowColor = getPreferenceManager().findPreference("pref_charging_low_color");
            chargingLowColor.setOnPreferenceClickListener(onPreferenceClickListener);

            final Preference chargingMediumColor = getPreferenceManager().findPreference("pref_charging_medium_color");
            chargingMediumColor.setOnPreferenceClickListener(onPreferenceClickListener);

            final Preference chargingFullColor = getPreferenceManager().findPreference("pref_charging_full_color");
            chargingFullColor.setOnPreferenceClickListener(onPreferenceClickListener);

            final Preference chargingOnms = getPreferenceManager().findPreference("pref_charging_onms");
            chargingOnms.setOnPreferenceChangeListener(onPreferenceChangeListener);

            final Preference chargingOffms = getPreferenceManager().findPreference("pref_charging_offms");
            chargingOffms.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
    }
}