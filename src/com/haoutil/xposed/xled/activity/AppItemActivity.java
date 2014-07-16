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
import android.widget.Toast;

import com.haoutil.xposed.xled.R;
import com.haoutil.xposed.xled.util.SettingsHelper;

public class AppItemActivity extends Activity {
	private static Context mContext;
	
	private static SettingsHelper settingsHelper;
	
	private static CheckBoxPreference prefEnable;
	private static Preference prefColor;
	private static EditTextPreference prefOnms;
	private static EditTextPreference prefOffms;
	
	private static String appName;
	private static String packageName;
	
	private static boolean enable;
	private static int color;
	private static int onms;
	private static int offms;
	
	private static OnPreferenceClickListener onColorClickListener;
	
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
		
		onColorClickListener = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(AppItemActivity.this.getApplicationContext(), ColorPickerActivity.class);
				intent.putExtra("originColor", color);
				AppItemActivity.this.startActivityForResult(intent, 0);
				
				return false;
			}
		};
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra("enable", settingsHelper.getBoolean("pref_app_enable_" + packageName, false));
		intent.putExtra("color", settingsHelper.getInt("pref_app_color_" + packageName, Color.TRANSPARENT));
		AppItemActivity.this.setResult(RESULT_OK, intent);
		
		super.onBackPressed();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK) {
			try {
				int newColor = Color.parseColor(data.getStringExtra("color"));
				settingsHelper.setInt("pref_app_color_" + packageName, newColor);
				color = newColor;
			} catch (Exception e) {
				Toast.makeText(mContext, getString(R.string.tip_incorrect_colorformat), Toast.LENGTH_SHORT).show();
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
					
					return false;
				}
			});

			prefColor = getPreferenceManager().findPreference("pref_color");
			prefColor.setOnPreferenceClickListener(onColorClickListener);
			
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
						Toast.makeText(mContext, getString(R.string.tip_incorrect_onmsformat), Toast.LENGTH_SHORT).show();
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
						Toast.makeText(mContext, getString(R.string.tip_incorrect_offmsformat), Toast.LENGTH_SHORT).show();
					}
					return false;
				}
			});
		}
	}
}
