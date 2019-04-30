/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget.picker;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Provides a widget for selecting a date.
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 * </p>
 * <p>
 * For a dialog using this view, see {@link android.app.DatePickerDialog}.
 * </p>
 *
 * @attr ref android.R.styleable#DatePicker_startYear
 * @attr ref android.R.styleable#DatePicker_endYear
 * @attr ref android.R.styleable#DatePicker_maxDate
 * @attr ref android.R.styleable#DatePicker_minDate
 * @attr ref android.R.styleable#DatePicker_spinnersShown
 * @attr ref android.R.styleable#DatePicker_calendarViewShown
 * @attr ref android.R.styleable#DatePicker_dayOfWeekBackground
 * @attr ref android.R.styleable#DatePicker_dayOfWeekTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerBackground
 * @attr ref android.R.styleable#DatePicker_headerMonthTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerDayOfMonthTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerYearTextAppearance
 * @attr ref android.R.styleable#DatePicker_yearListItemTextAppearance
 * @attr ref android.R.styleable#DatePicker_yearListSelectorColor
 * @attr ref android.R.styleable#DatePicker_calendarTextColor
 * @attr ref android.R.styleable#DatePicker_datePickerMode
 */
@SuppressWarnings("all")
public class CNDatePicker extends DatePicker {

    public CNDatePicker(Context context) {
        super(context);
    }

    public CNDatePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CNDatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CNDatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected DatePickerDelegate createSpinnerUIDelegate(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        return new CNDatePickerSpinnerDelegate(this, context, attrs, defStyleAttr, defStyleRes);
    }
}
