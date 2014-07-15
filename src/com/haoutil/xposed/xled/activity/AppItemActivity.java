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
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.haoutil.xposed.xled.R;
import com.haoutil.xposed.xled.util.SettingsHelper;

public class AppItemActivity extends Activity {
	private static Context mContext;
	
	private static SettingsHelper settingsHelper;
	
	private static CheckBoxPreference prefEnable;
	private static EditTextPreference prefColor;
	private static EditTextPreference prefOnms;
	private static EditTextPreference prefOffms;
	
	private static String appName;
	private static String packageName;
	
	private static boolean enable;
	private static int color;
	private static int onms;
	private static int offms;
	
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
		setTitle("LED config[" + appName + "]");
		
		packageName = getIntent().getStringExtra("packageName");
		
		enable = settingsHelper.getBoolean("pref_app_enable_" + packageName, false);
		color = settingsHelper.getInt("pref_app_color_" + packageName, Color.TRANSPARENT);
		onms = settingsHelper.getInt("pref_app_onms_" + packageName, settingsHelper.getInt("pref_led_onms", 300));
		offms = settingsHelper.getInt("pref_app_offms_" + packageName, settingsHelper.getInt("pref_led_offms", 1000));
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra("enable", settingsHelper.getBoolean("pref_app_enable_" + packageName, false));
		intent.putExtra("color", settingsHelper.getInt("pref_app_color_" + packageName, Color.TRANSPARENT));
		AppItemActivity.this.setResult(RESULT_OK, intent);
		
		super.onBackPressed();
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
					
					return false;
				}
			});
			
			prefColor = (EditTextPreference) getPreferenceManager().findPreference("pref_color");
			prefColor.setText(String.format("#%08X", (0xFFFFFFFF & color)));
			prefColor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String color = (String) newValue;
					try {
						settingsHelper.setInt("pref_app_color_" + packageName, Color.parseColor(color));
						prefColor.setText(color);
					} catch (Exception e) {
						Toast.makeText(mContext, "color format is incorrect!\nexample: #FF00FF00", Toast.LENGTH_SHORT).show();
					}
					
					return false;
				}
			});
			
			prefOnms = (EditTextPreference) getPreferenceManager().findPreference("pref_onms");
			prefOnms.setText(String.valueOf(onms));
			prefOnms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String onms = (String) newValue;
					try {
						settingsHelper.setInt("pref_app_onms_" + packageName, Integer.parseInt(onms));
						prefOnms.setText(onms);
					} catch (Exception e) {
						Toast.makeText(mContext, "onms format is incorrect!\nexample: 300", Toast.LENGTH_SHORT).show();
					}
					
					return false;
				}
			});
			
			prefOffms = (EditTextPreference) getPreferenceManager().findPreference("pref_offms");
			prefOffms.setText(String.valueOf(offms));
			prefOffms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String offms = (String) newValue;
					try {
						settingsHelper.setInt("pref_app_offms_" + packageName, Integer.parseInt(offms));
						prefOffms.setText(offms);
					} catch (Exception e) {
						Toast.makeText(mContext, "offms format is incorrect!\nexample: 1000", Toast.LENGTH_SHORT).show();
					}
					return false;
				}
			});
		}
	}
}
