package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.haoutil.xposed.xled.R;

public class SettingsActivity extends Activity {
	private static String versionName;
	private static OnPreferenceClickListener onPreferenceClickListener;
	private static OnPreferenceChangeListener onPreferenceChangeListener;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Intent intent = new Intent("com.haoutil.xposed.xled.UPDATE_SCREENON_LED");
                    intent.putExtra("enable", (Boolean) msg.obj);
                    SettingsActivity.this.sendBroadcast(intent);
                    break;
                case 1:
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(SettingsActivity.this,"com.haoutil.xposed.xled.activity.ShowIcon"),
                            ((Boolean) msg.obj) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    break;
                case 2:
                    SettingsActivity.this.sendBroadcast(new Intent("com.haoutil.xposed.xled.UPDATE_CHARGING_LED"));
                    break;
            }
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
		}
		
		try {
			versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {}
		
		onPreferenceClickListener = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("pref_app_config")) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, AppListActivity.class));
                } else if (preference.getKey().equals("pref_charging_led")) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ChargingActivity.class));
                }
				
				return false;
			}
		};
		
		onPreferenceChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
                Message msg = new Message();

                String key = preference.getKey();
                if (key.equals("pref_screenon_enable")) {
                    msg.what = 0;
                    msg.obj = newValue;
                } else if (key.equals("pref_icon")) {
                    msg.what = 1;
                    msg.obj = newValue;
                } else if (key.equals("pref_sleep") || key.equals("pref_sleep_start") || key.equals("pref_sleep_end")) {
                    msg.what = 2;
                }

                // delay 100ms for waiting value persisting.
                mHandler.sendMessageDelayed(msg, 100);
				
				return true;
			}
		};
	}

	public static class PrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

			addPreferencesFromResource(R.xml.preferences);
			
			getPreferenceManager().findPreference("pref_app_config").setOnPreferenceClickListener(onPreferenceClickListener);
            getPreferenceManager().findPreference("pref_charging_led").setOnPreferenceClickListener(onPreferenceClickListener);
            getPreferenceManager().findPreference("pref_screenon_enable").setOnPreferenceChangeListener(onPreferenceChangeListener);
            getPreferenceManager().findPreference("pref_sleep").setOnPreferenceChangeListener(onPreferenceChangeListener);
            getPreferenceManager().findPreference("pref_sleep_start").setOnPreferenceChangeListener(onPreferenceChangeListener);
            getPreferenceManager().findPreference("pref_sleep_end").setOnPreferenceChangeListener(onPreferenceChangeListener);
            getPreferenceManager().findPreference("pref_icon").setOnPreferenceChangeListener(onPreferenceChangeListener);
			getPreferenceManager().findPreference("pref_app_info").setSummary(getString(R.string.app_info_sum).replace("#version#", versionName));
		}
	}
}
