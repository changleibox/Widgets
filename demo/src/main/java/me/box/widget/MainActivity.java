/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;

import me.box.widget.picker.DatePicker;

/**
 * Created by box on 2019-04-30.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final DatePicker datePicker = findViewById(R.id.date_picker);
        final TextView tvTime = findViewById(R.id.tv_time);

        datePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                final Calendar cal = Calendar.getInstance();
                cal.clear();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, monthOfYear);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                tvTime.setText(DateFormat.getDateInstance().format(cal.getTime()));
            }
        });
    }
}
