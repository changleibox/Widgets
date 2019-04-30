/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget.picker;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by box on 2019-04-30.
 */
public class CNTimePicker extends TimePicker {

    public CNTimePicker(Context context) {
        super(context);
    }

    public CNTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CNTimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CNTimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    TimePickerDelegate createSpinnerUIDelegate(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        return new CNTimePickerSpinnerDelegate(this, context, attrs, defStyleAttr, defStyleRes);
    }
}
