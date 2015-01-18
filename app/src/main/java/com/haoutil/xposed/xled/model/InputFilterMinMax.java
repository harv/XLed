package com.haoutil.xposed.xled.model;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterMinMax implements InputFilter {

    private int min, max;

    public InputFilterMinMax(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public InputFilterMinMax(String min, String max) {
        this.min = Integer.parseInt(min);
        this.max = Integer.parseInt(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            String newString = dest.toString().substring(0, dstart) + source.toString().substring(start, end) + dest.toString().substring(dend);

            Double input;
            // If we only have one char and it is a minus sign, test against -1:
            if (newString.length() == 1 && newString.charAt(0) == '-')
                input = -1d;
            else
                input = Double.parseDouble(newString);

            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException nfe) {
        }
        return "";
    }

    private boolean isInRange(int a, int b, Double c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}
