package com.haoutil.xposed.xled.hook;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;

import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class NotificationLedHook extends BaseHook {
    private int[] defaultNotificationLed = null;

    public NotificationLedHook(SettingsHelper settingsHelper) {
        super(settingsHelper);
    }

    @Override
    public void hook() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                Notification notification = (Notification) param.args[2];
                /* since CyclingNotificationLedHook can disable led flashing, we wont deal with here

                if (isSleeping()) {
                    Logger.log(Constant.TAG, "sleep mode is on, disable all led notifications.");
                    if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
                        notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
                    } else if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) == Notification.FLAG_SHOW_LIGHTS) {
                        notification.flags &= ~(~notification.flags | Notification.FLAG_SHOW_LIGHTS);
                    }
                } else {*/
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    String packageName = mContext.getPackageName();
                    if (!settingsHelper.getBoolean("pref_app_enable_" + packageName, false)) {
                        return;
                    }

                    Logger.log(Constant.TAG, "handle package " + packageName);

                    if (settingsHelper.getBoolean("pref_app_disableled_" + packageName, false)) {
                        Logger.log(Constant.TAG, "disable led flashing.");
                        notification.flags &= ~(~notification.flags | Notification.FLAG_SHOW_LIGHTS);
                    } else {
                        if ((notification.defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
                            Logger.log(Constant.TAG, "ignore default led settings(Notification.DEFAULT_LIGHTS).");
                            notification.defaults &= ~(~notification.defaults | Notification.DEFAULT_LIGHTS);
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;

                            if (defaultNotificationLed == null) {
                                defaultNotificationLed = new int[3];

                                Resources resources = mContext.getResources();
                                defaultNotificationLed[0] = resources.getColor(resources.getIdentifier("config_defaultNotificationColor", "color", "android"));
                                defaultNotificationLed[1] = resources.getInteger(resources.getIdentifier("config_defaultNotificationLedOn", "integer", "android"));
                                defaultNotificationLed[2] = resources.getInteger(resources.getIdentifier("config_defaultNotificationLedOff", "integer", "android"));
                                Logger.log(Constant.TAG, "got default notification led {color:" + defaultNotificationLed[0] + ",onms:" + defaultNotificationLed[1] + ",offms:" + defaultNotificationLed[2] + "}");
                            }

                            notification.ledARGB = defaultNotificationLed[0];
                            notification.ledOnMS = defaultNotificationLed[1];
                            notification.ledOffMS = defaultNotificationLed[2];
                            Logger.log(Constant.TAG, "default led settings... {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                        }

                        if ((notification.flags & Notification.FLAG_SHOW_LIGHTS) != Notification.FLAG_SHOW_LIGHTS
                                && settingsHelper.getBoolean("pref_app_forceenable_" + packageName, true)) {
                            Logger.log(Constant.TAG, "force led flashing.");
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        }

                        Logger.log(Constant.TAG, "changing led settings... {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                        int color = settingsHelper.getInt("pref_app_color_" + packageName, Constant.INVALID_COLOR);
                        if (color != Constant.INVALID_COLOR) {
                            notification.ledARGB = color;
                        }
                        int onms = settingsHelper.getInt("pref_app_onms_" + packageName, Constant.INVALID_ONMS);
                        if (onms != Constant.INVALID_ONMS) {
                            notification.ledOnMS = onms;
                        }
                        int offms = settingsHelper.getInt("pref_app_offms_" + packageName, Constant.INVALID_OFFMS);
                        if (offms != Constant.INVALID_OFFMS) {
                            notification.ledOffMS = offms;
                        }
                        Logger.log(Constant.TAG, "change led settings succeed {color:" + notification.ledARGB + ",onms:" + notification.ledOnMS + ",offms:" + notification.ledOffMS + "}");
                    }
                /*}*/

                param.args[2] = notification;
            }
        };
        XposedHelpers.findAndHookMethod(NotificationManager.class, "notify", String.class, Integer.TYPE, Notification.class, hook);
        try {   // android 4.2 and above
            XposedHelpers.findAndHookMethod(NotificationManager.class, "notifyAsUser", String.class, Integer.TYPE, Notification.class, "android.os.UserHandle", hook);
        } catch (Throwable t) {
            Logger.log(Constant.TAG, "can not find NotificationManager.notifyAsUser().");
        }
    }
}
