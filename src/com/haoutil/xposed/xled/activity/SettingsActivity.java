package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.haoutil.xposed.xled.R;

public class SettingsActivity extends Activity {
	private static String versionName;
	private static OnPreferenceClickListener onPreferenceClickListener;
	private static OnPreferenceChangeListener onPreferenceChangeListener;
	
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
                String key = preference.getKey();
                if (key.equals("pref_screenon_enable")) {
                    Intent intent = new Intent("com.haoutil.xposed.xled.UPDATE_SCREENON_LED");
                    intent.putExtra("enable", (Boolean) newValue);
                    SettingsActivity.this.sendBroadcast(intent);
                } else if (key.equals("pref_icon")) {
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(SettingsActivity.this, "com.haoutil.xposed.xled.activity.ShowIcon"),
                            ((Boolean) newValue) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
				
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
            getPreferenceManager().findPreference("pref_icon").setOnPreferenceChangeListener(onPreferenceChangeListener);
			getPreferenceManager().findPreference("pref_app_info").setSummary(getString(R.string.app_info_sum).replace("#version#", versionName));
		}
	}
}
