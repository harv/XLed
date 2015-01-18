package com.haoutil.xposed.xled.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ScreenOnLedHook extends BaseHook {
    public ScreenOnLedHook(SettingsHelper settingsHelper) {
        super(settingsHelper);
    }

    @Override
    public void hook() {
        XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                final Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                final BroadcastReceiver mIntentReceiver = (BroadcastReceiver) XposedHelpers.getObjectField(param.thisObject, "mIntentReceiver");
                if (settingsHelper.getBoolean("pref_screenon_enable", false)) {
                    Logger.log(Constant.TAG, "enable screen on LED.");
                    XposedHelpers.setBooleanField(param.thisObject, "mScreenOn", false);

                    mContext.unregisterReceiver(mIntentReceiver);

                    IntentFilter filter = new IntentFilter();
                    filter.addAction("android.intent.action.PHONE_STATE");
                    filter.addAction("android.intent.action.USER_STOPPED");

                    mContext.registerReceiver(mIntentReceiver, filter);
                }

                final Object obj = param.thisObject;
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Logger.log(Constant.TAG, "force update screen on led.");
                        mContext.unregisterReceiver(mIntentReceiver);

                        IntentFilter filter = new IntentFilter();
                        filter.addAction("android.intent.action.PHONE_STATE");
                        filter.addAction("android.intent.action.USER_STOPPED");
                        if (!intent.getBooleanExtra("enable", false)) {
                            XposedHelpers.setBooleanField(obj, "mScreenOn", true);

                            filter.addAction("android.intent.action.SCREEN_ON");
                            filter.addAction("android.intent.action.SCREEN_OFF");
                            filter.addAction("android.intent.action.USER_PRESENT");
                        } else {
                            XposedHelpers.setBooleanField(obj, "mScreenOn", false);
                        }

                        mContext.registerReceiver(mIntentReceiver, filter);

                        XposedHelpers.callMethod(obj, "updateLightsLocked");
                    }
                }, new IntentFilter("com.haoutil.xposed.xled.UPDATE_SCREENON_LED"));
            }
        });
    }
}
