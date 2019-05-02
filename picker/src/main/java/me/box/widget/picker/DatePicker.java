/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget.picker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.icu.util.TimeZone;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.CalendarView;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Locale;

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
public class DatePicker extends FrameLayout {
    private static final String LOG_TAG = DatePicker.class.getSimpleName();

    private final DatePickerDelegate mDelegate;

    /**
     * The callback used to indicate the user changed the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.datePickerStyle);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_Material_DatePicker);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // DatePicker is important by default, unless app developer overrode attribute.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (getImportantForAutofill() == IMPORTANT_FOR_AUTOFILL_AUTO) {
                setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
            }
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyleAttr, defStyleRes);
        final int firstDayOfWeek = a.getInt(R.styleable.DatePicker_firstDayOfWeek, 0);
        a.recycle();

        mDelegate = createSpinnerUIDelegate(context, attrs, defStyleAttr, defStyleRes);

        if (firstDayOfWeek != 0) {
            setFirstDayOfWeek(firstDayOfWeek);
        }

        mDelegate.setAutoFillChangeListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final Context context = getContext();
                    final AutofillManager afm = context.getSystemService(AutofillManager.class);
                    if (afm != null) {
                        afm.notifyValueChanged(DatePicker.this);
                    }
                }
            }
        });
    }

    DatePickerDelegate createSpinnerUIDelegate(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        return new DatePickerSpinnerDelegate(this, context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param year                  The initial year.
     * @param monthOfYear           The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth            The initial day of the month.
     * @param onDateChangedListener How user is notified date is changed by
     *                              user, can be null.
     */
    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener) {
        mDelegate.init(year, monthOfYear, dayOfMonth, onDateChangedListener);
    }

    /**
     * Set the callback that indicates the date has been adjusted by the user.
     *
     * @param onDateChangedListener How user is notified date is changed by
     *                              user, can be null.
     */
    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        mDelegate.setOnDateChangedListener(onDateChangedListener);
    }

    /**
     * Update the current date.
     *
     * @param year       The year.
     * @param month      The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    public void updateDate(int year, int month, int dayOfMonth) {
        mDelegate.updateDate(year, month, dayOfMonth);
    }

    /**
     * @return The selected year.
     */
    public int getYear() {
        return mDelegate.getYear();
    }

    /**
     * @return The selected month.
     */
    public int getMonth() {
        return mDelegate.getMonth();
    }

    /**
     * @return The selected day of month.
     */
    public int getDayOfMonth() {
        return mDelegate.getDayOfMonth();
    }

    /**
     * Gets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default minimal date is 01/01/1900.
     * <p>
     *
     * @return The minimal supported date.
     */
    public long getMinDate() {
        return mDelegate.getMinDate().getTimeInMillis();
    }

    /**
     * Sets the minimal date supported by this {@link NumberPicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mDelegate.setMinDate(minDate);
    }

    /**
     * Gets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default maximal date is 12/31/2100.
     * <p>
     *
     * @return The maximal supported date.
     */
    public long getMaxDate() {
        return mDelegate.getMaxDate().getTimeInMillis();
    }

    /**
     * Sets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mDelegate.setMaxDate(maxDate);
    }

    /**
     * Sets the callback that indicates the current date is valid.
     *
     * @param callback the callback, may be null
     * @hide
     */
    public void setValidationCallback(@Nullable ValidationCallback callback) {
        mDelegate.setValidationCallback(callback);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mDelegate.isEnabled() == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDelegate.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mDelegate.isEnabled();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final AccessibilityDelegate accessibilityDelegate = (AccessibilityDelegate) getValue(this, "mAccessibilityDelegate");
        if (accessibilityDelegate != null) {
            return accessibilityDelegate.dispatchPopulateAccessibilityEvent(this, event);
        } else {
            return dispatchPopulateAccessibilityEventInternal(event);
        }
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        return mDelegate.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        final AccessibilityDelegate accessibilityDelegate = (AccessibilityDelegate) getValue(this, "mAccessibilityDelegate");
        if (accessibilityDelegate != null) {
            accessibilityDelegate.onPopulateAccessibilityEvent(this, event);
        } else {
            onPopulateAccessibilityEventInternal(event);
        }
    }

    /**
     * @hide
     */
    public void onPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if ((event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                    && !TextUtils.isEmpty(getAccessibilityPaneTitle())) {
                event.getText().add(getAccessibilityPaneTitle());
            }
        }
        mDelegate.onPopulateAccessibilityEvent(event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return DatePicker.class.getName();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDelegate.onConfigurationChanged(newConfig);
    }

    /**
     * Sets the first day of week.
     *
     * @param firstDayOfWeek The first day of the week conforming to the
     *                       {@link CalendarView} APIs.
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }
        mDelegate.setFirstDayOfWeek(firstDayOfWeek);
    }

    /**
     * Gets the first day of week.
     *
     * @return The first day of the week conforming to the {@link CalendarView}
     * APIs.
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     */
    public int getFirstDayOfWeek() {
        return mDelegate.getFirstDayOfWeek();
    }

    /**
     * Returns whether the {@link CalendarView} is shown.
     *
     * @return {@code true} if the calendar view is shown
     * @see #getCalendarView()
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public boolean getCalendarViewShown() {
        return mDelegate.getCalendarViewShown();
    }

    /**
     * Returns the {@link CalendarView} used by this picker.
     * <p>
     * <strong>Note:</strong> This method throws an
     * {@link UnsupportedOperationException} when the
     * to {@code calendar}.
     *
     * @return the calendar view
     * @throws UnsupportedOperationException if called when the picker is
     *                                       displayed in {@code calendar} mode
     * @see #getCalendarViewShown()
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public CalendarView getCalendarView() {
        return mDelegate.getCalendarView();
    }

    /**
     * Sets whether the {@link CalendarView} is shown.
     * <p>
     * <strong>Note:</strong> Calling this method has no effect when the
     *
     * @param shown {@code true} to show the calendar view, {@code false} to
     *              hide it
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public void setCalendarViewShown(boolean shown) {
        mDelegate.setCalendarViewShown(shown);
    }

    /**
     * Returns whether the spinners are shown.
     * <p>
     * <strong>Note:</strong> his method returns {@code false} when the
     *
     * @return {@code true} if the spinners are shown
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public boolean getSpinnersShown() {
        return mDelegate.getSpinnersShown();
    }

    /**
     * Sets whether the spinners are shown.
     * <p>
     * Calling this method has no effect when the
     *
     * @param shown {@code true} to show the spinners, {@code false} to hide
     *              them
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public void setSpinnersShown(boolean shown) {
        mDelegate.setSpinnersShown(shown);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return mDelegate.onSaveInstanceState(superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState ss = (BaseSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mDelegate.onRestoreInstanceState(ss);
    }

    /**
     * A delegate interface that defined the public API of the DatePicker. Allows different
     * DatePicker implementations. This would need to be implemented by the DatePicker delegates
     * for the real behavior.
     */
    interface DatePickerDelegate {
        void init(int year, int monthOfYear, int dayOfMonth,
                  OnDateChangedListener onDateChangedListener);

        void setOnDateChangedListener(OnDateChangedListener onDateChangedListener);

        void setAutoFillChangeListener(OnDateChangedListener onDateChangedListener);

        void updateDate(int year, int month, int dayOfMonth);

        int getYear();

        int getMonth();

        int getDayOfMonth();

        void autofill(AutofillValue value);

        AutofillValue getAutofillValue();

        void setFirstDayOfWeek(int firstDayOfWeek);

        int getFirstDayOfWeek();

        void setMinDate(long minDate);

        Calendar getMinDate();

        void setMaxDate(long maxDate);

        Calendar getMaxDate();

        void setEnabled(boolean enabled);

        boolean isEnabled();

        CalendarView getCalendarView();

        void setCalendarViewShown(boolean shown);

        boolean getCalendarViewShown();

        void setSpinnersShown(boolean shown);

        boolean getSpinnersShown();

        void setValidationCallback(ValidationCallback callback);

        void onConfigurationChanged(Configuration newConfig);

        Parcelable onSaveInstanceState(Parcelable superState);

        void onRestoreInstanceState(Parcelable state);

        boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

        void onPopulateAccessibilityEvent(AccessibilityEvent event);
    }

    /**
     * An abstract class which can be used as a start for DatePicker implementations
     */
    abstract static class AbstractDatePickerDelegate implements DatePickerDelegate {
        // The delegator
        protected DatePicker mDelegator;

        // The context
        protected Context mContext;

        // NOTE: when subclasses change this variable, they must call resetAutofilledValue().
        protected Calendar mCurrentDate;

        // The current locale
        protected Locale mCurrentLocale;

        // Callbacks
        protected OnDateChangedListener mOnDateChangedListener;
        protected OnDateChangedListener mAutoFillChangeListener;
        protected ValidationCallback mValidationCallback;

        // The value that was passed to autofill() - it must be stored because it getAutofillValue()
        // must return the exact same value that was autofilled, otherwise the widget will not be
        // properly highlighted after autofill().
        private long mAutofilledValue;

        public AbstractDatePickerDelegate(DatePicker delegator, Context context) {
            mDelegator = delegator;
            mContext = context;

            setCurrentLocale(Locale.getDefault());
        }

        protected void setCurrentLocale(Locale locale) {
            if (!locale.equals(mCurrentLocale)) {
                mCurrentLocale = locale;
                onLocaleChanged(locale);
            }
        }

        @Override
        public void setOnDateChangedListener(OnDateChangedListener callback) {
            mOnDateChangedListener = callback;
        }

        @Override
        public void setAutoFillChangeListener(OnDateChangedListener callback) {
            mAutoFillChangeListener = callback;
        }

        @Override
        public void setValidationCallback(ValidationCallback callback) {
            mValidationCallback = callback;
        }

        @Override
        public final void autofill(AutofillValue value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (value == null || !value.isDate()) {
                    Log.w(LOG_TAG, value + " could not be autofilled into " + this);
                    return;
                }
            }

            long time = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                time = value.getDateValue();
            }

            final Calendar cal = Calendar.getInstance(mCurrentLocale);
            cal.setTimeInMillis(time);
            updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            // Must set mAutofilledValue *after* calling subclass method to make sure the value
            // returned by getAutofillValue() matches it.
            mAutofilledValue = time;
        }

        @Override
        public final AutofillValue getAutofillValue() {
            final long time = mAutofilledValue != 0
                    ? mAutofilledValue
                    : mCurrentDate.getTimeInMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return AutofillValue.forDate(time);
            }
            return null;
        }

        /**
         * This method must be called every time the value of the year, month, and/or day is
         * changed by a subclass method.
         */
        protected void resetAutofilledValue() {
            mAutofilledValue = 0;
        }

        protected void onValidationChanged(boolean valid) {
            if (mValidationCallback != null) {
                mValidationCallback.onValidationChanged(valid);
            }
        }

        protected void onLocaleChanged(Locale locale) {
            // Stub.
        }

        @Override
        public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
            event.getText().add(getFormattedCurrentDate());
        }

        protected String getFormattedCurrentDate() {
            return DateUtils.formatDateTime(mContext, mCurrentDate.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                            | DateUtils.FORMAT_SHOW_WEEKDAY);
        }

        /**
         * Class for managing state storing/restoring.
         */
        static class SavedState extends BaseSavedState {
            private final int mSelectedYear;
            private final int mSelectedMonth;
            private final int mSelectedDay;
            private final long mMinDate;
            private final long mMaxDate;
            private final int mCurrentView;
            private final int mListPosition;
            private final int mListPositionOffset;

            public SavedState(Parcelable superState, int year, int month, int day, long minDate,
                              long maxDate) {
                this(superState, year, month, day, minDate, maxDate, 0, 0, 0);
            }

            /**
             * Constructor called from {@link DatePicker#onSaveInstanceState()}
             */
            public SavedState(Parcelable superState, int year, int month, int day, long minDate,
                              long maxDate, int currentView, int listPosition, int listPositionOffset) {
                super(superState);
                mSelectedYear = year;
                mSelectedMonth = month;
                mSelectedDay = day;
                mMinDate = minDate;
                mMaxDate = maxDate;
                mCurrentView = currentView;
                mListPosition = listPosition;
                mListPositionOffset = listPositionOffset;
            }

            /**
             * Constructor called from {@link #CREATOR}
             */
            private SavedState(Parcel in) {
                super(in);
                mSelectedYear = in.readInt();
                mSelectedMonth = in.readInt();
                mSelectedDay = in.readInt();
                mMinDate = in.readLong();
                mMaxDate = in.readLong();
                mCurrentView = in.readInt();
                mListPosition = in.readInt();
                mListPositionOffset = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                super.writeToParcel(dest, flags);
                dest.writeInt(mSelectedYear);
                dest.writeInt(mSelectedMonth);
                dest.writeInt(mSelectedDay);
                dest.writeLong(mMinDate);
                dest.writeLong(mMaxDate);
                dest.writeInt(mCurrentView);
                dest.writeInt(mListPosition);
                dest.writeInt(mListPositionOffset);
            }

            public int getSelectedDay() {
                return mSelectedDay;
            }

            public int getSelectedMonth() {
                return mSelectedMonth;
            }

            public int getSelectedYear() {
                return mSelectedYear;
            }

            public long getMinDate() {
                return mMinDate;
            }

            public long getMaxDate() {
                return mMaxDate;
            }

            public int getCurrentView() {
                return mCurrentView;
            }

            public int getListPosition() {
                return mListPosition;
            }

            public int getListPositionOffset() {
                return mListPositionOffset;
            }

            // suppress unused and hiding
            public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     */
    public interface ValidationCallback {
        void onValidationChanged(boolean valid);
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags) {
        // This view is self-sufficient for autofill, so it needs to call
        // onProvideAutoFillStructure() to fill itself, but it does not need to call
        // dispatchProvideAutoFillStructure() to fill its children.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            structure.setAutofillId(getAutofillId());
            onProvideAutofillStructure(structure, flags);
        }
    }

    @Override
    public void autofill(AutofillValue value) {
        if (!isEnabled()) return;

        mDelegate.autofill(value);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public int getAutofillType() {
        return isEnabled() ? AUTOFILL_TYPE_DATE : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue() {
        return isEnabled() ? mDelegate.getAutofillValue() : null;
    }

    public static Object getValue(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        String[] fs = fieldName.split("\\.");
        try {
            for (int i = 0; i < fs.length - 1; i++) {
                Field f = clazz.getDeclaredField(fs[i]);
                f.setAccessible(true);
                target = f.get(target);
                clazz = target.getClass();
            }
            Field f = clazz.getDeclaredField(fs[fs.length - 1]);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
