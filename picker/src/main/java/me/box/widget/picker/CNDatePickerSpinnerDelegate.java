/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget.picker;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CalendarView;
import android.widget.LinearLayout;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import me.box.widget.picker.PickerView.OnValueChangeListener;

/**
 * A delegate implementing the basic DatePicker
 */
@SuppressWarnings("all")
class CNDatePickerSpinnerDelegate extends DatePicker.AbstractDatePickerDelegate {

    private static final String DATE_FORMAT = "MM/dd/yyyy";

    private static final int DEFAULT_START_YEAR = 1900;

    private static final int DEFAULT_END_YEAR = 2100;

    private static final boolean DEFAULT_CALENDAR_VIEW_SHOWN = true;

    private static final boolean DEFAULT_SPINNERS_SHOWN = true;

    private static final boolean DEFAULT_ENABLED_STATE = true;

    private final LinearLayout mSpinners;

    private final PickerView mDaySpinner;

    private final PickerView mMonthSpinner;

    private final PickerView mYearSpinner;

    private final CalendarView mCalendarView;

    private String[] mShortMonths;

    private final java.text.DateFormat mDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    private int mNumberOfMonths;

    private Calendar mTempDate;

    private Calendar mMinDate;

    private Calendar mMaxDate;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;

    private static final PickerView.Formatter sDayFormatter = new PickerView.Formatter() {
        @Override
        public String format(int value) {
            return String.format("%s日", PickerView.getTwoDigitFormatter().format(value));
        }
    };

    private static final PickerView.Formatter sMonthFormatter = new PickerView.Formatter() {
        @Override
        public String format(int value) {
            return String.format("%s月", PickerView.getTwoDigitFormatter().format(value));
        }
    };

    private static final PickerView.Formatter sYearFormatter = new PickerView.Formatter() {
        @Override
        public String format(int value) {
            return String.format("%d年", value);
        }
    };

    CNDatePickerSpinnerDelegate(DatePicker delegator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(delegator, context);

        mDelegator = delegator;
        mContext = context;

        // initialization based on locale
        setCurrentLocale(Locale.getDefault());

        final TypedArray attributesArray = context.obtainStyledAttributes(attrs,
                R.styleable.DatePicker, defStyleAttr, defStyleRes);
        boolean spinnersShown = true;
        boolean calendarViewShown = false;
        int startYear = attributesArray.getInt(R.styleable.DatePicker_startYear, DEFAULT_START_YEAR);
        int endYear = attributesArray.getInt(R.styleable.DatePicker_endYear, DEFAULT_END_YEAR);
        String minDate = attributesArray.getString(R.styleable.DatePicker_minDate);
        String maxDate = attributesArray.getString(R.styleable.DatePicker_maxDate);
        int layoutResourceId = attributesArray.getResourceId(
                R.styleable.DatePicker_internalLayout, R.layout.date_picker_material_cn);
        attributesArray.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(layoutResourceId, mDelegator, true);
        view.setSaveFromParentEnabled(false);

        OnValueChangeListener onChangeListener = new OnValueChangeListener() {
            public void onValueChange(PickerView picker, int oldVal, int newVal) {
                mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
                // take care of wrapping of days and months to update greater fields
                if (picker == mDaySpinner) {
                    int maxDayOfMonth = mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (oldVal == maxDayOfMonth && newVal == 1) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (oldVal == 1 && newVal == maxDayOfMonth) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                    } else {
                        mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                    }
                } else if (picker == mMonthSpinner) {
                    if (oldVal == 11 && newVal == 0) {
                        mTempDate.add(Calendar.MONTH, 1);
                    } else if (oldVal == 0 && newVal == 11) {
                        mTempDate.add(Calendar.MONTH, -1);
                    } else {
                        mTempDate.add(Calendar.MONTH, newVal - oldVal);
                    }
                } else if (picker == mYearSpinner) {
                    mTempDate.set(Calendar.YEAR, newVal);
                } else {
                    throw new IllegalArgumentException();
                }
                // now set the date to the adjusted one
                setDate(mTempDate.get(Calendar.YEAR), mTempDate.get(Calendar.MONTH), mTempDate.get(Calendar.DAY_OF_MONTH));
                updateSpinners();
                updateCalendarView();
                notifyDateChanged();
            }
        };

        mSpinners = (LinearLayout) mDelegator.findViewById(R.id.pickers);

        // calendar view day-picker
        mCalendarView = (CalendarView) mDelegator.findViewById(R.id.calendar_view);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            public void onSelectedDayChange(CalendarView view, int year, int month, int monthDay) {
                setDate(year, month, monthDay);
                updateSpinners();
                notifyDateChanged();
            }
        });

        final int numberpickerInputId = R.id.numberpicker_input;

        // day
        mDaySpinner = (PickerView) mDelegator.findViewById(R.id.day);
        mDaySpinner.setFormatter(sDayFormatter);
        mDaySpinner.setOnLongPressUpdateInterval(100);
        mDaySpinner.setOnValueChangedListener(onChangeListener);

        // month
        mMonthSpinner = (PickerView) mDelegator.findViewById(R.id.month);
        mMonthSpinner.setFormatter(sMonthFormatter);
        mMonthSpinner.setMinValue(0);
        mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
        mMonthSpinner.setDisplayedValues(mShortMonths);
        mMonthSpinner.setOnLongPressUpdateInterval(200);
        mMonthSpinner.setOnValueChangedListener(onChangeListener);

        // year
        mYearSpinner = (PickerView) mDelegator.findViewById(R.id.year);
        mYearSpinner.setFormatter(sYearFormatter);
        mYearSpinner.setOnLongPressUpdateInterval(100);
        mYearSpinner.setOnValueChangedListener(onChangeListener);

        // show only what the user required but make sure we
        // show something and the spinners have higher priority
        if (!spinnersShown && !calendarViewShown) {
            setSpinnersShown(true);
        } else {
            setSpinnersShown(spinnersShown);
            setCalendarViewShown(calendarViewShown);
        }

        // set the min date giving priority of the minDate over startYear
        mTempDate.clear();
        if (!TextUtils.isEmpty(minDate)) {
            if (!parseDate(minDate, mTempDate)) {
                mTempDate.set(startYear, 0, 1);
            }
        } else {
            mTempDate.set(startYear, 0, 1);
        }
        setMinDate(mTempDate.getTimeInMillis());

        // set the max date giving priority of the maxDate over endYear
        mTempDate.clear();
        if (!TextUtils.isEmpty(maxDate)) {
            if (!parseDate(maxDate, mTempDate)) {
                mTempDate.set(endYear, 11, 31);
            }
        } else {
            mTempDate.set(endYear, 11, 31);
        }
        setMaxDate(mTempDate.getTimeInMillis());

        // initialize to current date
        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate
                .get(Calendar.DAY_OF_MONTH), null);

        // re-order the number spinners to match the current date format
        reorderSpinners();

        // accessibility
        setContentDescriptions();

        // If not explicitly specified this view is important for accessibility.
        if (mDelegator.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            mDelegator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @Override
    public void init(int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener onDateChangedListener) {
        setDate(year, monthOfYear, dayOfMonth);
        updateSpinners();
        updateCalendarView();

        mOnDateChangedListener = onDateChangedListener;
    }

    @Override
    public void updateDate(int year, int month, int dayOfMonth) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateSpinners();
        updateCalendarView();
        notifyDateChanged();
    }

    @Override
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    @Override
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    @Override
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mCalendarView.setFirstDayOfWeek(firstDayOfWeek);
    }

    @Override
    public int getFirstDayOfWeek() {
        return mCalendarView.getFirstDayOfWeek();
    }

    @Override
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMinDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        mMinDate.setTimeInMillis(minDate);
        mCalendarView.setMinDate(minDate);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    @Override
    public Calendar getMinDate() {
        final Calendar minDate = Calendar.getInstance();
        minDate.setTimeInMillis(mCalendarView.getMinDate());
        return minDate;
    }

    @Override
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        mMaxDate.setTimeInMillis(maxDate);
        mCalendarView.setMaxDate(maxDate);
        if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    @Override
    public Calendar getMaxDate() {
        final Calendar maxDate = Calendar.getInstance();
        maxDate.setTimeInMillis(mCalendarView.getMaxDate());
        return maxDate;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mDaySpinner.setEnabled(enabled);
        mMonthSpinner.setEnabled(enabled);
        mYearSpinner.setEnabled(enabled);
        mCalendarView.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public CalendarView getCalendarView() {
        return mCalendarView;
    }

    @Override
    public void setCalendarViewShown(boolean shown) {
        mCalendarView.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean getCalendarViewShown() {
        return (mCalendarView.getVisibility() == View.VISIBLE);
    }

    @Override
    public void setSpinnersShown(boolean shown) {
        mSpinners.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean getSpinnersShown() {
        return mSpinners.isShown();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        return new SavedState(superState, getYear(), getMonth(), getDayOfMonth(),
                getMinDate().getTimeInMillis(), getMaxDate().getTimeInMillis());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            final SavedState ss = (SavedState) state;
            setDate(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
            updateSpinners();
            updateCalendarView();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    @Override
    protected void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);

        mTempDate = getCalendarForLocale(mTempDate, locale);
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

        mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
        mShortMonths = new DateFormatSymbols().getShortMonths();

        if (usingNumericMonths()) {
            // We're in a locale where a date should either be all-numeric, or all-text.
            // All-text would require custom NumberPicker formatters for day and year.
            mShortMonths = new String[mNumberOfMonths];
            for (int i = 0; i < mNumberOfMonths; ++i) {
                mShortMonths[i] = sMonthFormatter.format(i + 1);
            }
        }
    }

    /**
     * Tests whether the current locale is one where there are no real month names,
     * such as Chinese, Japanese, or Korean locales.
     */
    private boolean usingNumericMonths() {
        return Character.isDigit(mShortMonths[Calendar.JANUARY].charAt(0));
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale      The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    /**
     * Reorders the spinners according to the date format that is
     * explicitly set by the user and if no such is set fall back
     * to the current locale's default format.
     */
    private void reorderSpinners() {
        mSpinners.removeAllViews();
        // We use numeric spinners for year and day, but textual months. Ask icu4c what
        // order the user's locale uses for that combination. http://b/7207103.
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMdd");
        char[] order = DateFormat.getDateFormatOrder(mSpinners.getContext());
        final int spinnerCount = order.length;
        for (int i = 0; i < spinnerCount; i++) {
            switch (order[i]) {
                case 'd':
                    mSpinners.addView(mDaySpinner);
                    break;
                case 'M':
                    mSpinners.addView(mMonthSpinner);
                    break;
                case 'y':
                    mSpinners.addView(mYearSpinner);
                    break;
                default:
                    throw new IllegalArgumentException(Arrays.toString(order));
            }
        }
    }

    /**
     * Parses the given <code>date</code> and in case of success sets the result
     * to the <code>outDate</code>.
     *
     * @return True if the date was parsed.
     */
    private boolean parseDate(String date, Calendar outDate) {
        try {
            outDate.setTime(mDateFormat.parse(date));
            return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != month
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != dayOfMonth);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);
        resetAutofilledValue();
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }

    private void updateSpinners() {
        // set the spinner ranges respecting the min and max dates
        if (mCurrentDate.equals(mMinDate)) {
            mDaySpinner.setMinValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(mCurrentDate.get(Calendar.MONTH));
            mMonthSpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
            mMonthSpinner.setWrapSelectorWheel(false);
        } else if (mCurrentDate.equals(mMaxDate)) {
            mDaySpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
            mMonthSpinner.setMaxValue(mCurrentDate.get(Calendar.MONTH));
            mMonthSpinner.setWrapSelectorWheel(false);
        } else {
            mDaySpinner.setMinValue(1);
            mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(true);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(11);
            mMonthSpinner.setWrapSelectorWheel(true);
        }

        // make sure the month names are a zero based array
        // with the months in the month spinner
        String[] displayedValues = Arrays.copyOfRange(mShortMonths,
                mMonthSpinner.getMinValue(), mMonthSpinner.getMaxValue() + 1);
        mMonthSpinner.setDisplayedValues(displayedValues);

        // year spinner range does not change based on the current date
        mYearSpinner.setMinValue(mMinDate.get(Calendar.YEAR));
        mYearSpinner.setMaxValue(mMaxDate.get(Calendar.YEAR));
        mYearSpinner.setWrapSelectorWheel(false);

        // set the spinner values
        mYearSpinner.setValue(mCurrentDate.get(Calendar.YEAR));
        mMonthSpinner.setValue(mCurrentDate.get(Calendar.MONTH));
        mDaySpinner.setValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Updates the calendar view with the current date.
     */
    private void updateCalendarView() {
        mCalendarView.setDate(mCurrentDate.getTimeInMillis(), false, false);
    }


    /**
     * Notifies the listener, if such, for a change in the selected date.
     */
    private void notifyDateChanged() {
        mDelegator.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnDateChangedListener != null) {
            mOnDateChangedListener.onDateChanged(mDelegator, getYear(), getMonth(),
                    getDayOfMonth());
        }
        if (mAutoFillChangeListener != null) {
            mAutoFillChangeListener.onDateChanged(mDelegator, getYear(), getMonth(),
                    getDayOfMonth());
        }
    }

    private void setContentDescriptions() {
        // Day
        trySetContentDescription(mDaySpinner, R.id.increment,
                R.string.date_picker_increment_day_button);
        trySetContentDescription(mDaySpinner, R.id.increment,
                R.string.date_picker_decrement_day_button);
        // Month
        trySetContentDescription(mMonthSpinner, R.id.increment,
                R.string.date_picker_increment_month_button);
        trySetContentDescription(mMonthSpinner, R.id.increment,
                R.string.date_picker_decrement_month_button);
        // Year
        trySetContentDescription(mYearSpinner, R.id.increment,
                R.string.date_picker_increment_year_button);
        trySetContentDescription(mYearSpinner, R.id.increment,
                R.string.date_picker_decrement_year_button);
    }

    private void trySetContentDescription(View root, int viewId, int contDescResId) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setContentDescription(mContext.getString(contDescResId));
        }
    }
}
