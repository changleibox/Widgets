/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import me.box.widget.picker.PickerView;

/**
 * Created by box on 2019-04-30.
 */
public class MainActivity extends AppCompatActivity {

    private final Calendar mCurrentDate = Calendar.getInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PickerView datePicker = findViewById(R.id.date_picker);
        final PickerView timePicker = findViewById(R.id.time_picker);

        datePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        datePicker.setMinValue(1900);
        datePicker.setMaxValue(2200);
        datePicker.setFormatter(new PickerView.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%d年", value);
            }
        });
        timePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        timePicker.setMinValue(0);
        timePicker.setMaxValue(11);
        timePicker.setFormatter(new PickerView.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%s月", PickerView.getTwoDigitFormatter().format(value + 1));
            }
        });
        // final TextView tvTime = findViewById(R.id.tv_time);
        //
        // mCurrentDate.set(Calendar.YEAR, datePicker.getYear());
        // mCurrentDate.set(Calendar.MONTH, datePicker.getMonth());
        // mCurrentDate.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        // mCurrentDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        // mCurrentDate.set(Calendar.MINUTE, timePicker.getMinute());
        //
        // tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));
        //
        // datePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
        //     @Override
        //     public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        //         mCurrentDate.set(Calendar.YEAR, year);
        //         mCurrentDate.set(Calendar.MONTH, monthOfYear);
        //         mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        //         tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));
        //     }
        // });
        //
        // timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
        //     @Override
        //     public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        //         mCurrentDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        //         mCurrentDate.set(Calendar.MINUTE, minute);
        //         tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));
        //     }
        // });
    }
}
