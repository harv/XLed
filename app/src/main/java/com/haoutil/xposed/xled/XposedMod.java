package com.haoutil.xposed.xled;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.BatteryManager;

import com.haoutil.xposed.xled.model.TimePreference;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import java.util.Calendar;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class XposedMod implements IXposedHookZygoteInit {
    public final static int DEFAULT_COLOR = Color.TRANSPARENT;
    public final static int INVALID_COLOR = 16777216;
    public final static int INVALID_ONMS = -1;
    public final static int INVALID_OFFMS = -1;

    private final static String TAG = "XLED";

    private static SettingsHelper settingsHelper;

    // Notification.DEFAULT_LIGHTS
    private int[] defaultNotificationLed = null;

    private int[] defaultChargingLed = null;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        settingsHelper = new SettingsHelper();

        // notification led
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                Notification notification = (Notification) param.args[2];
                if (isSleeping()) {
                    Logger.log(TAG, "sleep mode is on, disable all led notifications.");
                    if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
                        notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
                    } else if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) == Notification.FLAG_SHOW_LIGHTS) {
                        notification.flags &= ~(~notification.flags | Notification.FLAG_SHOW_LIGHTS);
                    }
                } else {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    String packageName = mContext.getPackageName();
                    if (!settingsHelper.getBoolean("pref_app_enable_" + packageName, false)) {
                        return;
                    }

                    Logger.log(TAG, "handle package " + packageName);

                    if (settingsHelper.getBoolean("pref_app_disableled_" + packageName, false)) {
                        Logger.log(TAG, "disable led flashing.");
                        notification.flags &= ~(~notification.flags | Notification.FLAG_SHOW_LIGHTS);
                    } else {
                        if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
                            Logger.log(TAG, "ignore default led settings(Notification.DEFAULT_LIGHTS).");
                            notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;

                            if (defaultNotificationLed == null) {
                                defaultNotificationLed = new int[3];

                                Resources resources = mContext.getResources();
                                defaultNotificationLed[0] = resources.getColor(resources.getIdentifier("config_defaultNotificationColor", "color", "android"));
                                defaultNotificationLed[1] = resources.getInteger(resources.getIdentifier("config_defaultNotificationLedOn", "integer", "android"));
                                defaultNotificationLed[2] = resources.getInteger(resources.getIdentifier("config_defaultNotificationLedOff", "integer", "android"));
                                Logger.log(TAG, "got default notification led {color:" + defaultNotificationLed[0] + ",onms:" + defaultNotificationLed[1] + ",offms:" + defaultNotificationLed[2] + "}");
                            }

                            notification.ledARGB = defaultNotificationLed[0];
                            notification.ledOnMS = defaultNotificationLed[1];
                            notification.ledOffMS = defaultNotificationLed[2];
                            Logger.log(TAG, "default led settings... {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                        }

                        if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) != Notification.FLAG_SHOW_LIGHTS
                                && settingsHelper.getBoolean("pref_app_forceenable_" + packageName, true)) {
                            Logger.log(TAG, "force led flashing.");
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        }

                        Logger.log(TAG, "changing led settings... {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                        int color = settingsHelper.getInt("pref_app_color_" + packageName, INVALID_COLOR);
                        if (color != INVALID_COLOR) {
                            notification.ledARGB = color;
                        }
                        int onms = settingsHelper.getInt("pref_app_onms_" + packageName, INVALID_ONMS);
                        if (onms != INVALID_ONMS) {
                            notification.ledOnMS = onms;
                        }
                        int offms = settingsHelper.getInt("pref_app_offms_" + packageName, INVALID_OFFMS);
                        if (offms != INVALID_OFFMS) {
                            notification.ledOffMS = offms;
                        }
                        Logger.log(TAG, "change led settings succeed {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                    }
                }

                param.args[2] = notification;
            }
        };
        XposedHelpers.findAndHookMethod(NotificationManager.class, "notify", String.class, Integer.TYPE, Notification.class, hook);
        try {   // android 4.2 and above
            XposedHelpers.findAndHookMethod(NotificationManager.class, "notifyAsUser", String.class, Integer.TYPE, Notification.class, "android.os.UserHandle", hook);
        } catch (Throwable t) {
            Logger.log(TAG, "can not find NotificationManager.notifyAsUser().");
        }

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

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                Object mBatteryLight = XposedHelpers.getObjectField(param.thisObject, "mBatteryLight");

                if (isSleeping()) {
                    Logger.log(TAG, "sleep mode is on, disable charging led.");
                    XposedHelpers.callMethod(mBatteryLight, "turnOff");
                    param.setResult(null);
                    return;
                }

                if (!settingsHelper.getBoolean("pref_charging_enable", false)) {
                    return;
                }

                Logger.log(TAG, "update charging led.");

                if (defaultChargingLed == null) {
                    defaultChargingLed = new int[5];
                    defaultChargingLed[0] = XposedHelpers.getIntField(param.thisObject, "mBatteryLowARGB");
                    defaultChargingLed[1] = XposedHelpers.getIntField(param.thisObject, "mBatteryMediumARGB");
                    defaultChargingLed[2] = XposedHelpers.getIntField(param.thisObject, "mBatteryFullARGB");
                    defaultChargingLed[3] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOn");
                    defaultChargingLed[4] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOff");
                    Logger.log(TAG, "got default charging led {low:" + defaultChargingLed[0] + ",medium:" + defaultChargingLed[1] + ",full:" + defaultChargingLed[2] + ",onms:" + defaultChargingLed[3] + ",offms:" + defaultChargingLed[4] + "}");
                }

                int level, status;
                Object batteryService = XposedHelpers.getSurroundingThis(param.thisObject);
                try {    // android 4.3 and below
                    level = XposedHelpers.getIntField(batteryService, "mBatteryLevel");
                    status = XposedHelpers.getIntField(batteryService, "mBatteryStatus");
                } catch (Throwable t) {
                    try {    // android 4.4 and above
                        Object batteryProps = XposedHelpers.getObjectField(batteryService, "mBatteryProps");
                        level = XposedHelpers.getIntField(batteryProps, "batteryLevel");
                        status = XposedHelpers.getIntField(batteryProps, "batteryStatus");
                    } catch (Throwable t1) {
                        Logger.log(TAG, "can not get battery status, break.");
                        return;
                    }
                }

                // for SamSung
                boolean mScreenOn, mLedChargingSettingsEnable, mDormantOn;
                try {
                    mScreenOn = XposedHelpers.getBooleanField(batteryService, "mScreenOn");
                    mLedChargingSettingsEnable = XposedHelpers.getBooleanField(batteryService, "mLedChargingSettingsEnable");
                    mDormantOn = false;
                    if (XposedHelpers.getBooleanField(batteryService, "mDormantOnOff")) {
                        if (XposedHelpers.getBooleanField(batteryService, "mDormantDisableLED")) {
                            if (XposedHelpers.getBooleanField(batteryService, "mDormantAlways")) {
                                Logger.log(TAG, "(SamSung)Dormant mode is always on, break.");
                                mDormantOn = true;
                            } else {
                                Calendar calendar = Calendar.getInstance();
                                int m = 60 * calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE);
                                int mDormantStartMinutes = XposedHelpers.getIntField(batteryService, "mDormantStartMinutes");
                                int mDormantEndMinutes = XposedHelpers.getIntField(batteryService, "mDormantEndMinutes");
                                if ((mDormantStartMinutes <= mDormantEndMinutes && mDormantStartMinutes <= m && m < mDormantEndMinutes)
                                        || (mDormantStartMinutes > mDormantEndMinutes && (mDormantStartMinutes <= m || m < mDormantEndMinutes))) {
                                    Logger.log(TAG, "(SamSung)Dormant mode is on, break.");
                                    mDormantOn = true;
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    mScreenOn = false;
                    mLedChargingSettingsEnable = true;
                    mDormantOn = false;
                }

                if (!mScreenOn && mLedChargingSettingsEnable && !mDormantOn) {
                    if (level < XposedHelpers.getIntField(batteryService, "mLowBatteryWarningLevel")) {
                        if (settingsHelper.getBoolean("pref_low_disable", false) || settingsHelper.getBoolean("pref_charging_low_disable", false)) {
                            Logger.log(TAG, "low battery led disabled, turnoff.");
                            XposedHelpers.callMethod(mBatteryLight, "turnOff");
                        } else {
                            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                                int lowColor = settingsHelper.getInt("pref_charging_low_color", defaultChargingLed[0]);
                                XposedHelpers.callMethod(mBatteryLight, "setColor", lowColor);
                                Logger.log(TAG, "setting low battery led(charging)..." + lowColor);
                            } else {
                                int lowColor = settingsHelper.getInt("pref_charging_low_color", defaultChargingLed[0]);
                                int onms = settingsHelper.getInt("pref_charging_onms", defaultChargingLed[3]);
                                int offms = settingsHelper.getInt("pref_charging_offms", defaultChargingLed[4]);
                                XposedHelpers.callMethod(
                                        mBatteryLight,
                                        "setFlashing",
                                        lowColor,
                                        XposedHelpers.getStaticIntField(XposedHelpers.findClass("com.android.server.LightsService", null), "LIGHT_FLASH_TIMED"),
                                        onms,
                                        offms
                                );
                                Logger.log(TAG, "setting low battery led(not charging)..." + lowColor + "," + onms + "," + offms);
                            }
                        }
                    } else if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                        if (status == BatteryManager.BATTERY_STATUS_FULL || level >= 90) {
                            if (settingsHelper.getBoolean("pref_charging_full_disable", false)) {
                                Logger.log(TAG, "full battery led disabled, turnoff.");
                                XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            } else {
                                int fullColor = settingsHelper.getInt("pref_charging_full_color", defaultChargingLed[2]);
                                XposedHelpers.callMethod(mBatteryLight, "setColor", fullColor);
                                Logger.log(TAG, "setting full battery led..." + fullColor);
                            }
                        } else {
                            if (settingsHelper.getBoolean("pref_charging_medium_disable", false)) {
                                Logger.log(TAG, "medium battery led disabled, turnoff.");
                                XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            } else {
                                int mediumColor = settingsHelper.getInt("pref_charging_medium_color", defaultChargingLed[1]);
                                XposedHelpers.callMethod(mBatteryLight, "setColor", mediumColor);
                                Logger.log(TAG, "setting medium battery led..." + mediumColor);
                            }
                        }
                    } else {
                        Logger.log(TAG, "turnoff charging led.");
                        XposedHelpers.callMethod(mBatteryLight, "turnOff");
                    }
                } else {
                    Logger.log(TAG, "(SamSung)charging led disabled, turnoff.");
                    XposedHelpers.callMethod(mBatteryLight, "turnOff");
                }
                param.setResult(null);
            }
        });
    }

    private boolean isSleeping() {
        settingsHelper.reload();

        if (!settingsHelper.getBoolean("pref_sleep", false)) {
            Logger.log(TAG, "sleep mode is not on.");
            return false;
        }

        String sleepStartTime = settingsHelper.getString("pref_sleep_start", null);
        String sleepEndTime = settingsHelper.getString("pref_sleep_end", null);
        if (sleepStartTime == null || sleepEndTime == null) {
            Logger.log(TAG, "begin or end time of sleep mode is not set.");
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        int m = 60 * calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE);
        int sleepStartMinutes = 60 * TimePreference.getHour(sleepStartTime) + TimePreference.getMinuter(sleepStartTime);
        int sleepEndMinutes = 60 * TimePreference.getHour(sleepEndTime) + TimePreference.getMinuter(sleepEndTime);

        return (sleepStartMinutes <= sleepEndMinutes && sleepStartMinutes <= m && m < sleepEndMinutes)
                || (sleepStartMinutes > sleepEndMinutes && (sleepStartMinutes <= m || m < sleepEndMinutes));
    }
}
