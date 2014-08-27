package com.haoutil.xposed.xled;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;

import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class XposedMod implements IXposedHookZygoteInit {
    private final static String TAG = "XLED";

    private static SettingsHelper settingsHelper;

    private int[] defaultLed = null;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        settingsHelper = new SettingsHelper();

        // notification led
        XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "enqueueNotificationWithTag",
                String.class, String.class, int.class, Notification.class, int[].class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        settingsHelper.reload();

                        if (!settingsHelper.getBoolean("pref_enable", true)) {
                            return;
                        }

                        String packageName = (String) param.args[0];
                        if (packageName.equals("com.haoutil.xposed.xled") || !settingsHelper.getBoolean("pref_app_enable_" + packageName, false)) {
                            return;
                        }

                        Logger.log(TAG, "handle package " + packageName);

                        Notification notification = (Notification) param.args[3];
                        if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
                            Logger.log(TAG, "ignore default led settings(Notification.DEFAULT_LIGHTS).");
                            notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        }

                        if (settingsHelper.getBoolean("pref_app_disableled_" + packageName, false)) {
                            Logger.log(TAG, "disable led flashing.");
                            notification.flags &= ~(~notification.flags | Notification.FLAG_SHOW_LIGHTS);
                            return;
                        }

                        if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) != Notification.FLAG_SHOW_LIGHTS
                                && settingsHelper.getBoolean("pref_app_forceenable_" + packageName, true)) {
                            Logger.log(TAG, "force led flashing.");
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        }

                        Logger.log(TAG, "changing led settings... {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                        notification.ledARGB = settingsHelper.getInt("pref_app_color_" + packageName, Color.TRANSPARENT);
                        notification.ledOnMS = settingsHelper.getInt("pref_app_onms_" + packageName, settingsHelper.getInt("pref_led_onms", 300));
                        notification.ledOffMS = settingsHelper.getInt("pref_app_offms_" + packageName, settingsHelper.getInt("pref_led_offms", 1000));
                        Logger.log(TAG, "change led settings succeed {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");

                        param.args[3] = notification;
                    }
                });

        // screen on led
        XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                final Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                final BroadcastReceiver mIntentReceiver = (BroadcastReceiver) XposedHelpers.getObjectField(param.thisObject, "mIntentReceiver");
                if (settingsHelper.getBoolean("pref_screenon_enable", false)) {
                    Logger.log(TAG, "enable screen on LED.");
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
                        Logger.log(TAG, "force update screen on led.");
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

        // charging led
        XposedHelpers.findAndHookMethod("com.android.server.BatteryService", null, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Object mLed = XposedHelpers.getObjectField(param.thisObject, "mLed");
                ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Logger.log(TAG, "force update charging led.");
                        XposedHelpers.callMethod(mLed, "updateLightsLocked");
                    }
                }, new IntentFilter("com.haoutil.xposed.xled.UPDATE_CHARGING_LED"));
            }
        });

        XposedHelpers.findAndHookMethod("com.android.server.BatteryService$Led", null, "updateLightsLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true) || !settingsHelper.getBoolean("pref_charging_enable", false)) {
                    if (defaultLed != null) {
                        XposedHelpers.setIntField(param.thisObject, "mBatteryLowARGB", defaultLed[0]);
                        XposedHelpers.setIntField(param.thisObject, "mBatteryMediumARGB", defaultLed[1]);
                        XposedHelpers.setIntField(param.thisObject, "mBatteryFullARGB", defaultLed[2]);
                        XposedHelpers.setIntField(param.thisObject, "mBatteryLedOn", defaultLed[3]);
                        XposedHelpers.setIntField(param.thisObject, "mBatteryLedOff", defaultLed[4]);
                    }

                    return;
                }

                if (defaultLed == null) {
                    defaultLed = new int[5];
                    defaultLed[0] = XposedHelpers.getIntField(param.thisObject, "mBatteryLowARGB");
                    defaultLed[1] = XposedHelpers.getIntField(param.thisObject, "mBatteryMediumARGB");
                    defaultLed[2] = XposedHelpers.getIntField(param.thisObject, "mBatteryFullARGB");
                    defaultLed[3] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOn");
                    defaultLed[4] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOff");
                }

                Logger.log(TAG, "enable change charging LED");

                boolean turnOffLed = false;
                Object catteryService = XposedHelpers.getSurroundingThis(param.thisObject);

                int level = XposedHelpers.getIntField(catteryService, "mBatteryLevel");
                int status = XposedHelpers.getIntField(catteryService, "mBatteryStatus");

                if (level < XposedHelpers.getIntField(catteryService, "mLowBatteryWarningLevel")) {
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING && settingsHelper.getBoolean("pref_charging_low_disable", false)
                            || settingsHelper.getBoolean("pref_low_disable", false)) {
                        turnOffLed = true;
                    }
                } else if ((status == BatteryManager.BATTERY_STATUS_FULL || level >= 90) && settingsHelper.getBoolean("pref_charging_full_disable", false)
                        || status == BatteryManager.BATTERY_STATUS_CHARGING && settingsHelper.getBoolean("pref_charging_medium_disable", false)) {
                    turnOffLed = true;
                }

                if (turnOffLed) {
                    Logger.log(TAG, "disable charging led.");
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBatteryLight"), "turnOff");

                    param.setResult(null);
                } else {
                    Logger.log(TAG, "change charging led.");
                    XposedHelpers.setIntField(param.thisObject, "mBatteryLowARGB", settingsHelper.getInt("pref_charging_low_color", defaultLed[0]));
                    XposedHelpers.setIntField(param.thisObject, "mBatteryMediumARGB", settingsHelper.getInt("pref_charging_medium_color", defaultLed[1]));
                    XposedHelpers.setIntField(param.thisObject, "mBatteryFullARGB", settingsHelper.getInt("pref_charging_full_color", defaultLed[2]));
                    XposedHelpers.setIntField(param.thisObject, "mBatteryLedOn", settingsHelper.getInt("pref_charging_onms", defaultLed[3]));
                    XposedHelpers.setIntField(param.thisObject, "mBatteryLedOff", settingsHelper.getInt("pref_charging_offms", defaultLed[4]));
                }
            }
        });
    }
}
