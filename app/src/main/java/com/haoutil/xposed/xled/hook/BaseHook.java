package com.haoutil.xposed.xled.hook;

import com.haoutil.xposed.xled.model.TimePreference;
import com.haoutil.xposed.xled.util.Constant;
import com.haoutil.xposed.xled.util.Logger;
import com.haoutil.xposed.xled.util.SettingsHelper;

import java.util.Calendar;

public abstract class BaseHook {
    public SettingsHelper settingsHelper;

    public BaseHook(SettingsHelper settingsHelper) {
        this.settingsHelper = settingsHelper;
    }

    public abstract void hook();

    public boolean isSleeping() {
        settingsHelper.reload();

        if (!settingsHelper.getBoolean("pref_sleep", false)) {
            return false;
        }

        String sleepStartTime = settingsHelper.getString("pref_sleep_start", null);
        String sleepEndTime = settingsHelper.getString("pref_sleep_end", null);
        if (sleepStartTime == null || sleepEndTime == null) {
            Logger.log(Constant.TAG, "begin or end time of sleep mode is not set.");
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
