package com.haoutil.xposed.xled.util;

import android.graphics.Color;
import android.os.Build;

public class Constant {
    public final static int DEFAULT_COLOR = Color.TRANSPARENT;
    public final static int INVALID_COLOR = 16777216;
    public final static int INVALID_ONMS = -1;
    public final static int INVALID_OFFMS = -1;

    public final static String TAG = "XLED";

    public final static boolean isSamsung = Build.FINGERPRINT.toLowerCase().startsWith("samsung");
}
