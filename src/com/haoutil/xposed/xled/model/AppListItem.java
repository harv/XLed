package com.haoutil.xposed.xled.model;

import android.graphics.drawable.Drawable;

public class AppListItem {
	private String sortLetter;

	private boolean enable;
	private Drawable icon;
	private String name;
	private String pinyin;
	private String packageName;
	private int color;

	public AppListItem(String sortLetter, boolean enable, Drawable icon,
			String name, String pinyin, String packageName, int color) {
		this.sortLetter = sortLetter;
		this.enable = enable;
		this.icon = icon;
		this.name = name;
		this.pinyin = pinyin;
		this.packageName = packageName;
		this.color = color;
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
