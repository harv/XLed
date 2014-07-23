package com.haoutil.xposed.xled;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class XposedMod implements IXposedHookZygoteInit {
	private static SettingsHelper settingsHelper;
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		
		settingsHelper = new SettingsHelper();
		
		XC_MethodHook hook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				settingsHelper.reload();
				
				if (!settingsHelper.getBoolean("pref_enable", true)) {
					return;
				}
				
				String packageName = ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")).getPackageName();
				if (packageName.equals("com.haoutil.xposed.xled") || !settingsHelper.getBoolean("pref_app_enable_" + packageName, false)) {
					return;
				}
				
				Notification notification = (Notification) param.args[2];
				if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
					notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
					notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				}
				
				if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) != Notification.FLAG_SHOW_LIGHTS) {
					return;
				}
				
				notification.ledARGB = settingsHelper.getInt("pref_app_color_" + packageName, Color.TRANSPARENT);
				notification.ledOnMS = settingsHelper.getInt("pref_app_onms_" + packageName, settingsHelper.getInt("pref_led_onms", 300));
				notification.ledOffMS = settingsHelper.getInt("pref_app_offms_" + packageName, settingsHelper.getInt("pref_led_offms", 1000));
				
				param.args[2] = notification;
			}
		};
		
		XposedHelpers.findAndHookMethod(NotificationManager.class, "notify", new Object[] {String.class, Integer.TYPE, Notification.class, hook});
		XposedHelpers.findAndHookMethod(NotificationManager.class, "notifyAsUser", new Object[] {String.class, Integer.TYPE, Notification.class, "android.os.UserHandle", hook});
	}

}
