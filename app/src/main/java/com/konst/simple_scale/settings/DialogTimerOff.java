package com.konst.simple_scale.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import com.konst.simple_scale.Globals;
import com.konst.simple_scale.NumberPicker;
import com.konst.simple_scale.R;

import java.util.Arrays;

/**
 * @author Kostya
 */
class DialogTimerOff extends DialogPreference /*implements ActivityPreferences.InterfacePreference*/ {
    final Globals globals;
    private int mNumber;
    private final String[] timeArray;
    private NumberPicker numberPicker;
    final int minValue;
    final int maxValue;

    public DialogTimerOff(Context context, AttributeSet attrs) {
        super(context, attrs);
        timeArray = context.getResources().getStringArray(R.array.array_timer_minute);
        minValue = 0;
        maxValue = timeArray.length > 0 ? timeArray.length - 1 : 0;
        globals = Globals.getInstance();
        int time = getPersistedInt(context.getResources().getInteger(R.integer.default_min_time_off));
        //int time = globals.isScaleConnect()?globals.getScaleModule().getTimeOff():0;
        int index = Arrays.asList(timeArray).indexOf(String.valueOf(time));
        if(index != -1)
            mNumber = index;
        setPersistent(true);
        setDialogLayoutResource(R.layout.number_picker);
    }

    @Override
    protected void onBindDialogView(View view) {
        numberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
        numberPicker.setDisplayedValues(timeArray);
        numberPicker.setValue(mNumber);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // needed when user edits the text field and clicks OK
            numberPicker.clearFocus();
            setValue(numberPicker.getValue());
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int value = restoreValue? getPersistedInt(mNumber) : (Integer) defaultValue;
        setValue(Arrays.asList(timeArray).indexOf(String.valueOf(value)));
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(Integer.valueOf(timeArray[value]));
        }

        if (value != mNumber) {
            mNumber = value;
            notifyChanged();
            callChangeListener(Integer.valueOf(timeArray[value]));
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        //super.setDefaultValue(defaultValue);
        setValue(Arrays.asList(timeArray).indexOf(String.valueOf(defaultValue)));
        //setValue((int)defaultValue);
    }
}
