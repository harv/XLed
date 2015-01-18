package com.haoutil.xposed.xled.hook;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class CyclingNotificationLedHook extends BaseHook {
    private Object notificationManagerService;

    private Handler mHandler;

    private boolean isWaitingNext = false;

    private int mIndex = 0;

    public CyclingNotificationLedHook(SettingsHelper settingsHelper) {
        super(settingsHelper);
    }

    @Override
    public void hook() {
        Class<?> clazz = XposedHelpers.findClass("com.android.server.NotificationManagerService", null);

        XposedHelpers.findAndHookMethod(clazz, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                notificationManagerService = param.thisObject;
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        isWaitingNext = false;
                        XposedHelpers.callMethod(notificationManagerService, "updateLightsLocked");
                    }
                };

                ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Logger.log(Constant.TAG, "force update notification led.");
                        XposedHelpers.callMethod(param.thisObject, "updateLightsLocked");
                    }
                }, new IntentFilter("com.haoutil.xposed.xled.UPDATE_NOTIFICATION_LED"));
            }
        });

        XposedHelpers.findAndHookMethod(clazz, "updateLightsLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                settingsHelper.reload();

                if (!settingsHelper.getBoolean("pref_enable", true)) {
                    return;
                }

                Object mNotificationLight = XposedHelpers.getObjectField(param.thisObject, "mNotificationLight");
                if (isSleeping()) {
                    Logger.log(Constant.TAG, "sleep mode is on, disable notification led.");
                    XposedHelpers.callMethod(mNotificationLight, "turnOff");
                    param.setResult(null);
                    return;
                }

                if (!settingsHelper.getBoolean("pref_cyclingled_enable", false)) {
                    return;
                }

                if (isWaitingNext) {
                    Logger.log(Constant.TAG, "another task is on schedule, break.");
                    return;
                }

                Logger.log(Constant.TAG, "cycling notifications..." + mIndex);

                ArrayList<Object> mLights = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mLights");

                if (mLights.size() > 0) {
                    if (mIndex >= mLights.size()) {
                        mIndex = 0;
                    }

                    Object mLedNotification = mLights.get(mIndex);

                    if (mLedNotification == null || (boolean) XposedHelpers.getObjectField(param.thisObject, "mInCall") || (boolean) XposedHelpers.getObjectField(param.thisObject, "mScreenOn")) {
                        XposedHelpers.callMethod(mNotificationLight, "turnOff");
                    } else {
                        Notification notification;
                        try {   // 4.4.4
                            notification = (Notification) XposedHelpers.callMethod(XposedHelpers.getObjectField(mLedNotification, "sbn"), "getNotification");
                        } catch (Throwable t) {
                            notification = (Notification) XposedHelpers.getObjectField(mLedNotification, "notification");
                        }
                        int ledARGB = notification.ledARGB;
                        int ledOnMS = notification.ledOnMS;
                        int ledOffMS = notification.ledOffMS;
                        if ((notification.defaults & Notification.DEFAULT_LIGHTS) != 0) {
                            ledARGB = XposedHelpers.getIntField(param.thisObject, "mDefaultNotificationColor");
                            ledOnMS = XposedHelpers.getIntField(param.thisObject, "mDefaultNotificationLedOn");
                            ledOffMS = XposedHelpers.getIntField(param.thisObject, "mDefaultNotificationLedOff");
                        }

                        if ((boolean) XposedHelpers.getObjectField(param.thisObject, "mNotificationPulseEnabled")) {
                            Logger.log(Constant.TAG, "{color:" + ledARGB + ",ledOnMS:" + ledOnMS + ",ledOffMS:" + ledOffMS + "}");
                            XposedHelpers.callMethod(mNotificationLight, "setFlashing", ledARGB, 1, ledOnMS, ledOffMS);
                            if (mLights.size() > 1) {
                                isWaitingNext = true;
                                mHandler.sendEmptyMessageDelayed(0, ledOnMS + ledOffMS);
                            }
                        }
                    }
                }

                mIndex++;

                param.setResult(null);
            }
        });
    }
}
