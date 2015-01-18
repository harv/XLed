package com.haoutil.xposed.xled;

import com.haoutil.xposed.xled.hook.ChargingLedHook;
import com.haoutil.xposed.xled.hook.CyclingNotificationLedHook;
import com.haoutil.xposed.xled.hook.NotificationLedHook;
import com.haoutil.xposed.xled.hook.ScreenOnLedHook;
import com.haoutil.xposed.xled.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;

public class XposedMod implements IXposedHookZygoteInit {
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        SettingsHelper settingsHelper = new SettingsHelper();

        new NotificationLedHook(settingsHelper).hook();
        new ScreenOnLedHook(settingsHelper).hook();
        new ChargingLedHook(settingsHelper).hook();
        new CyclingNotificationLedHook(settingsHelper).hook();
    }
}
