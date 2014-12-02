package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.haoutil.xposed.xled.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends Activity {
    private static File prefsFile = new File(Environment.getDataDirectory(), "data/com.haoutil.xposed.xled/shared_prefs/com.haoutil.xposed.xled_preferences.xml");
    private static File backupPrefsFile = new File(Environment.getExternalStorageDirectory(), "XLED_Backup.xml");

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
                            new ComponentName(SettingsActivity.this, "com.haoutil.xposed.xled.activity.ShowIcon"),
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
        } catch (NameNotFoundException e) {
        }

        onPreferenceClickListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("pref_app_config")) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, AppListActivity.class));
                } else if (preference.getKey().equals("pref_charging_led")) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ChargingActivity.class));
                } else if (preference.getKey().equals("pref_backup")) {
                    new ExportTask().execute(backupPrefsFile);
                } else if (preference.getKey().equals("pref_restore")) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(getResources().getString(R.string.restore_title))
                            .setMessage(getResources().getString(R.string.restore_confirm))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    new ImportTask().execute(backupPrefsFile);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
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
            getPreferenceManager().findPreference("pref_backup").setOnPreferenceClickListener(onPreferenceClickListener);
            getPreferenceManager().findPreference("pref_restore").setOnPreferenceClickListener(onPreferenceClickListener);
            getPreferenceManager().findPreference("pref_app_info").setSummary(getString(R.string.app_info_sum).replace("#version#", versionName));
        }
    }

    private class ExportTask extends AsyncTask<File, String, String> {
        @Override
        protected String doInBackground(File... params) {
            File outFile = params[0];
            try {
                copyFile(prefsFile, outFile);
                return getString(R.string.backup_success) + outFile.getAbsolutePath();
            } catch (IOException ex) {
                return getString(R.string.backup_failed) + ex.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(SettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class ImportTask extends AsyncTask<File, String, String> {
        @Override
        protected String doInBackground(File... params) {
            File inFile = params[0];
            String tempFilename = "com.haoutil.xposed.xled_preferences-new";
            File newPrefsFile = new File(prefsFile.getParentFile(), tempFilename + ".xml");
            // Make sure the shared_prefs folder exists, with the proper permissions
            getSharedPreferences(tempFilename, Context.MODE_WORLD_READABLE).edit().commit();
            try {
                copyFile(inFile, newPrefsFile);
            } catch (IOException ex) {
                return getString(R.string.restore_failed) + ex.getMessage();
            }

            newPrefsFile.setReadable(true, false);
            SharedPreferences newPrefs = getSharedPreferences(tempFilename, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
            if (newPrefs.getAll().size() == 0) {
                // No entries in imported file, discard it
                newPrefsFile.delete();
                return getString(R.string.restore_invalid) + inFile.getAbsoluteFile();
            } else {
                if (!newPrefsFile.renameTo(prefsFile)) {
                    prefsFile.delete();
                    newPrefsFile.renameTo(prefsFile);
                }
                return getString(R.string.restore_success);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(SettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        boolean success = false;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[10 * 1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            out = null;
            success = true;
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                }
            }
            if (!success) {
                dest.delete();
            }
        }
    }
}
