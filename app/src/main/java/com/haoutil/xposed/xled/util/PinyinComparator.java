package com.haoutil.xposed.xled.util;

import com.haoutil.xposed.xled.model.AppListItem;

import java.util.Comparator;

public class PinyinComparator implements Comparator<AppListItem> {
	public int compare(AppListItem app1, AppListItem app2) {
		int i = 0;
		
		if (app1.getSortLetter().equals("☆") && !app2.getSortLetter().equals("☆")
				|| !app1.getSortLetter().equals("#") && app2.getSortLetter().equals("#")) {
			i = -1;
		} else if (!app1.getSortLetter().equals("☆") && app2.getSortLetter().equals("☆")
				|| app1.getSortLetter().equals("#") && !app2.getSortLetter().equals("#")) {
			i = 1;
		} else {
			if (app1.getSortLetter().equals("△") && !app2.getSortLetter().equals("△")) {
				i = -1;
			} else if (app2.getSortLetter().equals("△") && !app1.getSortLetter().equals("△")) {
				i = 1;
			} else {
				i = app1.getSortLetter().compareTo(app2.getSortLetter());
			}
		}

		if (i == 0) {
			i = app1.getPinyin().compareTo(app2.getPinyin());
		}

		return i;
	}
}
