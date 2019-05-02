/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import me.box.widget.picker.DatePicker;
import me.box.widget.picker.PickerView;
import me.box.widget.picker.TimePicker;

/**
 * Created by box on 2019-04-30.
 */
public class MainActivity extends AppCompatActivity {

    private final Calendar mCurrentDate = Calendar.getInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
    private int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.GRAY};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Custom3DView custom3DView = findViewById(R.id.activity_myview_customview);
        custom3DView.removeAllViewsInLayout();
        for (int i = 0; i < colors.length; i++) {
            ImageView imageView = new ImageView(this);
            imageView.setBackgroundColor(colors[i]);
            final int position = i;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, "点击了第" + (position + 1) + "张图片！", Toast.LENGTH_SHORT).show();
                }
            });
            custom3DView.addView(imageView);
        }

        final PickerView picker1 = findViewById(R.id.picker1);
        final PickerView picker2 = findViewById(R.id.picker2);

        picker1.setMinValue(1900);
        picker1.setMaxValue(2100);
        picker1.setFormatter(new PickerView.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%d年", value);
            }
        });
        picker2.setMinValue(0);
        picker2.setMaxValue(11);
        picker2.setFormatter(new PickerView.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%s月", PickerView.getTwoDigitFormatter().format(value + 1));
            }
        });

        final DatePicker datePicker = findViewById(R.id.date_picker);
        final TimePicker timePicker = findViewById(R.id.time_picker);
        final TextView tvTime = findViewById(R.id.tv_time);

        mCurrentDate.set(Calendar.YEAR, datePicker.getYear());
        mCurrentDate.set(Calendar.MONTH, datePicker.getMonth());
        mCurrentDate.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        mCurrentDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        mCurrentDate.set(Calendar.MINUTE, timePicker.getMinute());

        tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));

        datePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCurrentDate.set(Calendar.YEAR, year);
                mCurrentDate.set(Calendar.MONTH, monthOfYear);
                mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));
            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                mCurrentDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mCurrentDate.set(Calendar.MINUTE, minute);
                tvTime.setText(mDateFormat.format(mCurrentDate.getTime()));
            }
        });
    }
}
