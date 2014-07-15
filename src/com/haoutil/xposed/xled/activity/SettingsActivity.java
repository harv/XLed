package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.haoutil.xposed.xled.R;

public class SettingsActivity extends Activity {
	private static String versionName;
	private static OnPreferenceClickListener onPreferenceClickListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
		}
		
		try {
			PackageManager manager = this.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
			versionName = info.versionName;
		} catch (NameNotFoundException e) {}
		
		onPreferenceClickListener = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference paramPreference) {
				Intent intent = new Intent(SettingsActivity.this.getApplicationContext(), AppListActivity.class);
				SettingsActivity.this.startActivity(intent);
				
				return false;
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
			getPreferenceManager().findPreference("pref_app_info").setSummary("Version: v" + versionName + "\nAuthor: Harv Chen(ch05042210@gmail.com)");
		}
	}
}
