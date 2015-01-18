package com.haoutil.xposed.xled.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ChargingLedHook extends BaseHook {
    private int[] defaultChargingLed = null;

    public ChargingLedHook(SettingsHelper settingsHelper) {
        super(settingsHelper);
    }

    @Override
    public void hook() {
        XposedHelpers.findAndHookMethod("com.android.server.BatteryService", null, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Object mLed = XposedHelpers.getObjectField(param.thisObject, "mLed");
                ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Logger.log(Constant.TAG, "force update charging led.");
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
                    XposedHelpers.callMethod(mBatteryLight, "turnOff");
                    param.setResult(null);
                    Logger.log(Constant.TAG, "sleep mode is on, disable charging led.");
                    return;
                }

                if (!settingsHelper.getBoolean("pref_charging_enable", false)) {
                    return;
                }

                if (defaultChargingLed == null) {
                    defaultChargingLed = new int[5];
                    defaultChargingLed[0] = XposedHelpers.getIntField(param.thisObject, "mBatteryLowARGB");
                    defaultChargingLed[1] = XposedHelpers.getIntField(param.thisObject, "mBatteryMediumARGB");
                    defaultChargingLed[2] = XposedHelpers.getIntField(param.thisObject, "mBatteryFullARGB");
                    defaultChargingLed[3] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOn");
                    defaultChargingLed[4] = XposedHelpers.getIntField(param.thisObject, "mBatteryLedOff");
                    Logger.log(Constant.TAG, "got default charging led {low:" + defaultChargingLed[0] + ",medium:" + defaultChargingLed[1] + ",full:" + defaultChargingLed[2] + ",onms:" + defaultChargingLed[3] + ",offms:" + defaultChargingLed[4] + "}");
                }

                Logger.log(Constant.TAG, "handle charging led...");
                Object batteryService = XposedHelpers.getSurroundingThis(param.thisObject);
                int level, status;
                try {    // android 4.3 and below
                    level = XposedHelpers.getIntField(batteryService, "mBatteryLevel");
                    status = XposedHelpers.getIntField(batteryService, "mBatteryStatus");
                } catch (Throwable t1) {
                    try {    // android 4.4 and above
                        Object batteryProps = XposedHelpers.getObjectField(batteryService, "mBatteryProps");
                        level = XposedHelpers.getIntField(batteryProps, "batteryLevel");
                        status = XposedHelpers.getIntField(batteryProps, "batteryStatus");
                    } catch (Throwable t2) {
                        Logger.log(Constant.TAG, "can not get battery status, break.");
                        return;
                    }
                }

                if (Constant.isSamsung) {
                    return;
                }
                if (level < settingsHelper.getInt("pref_charging_percent_low", XposedHelpers.getIntField(batteryService, "mLowBatteryWarningLevel"))) {
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        if (settingsHelper.getBoolean("pref_charging_low_disable", false)) {
                            XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            Logger.log(Constant.TAG, "low battery led disabled(charging), turnoff.");
                        } else {
                            int lowColor = settingsHelper.getInt("pref_charging_low_color", defaultChargingLed[0]);
                            if (Constant.isSamsung) {
                                XposedHelpers.callMethod(mBatteryLight, "setLightLocked", lowColor, 13, 0, 0, 0);
                            } else {
                                XposedHelpers.callMethod(mBatteryLight, "setColor", lowColor);
                            }
                            Logger.log(Constant.TAG, "set low battery led(charging)..." + lowColor);
                        }
                    } else {
                        if (settingsHelper.getBoolean("pref_low_disable", false)) {
                            XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            Logger.log(Constant.TAG, "low battery led disabled(not charging), turnoff.");
                        } else {
                            int lowColor = settingsHelper.getInt("pref_charging_low_color", defaultChargingLed[0]);
                            int onms = settingsHelper.getInt("pref_charging_onms", defaultChargingLed[3]);
                            int offms = settingsHelper.getInt("pref_charging_offms", defaultChargingLed[4]);
                            if (Constant.isSamsung) {
                                XposedHelpers.callMethod(mBatteryLight, "setLightLocked", lowColor, 11, onms, offms, 0);
                            } else {
                                XposedHelpers.callMethod(mBatteryLight, "setFlashing", lowColor, 0, onms, offms);
                            }
                            Logger.log(Constant.TAG, "set low battery led(not charging)..." + lowColor + "," + onms + "," + offms);
                        }
                    }
                } else if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                    if (status == BatteryManager.BATTERY_STATUS_FULL || level >= settingsHelper.getInt("pref_charging_percent_full", 90)) {
                        if (settingsHelper.getBoolean("pref_charging_full_disable", false)) {
                            XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            Logger.log(Constant.TAG, "full battery led disabled, turnoff.");
                        } else {
                            int fullColor = settingsHelper.getInt("pref_charging_full_color", defaultChargingLed[2]);
                            if (Constant.isSamsung) {
                                XposedHelpers.callMethod(mBatteryLight, "setLightLocked", fullColor, 14, 0, 0, 0);
                            } else {
                                XposedHelpers.callMethod(mBatteryLight, "setColor", fullColor);
                            }
                            Logger.log(Constant.TAG, "set full battery led..." + fullColor);
                        }
                    } else {
                        if (settingsHelper.getBoolean("pref_charging_medium_disable", false)) {
                            XposedHelpers.callMethod(mBatteryLight, "turnOff");
                            Logger.log(Constant.TAG, "medium battery led disabled, turnoff.");
                        } else {
                            int mediumColor = settingsHelper.getInt("pref_charging_medium_color", defaultChargingLed[1]);
                            if (Constant.isSamsung) {
                                XposedHelpers.callMethod(mBatteryLight, "setLightLocked", mediumColor, 10, 0, 0, 0);
                            } else {
                                XposedHelpers.callMethod(mBatteryLight, "setColor", mediumColor);
                            }
                            Logger.log(Constant.TAG, "set medium battery led..." + mediumColor);
                        }
                    }
                } else {
                    XposedHelpers.callMethod(mBatteryLight, "turnOff");
                    Logger.log(Constant.TAG, "turnoff charging led.");
                }

                param.setResult(null);
            }
        });

        if (Constant.isSamsung) {
            XposedHelpers.findAndHookMethod("android.util.Slog", null, "d", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0].equals("LightsService")) {
                        Logger.log(Constant.TAG, (String) param.args[1]);
                    }
                }
            });
        }
    }
}
