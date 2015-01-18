package com.haoutil.xposed.xled.model;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class AppListItem {
    private String sortLetter;

    private ApplicationInfo appInfo;
    private boolean enable;
    private boolean disableLED;
    private Drawable icon;
    private String name;
    private String pinyin;
    private String packageName;
    private int color;

    public AppListItem(ApplicationInfo appInfo, boolean enable, boolean disableLED, String name, String pinyin, String packageName, int color) {
        this.appInfo = appInfo;
        this.enable = enable;
        this.disableLED = disableLED;
        this.name = name;
        this.pinyin = pinyin;
        this.packageName = packageName;
        this.color = color;
    }

    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(ApplicationInfo appInfo) {
        this.appInfo = appInfo;
    }

    public String getSortLetter() {
        return sortLetter;
    }

    public void setSortLetter(String sortLetter) {
        this.sortLetter = sortLetter;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isDisableLED() {
        return disableLED;
    }

    public void setDisableLED(boolean disableLED) {
        this.disableLED = disableLED;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
