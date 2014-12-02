package com.haoutil.xposed.xled.model;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimePreference extends DialogPreference {
    private int lastHour = 0;
    private int lastMinuter = 0;
    private CharSequence mSummary;
    private TimePicker picker = null;
    private String timeVal;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        initSummary(attrs);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initSummary(attrs);
    }

    private void initSummary(AttributeSet attrs) {
        setSummary(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "summary", 0));
    }

    public static int getHour(String timeVal) {
        String[] pieces = timeVal.split(":");
        return (Integer.parseInt(pieces[0]));
    }

    public static int getMinuter(String timeVal) {
        String[] pieces = timeVal.split(":");
        return (Integer.parseInt(pieces[1]));
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(true);

        return (picker);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        picker.setCurrentHour(lastHour);
        picker.setCurrentMinute(lastMinuter);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            lastHour = picker.getCurrentHour();
            lastMinuter = picker.getCurrentMinute();

            timeVal = lastHour + ":" + lastMinuter;
            if (callChangeListener(timeVal)) {
                persistString(timeVal);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        timeVal = null;

        if (restoreValue) {
            if (defaultValue == null) {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("HH:mm");
                String formatted = format1.format(cal.getTime());
                timeVal = getPersistedString(formatted);
            } else {
                timeVal = getPersistedString(defaultValue.toString());
            }
        } else {
            timeVal = defaultValue.toString();
        }

        lastHour = getHour(timeVal);
        lastMinuter = getMinuter(timeVal);
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        timeVal = text;
        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public String getText() {
        return timeVal;
    }

    public void setSummary(CharSequence summary) {
        if (summary == null && mSummary != null || summary != null && !summary.equals(mSummary)) {
            mSummary = summary;
            notifyChanged();
        }
    }

    public CharSequence getSummary() {
        return mSummary;
    }
}
