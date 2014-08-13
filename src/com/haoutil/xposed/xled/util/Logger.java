package com.haoutil.xposed.xled.util;

import android.util.Log;
import de.robv.android.xposed.XposedBridge;

public class Logger {
	private static SettingsHelper settingsHelper = new SettingsHelper();;
	
	public static void log(String tag, String msg) {
		if (!settingsHelper.getBoolean("pref_debug", false)) return;
		
		try {
			XposedBridge.log("[" + tag + "]" + msg);
		} catch (Exception e) {
			Log.i(tag, msg);
		}
	}
}
