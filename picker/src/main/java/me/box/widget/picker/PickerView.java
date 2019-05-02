/*
 * Copyright © 2017 CHANGLEI. All rights reserved.
 */

package me.box.widget.picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A widget that enables the user to select a number from a predefined range.
 * There are two flavors of this widget and which one is presented to the user
 * depends on the current theme.
 * <ul>
 * <li>
 * If the current theme is derived from {@link android.R.style#Theme} the widget
 * presents the current value as an editable input field with an increment button
 * above and a decrement button below. Long pressing the buttons allows for a quick
 * change of the current value. Tapping on the input field allows to type in
 * a desired value.
 * </li>
 * <li>
 * If the current theme is derived from {@link android.R.style#Theme_Holo} or
 * {@link android.R.style#Theme_Holo_Light} the widget presents the current
 * value as an editable input field with a lesser value above and a greater
 * value below. Tapping on the lesser or greater value selects it by animating
 * the number axis up or down to make the chosen value current. Flinging up
 * or down allows for multiple increments or decrements of the current value.
 * Long pressing on the lesser and greater values also allows for a quick change
 * of the current value. Tapping on the current value allows to type in a
 * desired value.
 * </li>
 * </ul>
 * <p>
 * For an example of using this widget, see {@link android.widget.TimePicker}.
 * </p>
 */
@SuppressWarnings({"SwitchStatementWithTooFewBranches", "UnusedReturnValue", "unused"})
public class PickerView extends LinearLayout {

    /**
     * The number of items show in the selector wheel.
     */
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;

    /**
     * The default update interval during long press.
     */
    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;

    /**
     * The coefficient by which to adjust (divide) the max fling velocity.
     */
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

    /**
     * The the duration for adjusting the selector wheel.
     */
    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;

    /**
     * The duration of scrolling while snapping to a given position.
     */
    private static final int SNAP_SCROLL_DURATION = 300;

    /**
     * The strength of fading in the top and bottom while drawing the selector.
     */
    private static final float TOP_AND_BOTTOM_FADING_EDGE_STRENGTH = 0.9f;

    /**
     * The default unscaled height of the selection divider.
     */
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2;

    /**
     * The default unscaled distance between the selection dividers.
     */
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE = 48;

    /**
     * The resource id for the default layout.
     */
    private static final int DEFAULT_LAYOUT_RESOURCE_ID = R.layout.picker_view_material;

    /**
     * Constant for unspecified size.
     */
    private static final int SIZE_UNSPECIFIED = -1;

    /**
     * User choice on whether the selector wheel should be wrapped.
     */
    private boolean mWrapSelectorWheelPreferred = true;

    private static class TwoDigitFormatter implements Formatter {
        final StringBuilder mBuilder = new StringBuilder();

        java.util.Formatter mFmt;

        final Object[] mArgs = new Object[1];

        TwoDigitFormatter() {
            final Locale locale = Locale.getDefault();
            init(locale);
        }

        private void init(Locale locale) {
            mFmt = createFormatter(locale);
        }

        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }

        private java.util.Formatter createFormatter(Locale locale) {
            return new java.util.Formatter(mBuilder, locale);
        }
    }

    private static final TwoDigitFormatter sTwoDigitFormatter = new TwoDigitFormatter();

    public static Formatter getTwoDigitFormatter() {
        return sTwoDigitFormatter;
    }

    /**
     * The text for showing the current value.
     */
    private final TextView mInputText;

    /**
     * The distance between the two selection dividers.
     */
    private final int mSelectionDividersDistance;

    /**
     * The min height of this widget.
     */
    private final int mMinHeight;

    /**
     * The max height of this widget.
     */
    private final int mMaxHeight;

    /**
     * The max width of this widget.
     */
    private final int mMinWidth;

    /**
     * The max width of this widget.
     */
    private int mMaxWidth;

    /**
     * Flag whether to compute the max width.
     */
    private final boolean mComputeMaxWidth;

    /**
     * The height of the text.
     */
    private final int mTextSize;

    /**
     * The height of the gap between text elements if the selector wheel.
     */
    private int mSelectorTextGapHeight;

    /**
     * The values to be displayed instead the indices.
     */
    private String[] mDisplayedValues;

    /**
     * Lower value of the range of numbers allowed for the PickerView
     */
    private int mMinValue;

    /**
     * Upper value of the range of numbers allowed for the PickerView
     */
    private int mMaxValue;

    /**
     * Current value of this PickerView
     */
    private int mValue;

    /**
     * Listener to be notified upon current value change.
     */
    private OnValueChangeListener mOnValueChangeListener;

    /**
     * Listener to be notified upon scroll state change.
     */
    private OnScrollListener mOnScrollListener;

    /**
     * Formatter for for displaying the current value.
     */
    private Formatter mFormatter;

    /**
     * The speed for updating the value form long press.
     */
    private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

    /**
     * Cache for the string representation of selector indices.
     */
    private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<>();

    /**
     * The selector indices whose value are show by the selector.
     */
    private final int[] mSelectorIndices;

    /**
     * The {@link Paint} for drawing the selector.
     */
    private final Paint mSelectorWheelPaint;

    /**
     * The height of a selector element (text + gap).
     */
    private int mSelectorElementHeight;

    /**
     * The initial offset of the scroll selector.
     */
    private int mInitialScrollOffset = Integer.MIN_VALUE;

    /**
     * The current offset of the scroll selector.
     */
    private int mCurrentScrollOffset;

    /**
     * The {@link Scroller} responsible for flinging the selector.
     */
    private final Scroller mFlingScroller;

    /**
     * The {@link Scroller} responsible for adjusting the selector.
     */
    private final Scroller mAdjustScroller;

    /**
     * The previous Y coordinate while scrolling the selector.
     */
    private int mPreviousScrollerY;

    /**
     * Handle to the reusable command for changing the current value from long
     * press by one.
     */
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;

    /**
     * Command for beginning an edit of the current value via IME on long press.
     */
    private BeginSoftInputOnLongPressCommand mBeginSoftInputOnLongPressCommand;

    /**
     * The Y position of the last down event.
     */
    private float mLastDownEventY;

    /**
     * The time of the last down event.
     */
    private long mLastDownEventTime;

    /**
     * The Y position of the last down or move event.
     */
    private float mLastDownOrMoveEventY;

    /**
     * Determines speed during touch scrolling.
     */
    private VelocityTracker mVelocityTracker;

    /**
     * @see ViewConfiguration#getScaledTouchSlop()
     */
    private int mTouchSlop;

    /**
     * @see ViewConfiguration#getScaledMinimumFlingVelocity()
     */
    private int mMinimumFlingVelocity;

    /**
     * @see ViewConfiguration#getScaledMaximumFlingVelocity()
     */
    private int mMaximumFlingVelocity;

    /**
     * Flag whether the selector should wrap around.
     */
    private boolean mWrapSelectorWheel;

    /**
     * The back ground color used to optimize scroller fading.
     */
    private final int mSolidColor;

    /**
     * Divider for showing item to be selected while scrolling
     */
    private final Drawable mSelectionDivider;

    /**
     * The height of the selection divider.
     */
    private final int mSelectionDividerHeight;

    /**
     * The current scroll state of the number picker.
     */
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /**
     * Flag whether to ignore move events - we ignore such when we show in IME
     * to prevent the content from scrolling.
     */
    private boolean mIgnoreMoveEvents;

    /**
     * Flag whether to perform a click on tap.
     */
    private boolean mPerformClickOnTap;

    /**
     * The top of the top selection divider.
     */
    private int mTopSelectionDividerTop;

    /**
     * The bottom of the bottom selection divider.
     */
    private int mBottomSelectionDividerBottom;

    /**
     * The virtual id of the last hovered child.
     */
    private int mLastHoveredChildVirtualViewId;

    /**
     * Whether the increment virtual button is pressed.
     */
    private boolean mIncrementVirtualButtonPressed;

    /**
     * Whether the decrement virtual button is pressed.
     */
    private boolean mDecrementVirtualButtonPressed;

    /**
     * Provider to report to clients the semantic structure of this widget.
     */
    private AccessibilityNodeProviderImpl mAccessibilityNodeProvider;

    /**
     * Helper class for managing pressed state of the virtual buttons.
     */
    private final PressedStateHelper mPressedStateHelper;

    /**
     * The keycode of the last handled DPAD down event.
     */
    private int mLastHandledDownDpadKeyCode = -1;

    private final int mMiddleItemIndex;

    private final float mWheelItemOffset;

    private float mMiddleItemY;

    private float mMiddleItemOffsetY;

    /**
     * Interface to listen for changes of the current value.
     */
    public interface OnValueChangeListener {

        /**
         * Called upon a change of the current value.
         *
         * @param picker The PickerView associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        void onValueChange(PickerView picker, int oldVal, int newVal);
    }

    /**
     * Interface to listen for the picker scroll state.
     */
    public interface OnScrollListener {
        @IntDef(value = {
                SCROLL_STATE_IDLE,
                SCROLL_STATE_TOUCH_SCROLL,
                SCROLL_STATE_FLING
        })
        @Retention(RetentionPolicy.SOURCE)
        @interface ScrollState {
        }

        /**
         * The view is not scrolling.
         */
        int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and his finger is still on the screen.
         */
        int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and performed a fling.
         */
        int SCROLL_STATE_FLING = 2;

        /**
         * Callback invoked while the number picker scroll state has changed.
         *
         * @param view        The view whose scroll state is being reported.
         * @param scrollState The current scroll state. One of
         *                    {@link #SCROLL_STATE_IDLE},
         *                    {@link #SCROLL_STATE_TOUCH_SCROLL} or
         *                    {@link #SCROLL_STATE_IDLE}.
         */
        void onScrollStateChange(PickerView view, @ScrollState int scrollState);
    }

    /**
     * Interface used to format current value into a string for presentation.
     */
    public interface Formatter {

        /**
         * Formats a string representation of the current value.
         *
         * @param value The currently selected value.
         * @return A formatted string representation.
         */
        String format(int value);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     */
    public PickerView(Context context) {
        this(context, null);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     * @param attrs   A collection of attributes.
     */
    public PickerView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pickerViewStyle);
    }

    /**
     * Create a new number picker
     *
     * @param context      the application environment.
     * @param attrs        a collection of attributes.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     */
    public PickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Create a new number picker
     *
     * @param context      the application environment.
     * @param attrs        a collection of attributes.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     * @param defStyleRes  A resource identifier of a style resource that
     *                     supplies default values for the view, used only if
     *                     defStyleAttr is 0 or can not be found in the theme. Can be 0
     *                     to not look for defaults.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // process style attributes
        final TypedArray attributesArray = context.obtainStyledAttributes(
                attrs, R.styleable.PickerView, defStyleAttr, defStyleRes);
        final int layoutResId = attributesArray.getResourceId(
                R.styleable.PickerView_internalLayout, DEFAULT_LAYOUT_RESOURCE_ID);

        mSolidColor = attributesArray.getColor(R.styleable.PickerView_solidColor, 0);

        final Drawable selectionDivider = attributesArray.getDrawable(
                R.styleable.PickerView_selectionDivider);
        if (selectionDivider != null) {
            selectionDivider.setCallback(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                selectionDivider.setLayoutDirection(getLayoutDirection());
            }
            if (selectionDivider.isStateful()) {
                selectionDivider.setState(getDrawableState());
            }
        }
        mSelectionDivider = selectionDivider;

        final int defSelectionDividerHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT,
                getResources().getDisplayMetrics());
        mSelectionDividerHeight = attributesArray.getDimensionPixelSize(
                R.styleable.PickerView_selectionDividerHeight, defSelectionDividerHeight);

        final int defSelectionDividerDistance = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE,
                getResources().getDisplayMetrics());
        mSelectionDividersDistance = attributesArray.getDimensionPixelSize(
                R.styleable.PickerView_selectionDividersDistance, defSelectionDividerDistance);

        mMinHeight = attributesArray.getDimensionPixelSize(
                R.styleable.PickerView_internalMinHeight, SIZE_UNSPECIFIED);

        mMaxHeight = attributesArray.getDimensionPixelSize(
                R.styleable.PickerView_internalMaxHeight, SIZE_UNSPECIFIED);
        if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
                && mMinHeight > mMaxHeight) {
            throw new IllegalArgumentException("minHeight > maxHeight");
        }

        mMinWidth = attributesArray.getDimensionPixelSize(
                R.styleable.PickerView_internalMinWidth, SIZE_UNSPECIFIED);

        mMaxWidth = attributesArray.getDimensionPixelSize(R.styleable.PickerView_internalMaxWidth, SIZE_UNSPECIFIED);
        if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED && mMinWidth > mMaxWidth) {
            throw new IllegalArgumentException("minWidth > maxWidth");
        }

        mComputeMaxWidth = (mMaxWidth == SIZE_UNSPECIFIED);

        int wheelItemCount = attributesArray.getInteger(
                R.styleable.PickerView_wheelItemCount, SELECTOR_WHEEL_ITEM_COUNT);
        if (wheelItemCount < SELECTOR_WHEEL_ITEM_COUNT || wheelItemCount % 2 == 0) {
            wheelItemCount = SELECTOR_WHEEL_ITEM_COUNT;
        }

        mSelectorIndices = new int[wheelItemCount];
        mMiddleItemIndex = mSelectorIndices.length >> 1;

        mWheelItemOffset = attributesArray.getFloat(R.styleable.PickerView_wheelItemOffset, 0.0f);

        attributesArray.recycle();

        mPressedStateHelper = new PressedStateHelper();

        // By default Linearlayout that we extend is not drawn. This is
        // its draw() method is not called but dispatchDraw() is called
        // directly (see ViewGroup.drawChild()). However, this class uses
        // the fading edge effect implemented by View and we need our
        // draw() method to be called. Therefore, we declare we will draw.
        setWillNotDraw(false);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(layoutResId, this, true);

        // input text
        mInputText = findViewById(R.id.picker_view_input);
        ViewCompat.setAccessibilityLiveRegion(mInputText, View.ACCESSIBILITY_LIVE_REGION_POLITE);

        mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        mInputText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mInputText.setVisibility(INVISIBLE);

        // initialize constants
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity()
                / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;
        mTextSize = (int) mInputText.getTextSize();

        // create the selector wheel paint
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(mTextSize);
        paint.setTypeface(mInputText.getTypeface());
        ColorStateList colors = mInputText.getTextColors();
        int color = colors.getColorForState(ENABLED_STATE_SET, Color.WHITE);
        paint.setColor(color);
        mSelectorWheelPaint = paint;

        // create the fling and adjust scrollers
        mFlingScroller = new Scroller(getContext(), null, true);
        mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));

        updateInputTextView();

        // If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        // Should be focusable by default, as the text view whose visibility changes is focusable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (getFocusable() == View.FOCUSABLE_AUTO) {
                setFocusable(View.FOCUSABLE);
                setFocusableInTouchMode(true);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int msrdWdth = getMeasuredWidth();
        final int msrdHght = getMeasuredHeight();

        // Input text centered horizontally.
        final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
        final int inptTxtMsrdHght = mInputText.getMeasuredHeight();
        final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
        final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
        final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
        final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
        mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

        if (changed) {
            // need to do all this when we know our size
            initializeSelectorWheel();
            initializeFadingEdges();
            mTopSelectionDividerTop = (getHeight() - mSelectionDividersDistance) / 2
                    - mSelectionDividerHeight;
            mBottomSelectionDividerBottom = mTopSelectionDividerTop + 2 * mSelectionDividerHeight
                    + mSelectionDividersDistance;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try greedily to fit the max width and height.
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
        // Flag if we are measured with width or height less than the respective min.
        // final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, getMeasuredWidth(), widthMeasureSpec);
        final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, getMeasuredHeight(), heightMeasureSpec);
        final int widthSize = resolveWithSize(heightSize, widthMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveWithSize(int heightSize, int widthMeasureSpec) {
        final int[] selectorIndices = mSelectorIndices;
        final int totalTextHeight = selectorIndices.length * mTextSize;
        final float totalTextGapHeight = heightSize - totalTextHeight;
        final float textGapCount = selectorIndices.length;
        final float selectorTextGapHeight = (int) (totalTextGapHeight / textGapCount + 0.5f);
        final float selectorElementHeight = mTextSize + selectorTextGapHeight;
        final int editTextTextPosition = mInputText.getBaseline() + mInputText.getTop();
        final float initialScrollOffset = editTextTextPosition - (selectorElementHeight * mMiddleItemIndex);
        final float middleItemY = heightSize / 2.f;
        final float middleItemOffsetY = mMiddleItemIndex * selectorElementHeight + initialScrollOffset - middleItemY;
        final Paint paint = new Paint(mSelectorWheelPaint);
        paint.setTextSize(getMaxTextSize(initialScrollOffset, middleItemOffsetY, middleItemY, selectorElementHeight));
        int maxWidth = tryComputeMaxWidth(paint);
        if (maxWidth <= Integer.MIN_VALUE) {
            maxWidth = mMaxWidth;
        }
        return resolveSizeAndStateRespectingMinSize(mMinWidth, maxWidth, widthMeasureSpec);
    }

    private float getMaxTextSize(float currentScrollOffset, float middleItemOffsetY, float middleItemY, float selectorElementHeight) {
        float y = currentScrollOffset;
        float textSize = mTextSize;
        for (int i = 0; i < mSelectorIndices.length; i++) {
            final float centerY = y - middleItemOffsetY;
            final float scale = 1.f - Math.abs((centerY - middleItemY) / middleItemY);
            final float realScale = (1.f - mWheelItemOffset) + mWheelItemOffset * scale;

            final float tmpTextSize = mTextSize * realScale;
            if (tmpTextSize > textSize) {
                textSize = tmpTextSize;
            }

            y += selectorElementHeight;
        }
        return textSize;
    }

    /**
     * Move to the final position of a scroller. Ensures to force finish the scroller
     * and if it is not at its final position a scroll of the selector wheel is
     * performed to fast forward to the final position.
     *
     * @param scroller The scroller to whose final position to get.
     * @return True of the a move was performed, i.e. the scroller was not in final position.
     */
    private boolean moveToFinalScrollerPosition(Scroller scroller) {
        scroller.forceFinished(true);
        int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
        int futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementHeight;
        int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
        if (overshootAdjustment != 0) {
            if (Math.abs(overshootAdjustment) > mSelectorElementHeight / 2) {
                if (overshootAdjustment > 0) {
                    overshootAdjustment -= mSelectorElementHeight;
                } else {
                    overshootAdjustment += mSelectorElementHeight;
                }
            }
            amountToScroll += overshootAdjustment;
            scrollBy(0, amountToScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                removeAllCallbacks();
                mLastDownOrMoveEventY = mLastDownEventY = event.getY();
                mLastDownEventTime = event.getEventTime();
                mIgnoreMoveEvents = false;
                mPerformClickOnTap = false;
                // Handle pressed state before any state change.
                if (mLastDownEventY < mTopSelectionDividerTop) {
                    if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mPressedStateHelper.buttonPressDelayed(
                                PressedStateHelper.BUTTON_DECREMENT);
                    }
                } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
                    if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mPressedStateHelper.buttonPressDelayed(
                                PressedStateHelper.BUTTON_INCREMENT);
                    }
                }
                // Make sure we support flinging inside scrollables.
                getParent().requestDisallowInterceptTouchEvent(true);
                if (!mFlingScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                } else if (!mAdjustScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                } else if (mLastDownEventY < mTopSelectionDividerTop) {
                    postChangeCurrentByOneFromLongPress(
                            false, ViewConfiguration.getLongPressTimeout());
                } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
                    postChangeCurrentByOneFromLongPress(
                            true, ViewConfiguration.getLongPressTimeout());
                } else {
                    mPerformClickOnTap = true;
                    postBeginSoftInputOnLongPressCommand();
                }
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                if (mIgnoreMoveEvents) {
                    break;
                }
                float currentMoveY = event.getY();
                if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                    if (deltaDownY > mTouchSlop) {
                        removeAllCallbacks();
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }
                } else {
                    int deltaMoveY = (int) ((currentMoveY - mLastDownOrMoveEventY));
                    scrollBy(0, deltaMoveY);
                    invalidate();
                }
                mLastDownOrMoveEventY = currentMoveY;
            }
            break;
            case MotionEvent.ACTION_UP: {
                removeBeginSoftInputCommand();
                removeChangeCurrentByOneFromLongPress();
                mPressedStateHelper.cancel();
                VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                    fling(initialVelocity);
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                } else {
                    int eventY = (int) event.getY();
                    int deltaMoveY = (int) Math.abs(eventY - mLastDownEventY);
                    long deltaTime = event.getEventTime() - mLastDownEventTime;
                    if (deltaMoveY <= mTouchSlop && deltaTime < ViewConfiguration.getTapTimeout()) {
                        if (mPerformClickOnTap) {
                            mPerformClickOnTap = false;
                            performClick();
                        } else {
                            int selectorIndexOffset = (eventY / mSelectorElementHeight) - mMiddleItemIndex;
                            if (selectorIndexOffset > 0) {
                                changeValueByOne(true);
                                mPressedStateHelper.buttonTapped(
                                        PressedStateHelper.BUTTON_INCREMENT);
                            } else if (selectorIndexOffset < 0) {
                                changeValueByOne(false);
                                mPressedStateHelper.buttonTapped(
                                        PressedStateHelper.BUTTON_DECREMENT);
                            }
                        }
                    } else {
                        ensureScrollWheelAdjusted();
                    }
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
            break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                removeAllCallbacks();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (mWrapSelectorWheel || ((keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                                ? getValue() < getMaxValue() : getValue() > getMinValue())) {
                            requestFocus();
                            mLastHandledDownDpadKeyCode = keyCode;
                            removeAllCallbacks();
                            if (mFlingScroller.isFinished()) {
                                changeValueByOne(keyCode == KeyEvent.KEYCODE_DPAD_DOWN);
                            }
                            return true;
                        }
                        break;
                    case KeyEvent.ACTION_UP:
                        if (mLastHandledDownDpadKeyCode == keyCode) {
                            mLastHandledDownDpadKeyCode = -1;
                            return true;
                        }
                        break;
                }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return super.dispatchTrackballEvent(event);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        final AccessibilityManager service = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (service != null && service.isEnabled()) {
            final int eventY = (int) event.getY();
            final int hoveredVirtualViewId;
            if (eventY < mTopSelectionDividerTop) {
                hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_DECREMENT;
            } else if (eventY > mBottomSelectionDividerBottom) {
                hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INCREMENT;
            } else {
                hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INPUT;
            }
            final int action = event.getActionMasked();
            AccessibilityNodeProviderImpl provider =
                    (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER: {
                    provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId,
                            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                    mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                    provider.performAction(hoveredVirtualViewId,
                            AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                }
                break;
                case MotionEvent.ACTION_HOVER_MOVE: {
                    if (mLastHoveredChildVirtualViewId != hoveredVirtualViewId
                            && mLastHoveredChildVirtualViewId != View.NO_ID) {
                        provider.sendAccessibilityEventForVirtualView(
                                mLastHoveredChildVirtualViewId,
                                AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                        provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId,
                                AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                        mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                        provider.performAction(hoveredVirtualViewId,
                                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                    }
                }
                break;
                case MotionEvent.ACTION_HOVER_EXIT: {
                    provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId,
                            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                    mLastHoveredChildVirtualViewId = View.NO_ID;
                }
                break;
            }
        }
        return false;
    }

    @Override
    public void computeScroll() {
        Scroller scroller = mFlingScroller;
        if (scroller.isFinished()) {
            scroller = mAdjustScroller;
            if (scroller.isFinished()) {
                return;
            }
        }
        scroller.computeScrollOffset();
        int currentScrollerY = scroller.getCurrY();
        if (mPreviousScrollerY == 0) {
            mPreviousScrollerY = scroller.getStartY();
        }
        scrollBy(0, currentScrollerY - mPreviousScrollerY);
        mPreviousScrollerY = currentScrollerY;
        if (scroller.isFinished()) {
            onScrollerFinished(scroller);
        } else {
            invalidate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mInputText.setEnabled(enabled);
    }

    @Override
    public void scrollBy(int x, int y) {
        int[] selectorIndices = mSelectorIndices;
        int startScrollOffset = mCurrentScrollOffset;
        if (!mWrapSelectorWheel && y > 0
                && selectorIndices[mMiddleItemIndex] <= mMinValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }
        if (!mWrapSelectorWheel && y < 0
                && selectorIndices[mMiddleItemIndex] >= mMaxValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }
        mCurrentScrollOffset += y;
        while (mCurrentScrollOffset - mInitialScrollOffset > mSelectorTextGapHeight) {
            mCurrentScrollOffset -= mSelectorElementHeight;
            decrementSelectorIndices(selectorIndices);
            setValueInternal(selectorIndices[mMiddleItemIndex], true);
            if (!mWrapSelectorWheel && selectorIndices[mMiddleItemIndex] <= mMinValue) {
                mCurrentScrollOffset = mInitialScrollOffset;
            }
        }
        while (mCurrentScrollOffset - mInitialScrollOffset < -mSelectorTextGapHeight) {
            mCurrentScrollOffset += mSelectorElementHeight;
            incrementSelectorIndices(selectorIndices);
            setValueInternal(selectorIndices[mMiddleItemIndex], true);
            if (!mWrapSelectorWheel && selectorIndices[mMiddleItemIndex] >= mMaxValue) {
                mCurrentScrollOffset = mInitialScrollOffset;
            }
        }
        if (startScrollOffset != mCurrentScrollOffset) {
            onScrollChanged(0, mCurrentScrollOffset, 0, startScrollOffset);
        }
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mCurrentScrollOffset;
    }

    @Override
    protected int computeVerticalScrollRange() {
        return (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    @Override
    public int getSolidColor() {
        return mSolidColor;
    }

    /**
     * Sets the listener to be notified on change of the current value.
     *
     * @param onValueChangedListener The listener.
     */
    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangeListener = onValueChangedListener;
    }

    /**
     * Set listener to be notified for scroll state changes.
     *
     * @param onScrollListener The listener.
     */
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    /**
     * Set the formatter to be used for formatting the current value.
     * <p>
     * Note: If you have provided alternative values for the values this
     * formatter is never invoked.
     * </p>
     *
     * @param formatter The formatter object. If formatter is <code>null</code>,
     *                  {@link String#valueOf(int)} will be used.
     * @see #setDisplayedValues(String[])
     */
    public void setFormatter(Formatter formatter) {
        if (formatter == mFormatter) {
            return;
        }
        mFormatter = formatter;
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
    }

    /**
     * Set the current value for the number picker.
     * <p>
     * If the argument is less than the {@link PickerView#getMinValue()} and
     * {@link PickerView#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link PickerView#getMinValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link PickerView#getMinValue()} and
     * {@link PickerView#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link PickerView#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link PickerView#getMaxValue()} and
     * {@link PickerView#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link PickerView#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link PickerView#getMaxValue()} and
     * {@link PickerView#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link PickerView#getMinValue()} value.
     * </p>
     *
     * @param value The current value.
     * @see #setWrapSelectorWheel(boolean)
     * @see #setMinValue(int)
     * @see #setMaxValue(int)
     */
    public void setValue(int value) {
        setValueInternal(value, false);
    }

    /**
     * Computes the max width if no such specified as an attribute.
     */
    private void tryComputeMaxWidth() {
        final int maxWidth = tryComputeMaxWidth(mSelectorWheelPaint);
        if (maxWidth > Integer.MIN_VALUE) {
            invalidate();
        }
    }

    private int tryComputeMaxWidth(@NonNull Paint paint) {
        if (!mComputeMaxWidth) {
            return Integer.MIN_VALUE;
        }
        int maxTextWidth = 0;
        if (mDisplayedValues == null) {
            for (int i = mMinValue; i < mMaxValue; i++) {
                final float textWidth = paint.measureText(formatNumber(i));
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        } else {
            for (String mDisplayedValue : mDisplayedValues) {
                final float textWidth = paint.measureText(mDisplayedValue);
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        }
        maxTextWidth += mInputText.getPaddingLeft() + mInputText.getPaddingRight();
        if (mMaxWidth != maxTextWidth) {
            if (maxTextWidth > mMinWidth) {
                mMaxWidth = maxTextWidth;
            } else {
                mMaxWidth = mMinWidth;
            }
            return mMaxWidth;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Gets whether the selector wheel wraps when reaching the min/max value.
     *
     * @return True if the selector wheel wraps.
     * @see #getMinValue()
     * @see #getMaxValue()
     */
    public boolean getWrapSelectorWheel() {
        return mWrapSelectorWheel;
    }

    /**
     * Sets whether the selector wheel shown during flinging/scrolling should
     * wrap around the {@link PickerView#getMinValue()} and
     * {@link PickerView#getMaxValue()} values.
     * <p>
     * By default if the range (max - min) is more than the number of items shown
     * on the selector wheel the selector wheel wrapping is enabled.
     * </p>
     * <p>
     * <strong>Note:</strong> If the number of items, i.e. the range (
     * {@link #getMaxValue()} - {@link #getMinValue()}) is less than
     * the number of items shown on the selector wheel, the selector wheel will
     * not wrap. Hence, in such a case calling this method is a NOP.
     * </p>
     *
     * @param wrapSelectorWheel Whether to wrap.
     */
    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mWrapSelectorWheelPreferred = wrapSelectorWheel;
        updateWrapSelectorWheel();

    }

    /**
     * Whether or not the selector wheel should be wrapped is determined by user choice and whether
     * the choice is allowed. The former comes from {@link #setWrapSelectorWheel(boolean)}, the
     * latter is calculated based on min & max value set vs selector's visual length. Therefore,
     * this method should be called any time any of the 3 values (i.e. user choice, min and max
     * value) gets updated.
     */
    private void updateWrapSelectorWheel() {
        final boolean wrappingAllowed = (mMaxValue - mMinValue) >= mSelectorIndices.length;
        mWrapSelectorWheel = wrappingAllowed && mWrapSelectorWheelPreferred;
    }

    /**
     * Sets the speed at which the numbers be incremented and decremented when
     * the up and down buttons are long pressed respectively.
     * <p>
     * The default value is 300 ms.
     * </p>
     *
     * @param intervalMillis The speed (in milliseconds) at which the numbers
     *                       will be incremented and decremented.
     */
    public void setOnLongPressUpdateInterval(long intervalMillis) {
        mLongPressUpdateInterval = intervalMillis;
    }

    /**
     * Returns the value of the picker.
     *
     * @return The value.
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Returns the min value of the picker.
     *
     * @return The min value
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * Sets the min value of the picker.
     *
     * @param minValue The min value inclusive.
     *
     *                 <strong>Note:</strong> The length of the displayed values array
     *                 set via {@link #setDisplayedValues(String[])} must be equal to the
     *                 range of selectable numbers which is equal to
     *                 {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setMinValue(int minValue) {
        if (mMinValue == minValue) {
            return;
        }
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue must be >= 0");
        }
        mMinValue = minValue;
        if (mMinValue > mValue) {
            mValue = mMinValue;
        }
        updateWrapSelectorWheel();
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
        invalidate();
    }

    /**
     * Returns the max value of the picker.
     *
     * @return The max value.
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Sets the max value of the picker.
     *
     * @param maxValue The max value inclusive.
     *
     *                 <strong>Note:</strong> The length of the displayed values array
     *                 set via {@link #setDisplayedValues(String[])} must be equal to the
     *                 range of selectable numbers which is equal to
     *                 {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setMaxValue(int maxValue) {
        if (mMaxValue == maxValue) {
            return;
        }
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        mMaxValue = maxValue;
        if (mMaxValue < mValue) {
            mValue = mMaxValue;
        }
        updateWrapSelectorWheel();
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
        invalidate();
    }

    /**
     * Gets the values to be displayed instead of string values.
     *
     * @return The displayed values.
     */
    public String[] getDisplayedValues() {
        return mDisplayedValues;
    }

    /**
     * Sets the values to be displayed.
     *
     * @param displayedValues The displayed values.
     *
     *                        <strong>Note:</strong> The length of the displayed values array
     *                        must be equal to the range of selectable numbers which is equal to
     *                        {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setDisplayedValues(String[] displayedValues) {
        if (mDisplayedValues == displayedValues) {
            return;
        }
        mDisplayedValues = displayedValues;
        if (mDisplayedValues != null) {
            // Allow text entry rather than strictly numeric entry.
            mInputText.setRawInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        updateInputTextView();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
    }

    /**
     * Retrieves the displayed value for the current selection in this picker.
     */
    public CharSequence getDisplayedValueForCurrentSelection() {
        // The cache field itself is initialized at declaration time, and since it's final, it
        // can't be null here. The cache is updated in ensureCachedScrollSelectorValue which is
        // called, directly or indirectly, on every call to setDisplayedValues, setFormatter,
        // setMinValue, setMaxValue and setValue, as well as user-driven interaction with the
        // picker. As such, the contents of the cache are always synced to the latest state of
        // the widget.
        return mSelectorIndexToStringCache.get(getValue());
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeAllCallbacks();
    }

    @CallSuper
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable selectionDivider = mSelectionDivider;
        if (selectionDivider != null && selectionDivider.isStateful()
                && selectionDivider.setState(getDrawableState())) {
            invalidateDrawable(selectionDivider);
        }
    }

    @CallSuper
    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mSelectionDivider != null) {
            mSelectionDivider.jumpToCurrentState();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mSelectionDivider != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mSelectionDivider.setLayoutDirection(newConfig.getLayoutDirection());
            }
        }
    }

    private final Camera mCamera = new Camera();
    private final Matrix mMatrix = new Matrix();

    @Override
    protected void onDraw(Canvas canvas) {
        mCamera.applyToCanvas(canvas);
        final float x = (getRight() - getLeft()) >> 1;
        float y = mCurrentScrollOffset;

        // draw the selector wheel
        for (final int selectorIndex : mSelectorIndices) {
            final String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            final float centerY = y - mMiddleItemOffsetY;
            final float scale = 1.f - Math.abs((centerY - mMiddleItemY) / mMiddleItemY);
            final float realScale = (1.f - mWheelItemOffset) + mWheelItemOffset * scale;
            final float degreeScale = (mMiddleItemY - centerY) / mMiddleItemY;
            final float degree = 90 * mWheelItemOffset * degreeScale;

            mCamera.save();
            mCamera.rotateX(degree);
            mCamera.getMatrix(mMatrix);
            mMatrix.preTranslate(-x, -centerY);
            mMatrix.postTranslate(x, centerY);
            mMatrix.postScale(realScale, realScale, x, centerY);
            mCamera.restore();

            mSelectorWheelPaint.setAlpha((int) (255.f * realScale));

            canvas.save();
            canvas.concat(mMatrix);
            canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
            canvas.restore();
            y += mSelectorElementHeight;
        }

        // draw the selection dividers
        if (mSelectionDivider != null) {
            // draw the top divider
            final int topOfTopDivider = mTopSelectionDividerTop;
            final int bottomOfTopDivider = topOfTopDivider + mSelectionDividerHeight;
            mSelectionDivider.setBounds(0, topOfTopDivider, getRight(), bottomOfTopDivider);
            mSelectionDivider.draw(canvas);

            // draw the bottom divider
            final int bottomOfBottomDivider = mBottomSelectionDividerBottom;
            final int topOfBottomDivider = bottomOfBottomDivider - mSelectionDividerHeight;
            mSelectionDivider.setBounds(0, topOfBottomDivider, getRight(), bottomOfBottomDivider);
            mSelectionDivider.draw(canvas);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(PickerView.class.getName());
        event.setScrollable(true);
        event.setScrollY((mMinValue + mValue) * mSelectorElementHeight);
        event.setMaxScrollY((mMaxValue - mMinValue) * mSelectorElementHeight);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new AccessibilityNodeProviderImpl();
        }
        return mAccessibilityNodeProvider;
    }

    /**
     * Makes a measure spec that tries greedily to use the max value.
     *
     * @param measureSpec The measure spec.
     * @param maxSize     The max value for the size.
     * @return A measure spec greedily imposing the max size.
     */
    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec;
        }
        final int size = MeasureSpec.getSize(measureSpec);
        final int mode = MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return measureSpec;
            case MeasureSpec.AT_MOST:
                return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED:
                return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY);
            default:
                throw new IllegalArgumentException("Unknown measure mode: " + mode);
        }
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec. Tries to respect the min size, unless a different size
     * is imposed by the constraints.
     *
     * @param minSize      The minimal desired size.
     * @param measuredSize The currently measured size.
     * @param measureSpec  The current measure spec.
     * @return The resolved size and state.
     */
    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize, int measureSpec) {
        if (minSize != SIZE_UNSPECIFIED) {
            final int desiredWidth = Math.max(minSize, measuredSize);
            return resolveSizeAndState(desiredWidth, measureSpec, 0);
        } else {
            return measuredSize;
        }
    }

    /**
     * Resets the selector indices and clear the cached string representation of
     * these indices.
     */
    private void initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear();
        int[] selectorIndices = mSelectorIndices;
        int current = getValue();
        for (int i = 0; i < mSelectorIndices.length; i++) {
            int selectorIndex = current + (i - mMiddleItemIndex);
            if (mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex);
            }
            selectorIndices[i] = selectorIndex;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    /**
     * Sets the current value of this PickerView.
     *
     * @param current      The new value of the PickerView.
     * @param notifyChange Whether to notify if the current value changed.
     */
    private void setValueInternal(int current, boolean notifyChange) {
        if (mValue == current) {
            return;
        }
        // Wrap around the values if we go past the start or end
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current);
        } else {
            current = Math.max(current, mMinValue);
            current = Math.min(current, mMaxValue);
        }
        int previous = mValue;
        mValue = current;
        // If we're flinging, we'll update the text view at the end when it becomes visible
        if (mScrollState != OnScrollListener.SCROLL_STATE_FLING) {
            updateInputTextView();
        }
        if (notifyChange) {
            notifyChange(previous, current);
        }
        initializeSelectorWheelIndices();
        invalidate();
    }

    /**
     * Changes the current value by one which is increment or
     * decrement based on the passes argument.
     * decrement the current value.
     *
     * @param increment True to increment, false to decrement.
     */
    private void changeValueByOne(boolean increment) {
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        mPreviousScrollerY = 0;
        if (increment) {
            mFlingScroller.startScroll(0, 0, 0, -mSelectorElementHeight, SNAP_SCROLL_DURATION);
        } else {
            mFlingScroller.startScroll(0, 0, 0, mSelectorElementHeight, SNAP_SCROLL_DURATION);
        }
        invalidate();
    }

    private void initializeSelectorWheel() {
        initializeSelectorWheelIndices();
        final int[] selectorIndices = mSelectorIndices;
        final int totalTextHeight = selectorIndices.length * mTextSize;
        final int measuredHeight = getMeasuredHeight();
        float totalTextGapHeight = measuredHeight - totalTextHeight;
        float textGapCount = selectorIndices.length;
        mSelectorTextGapHeight = (int) (totalTextGapHeight / textGapCount + 0.5f);
        mSelectorElementHeight = mTextSize + mSelectorTextGapHeight;
        // Ensure that the middle item is positioned the same as the text in
        // mInputText
        final int editTextTextPosition = mInputText.getBaseline() + mInputText.getTop();
        mInitialScrollOffset = editTextTextPosition - (mSelectorElementHeight * mMiddleItemIndex);
        mCurrentScrollOffset = mInitialScrollOffset;
        mMiddleItemY = measuredHeight / 2.f;
        mMiddleItemOffsetY = mMiddleItemIndex * mSelectorElementHeight + mInitialScrollOffset - mMiddleItemY;
        updateInputTextView();
    }

    private void initializeFadingEdges() {
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength((getBottom() - getTop() - mTextSize) / 2);
    }

    /**
     * Callback invoked upon completion of a given <code>scroller</code>.
     */
    private void onScrollerFinished(Scroller scroller) {
        if (scroller == mFlingScroller) {
            ensureScrollWheelAdjusted();
            updateInputTextView();
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else {
            if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                updateInputTextView();
            }
        }
    }

    /**
     * Handles transition to a given <code>scrollState</code>
     */
    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(this, scrollState);
        }
    }

    /**
     * Flings the selector with the given <code>velocityY</code>.
     */
    private void fling(int velocityY) {
        mPreviousScrollerY = 0;

        if (velocityY > 0) {
            mFlingScroller.fling(0, 0, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        } else {
            mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        }

        invalidate();
    }

    /**
     * @return The wrapped index <code>selectorIndex</code> value.
     */
    private int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
        }
        return selectorIndex;
    }

    /**
     * Increments the <code>selectorIndices</code> whose string representations
     * will be displayed in the selector.
     */
    private void incrementSelectorIndices(int[] selectorIndices) {
        if (selectorIndices.length - 1 >= 0) {
            System.arraycopy(selectorIndices, 1, selectorIndices, 0, selectorIndices.length - 1);
        }
        int nextScrollSelectorIndex = selectorIndices[selectorIndices.length - 2] + 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue;
        }
        selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    /**
     * Decrements the <code>selectorIndices</code> whose string representations
     * will be displayed in the selector.
     */
    private void decrementSelectorIndices(int[] selectorIndices) {
        if (selectorIndices.length - 1 >= 0) {
            System.arraycopy(selectorIndices, 0, selectorIndices, 1, selectorIndices.length - 1);
        }
        int nextScrollSelectorIndex = selectorIndices[1] - 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue;
        }
        selectorIndices[0] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    /**
     * Ensures we have a cached string representation of the given <code>
     * selectorIndex</code> to avoid multiple instantiations of the same string.
     */
    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = "";
        } else {
            if (mDisplayedValues != null) {
                int displayedValueIndex = selectorIndex - mMinValue;
                scrollSelectorValue = mDisplayedValues[displayedValueIndex];
            } else {
                scrollSelectorValue = formatNumber(selectorIndex);
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    private String formatNumber(int value) {
        return (mFormatter != null) ? mFormatter.format(value) : formatNumberWithLocale(value);
    }

    /**
     * Updates the view of this PickerView. If displayValues were specified in
     * the string corresponding to the index specified by the current value will
     * be returned. Otherwise, the formatter specified in {@link #setFormatter}
     * will be used to format the number.
     *
     * @return Whether the text was updated.
     */
    private boolean updateInputTextView() {
        /*
         * If we don't have displayed values then use the current number else
         * find the correct value in the displayed values for the current
         * number.
         */
        String text = (mDisplayedValues == null) ? formatNumber(mValue) : mDisplayedValues[mValue - mMinValue];
        if (!TextUtils.isEmpty(text)) {
            CharSequence beforeText = mInputText.getText();
            if (!text.equals(beforeText.toString())) {
                mInputText.setText(text);
                final AccessibilityManager service = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (service != null && service.isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
                    mInputText.onInitializeAccessibilityEvent(event);
                    mInputText.onPopulateAccessibilityEvent(event);
                    event.setFromIndex(0);
                    event.setRemovedCount(beforeText.length());
                    event.setAddedCount(text.length());
                    event.setBeforeText(beforeText);
                    event.setSource(PickerView.this, AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INPUT);
                    requestSendAccessibilityEvent(PickerView.this, event);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * PickerView.
     */
    private void notifyChange(int previous, int current) {
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(this, previous, current);
        }
    }

    /**
     * Posts a command for changing the current value by one.
     *
     * @param increment Whether to increment or decrement the value.
     */
    private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        } else {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        mChangeCurrentByOneFromLongPressCommand.setStep(increment);
        postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis);
    }

    /**
     * Removes the command for changing the current value by one.
     */
    private void removeChangeCurrentByOneFromLongPress() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
    }

    /**
     * Posts a command for beginning an edit of the current value via IME on
     * long press.
     */
    private void postBeginSoftInputOnLongPressCommand() {
        if (mBeginSoftInputOnLongPressCommand == null) {
            mBeginSoftInputOnLongPressCommand = new BeginSoftInputOnLongPressCommand();
        } else {
            removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
        postDelayed(mBeginSoftInputOnLongPressCommand, ViewConfiguration.getLongPressTimeout());
    }

    /**
     * Removes the command for beginning an edit of the current value via IME.
     */
    private void removeBeginSoftInputCommand() {
        if (mBeginSoftInputOnLongPressCommand != null) {
            removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
    }

    /**
     * Removes all pending callback from the message queue.
     */
    private void removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        if (mBeginSoftInputOnLongPressCommand != null) {
            removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
        mPressedStateHelper.cancel();
    }

    /**
     * Ensures that the scroll wheel is adjusted i.e. there is no offset and the
     * middle element is in the middle of the widget.
     *
     * @return Whether an adjustment has been made.
     */
    private boolean ensureScrollWheelAdjusted() {
        // adjust to the closest value
        int deltaY = mInitialScrollOffset - mCurrentScrollOffset;
        if (deltaY != 0) {
            mPreviousScrollerY = 0;
            if (Math.abs(deltaY) > mSelectorElementHeight / 2) {
                deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;
            }
            mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            invalidate();
            return true;
        }
        return false;
    }

    class PressedStateHelper implements Runnable {
        static final int BUTTON_INCREMENT = 1;
        static final int BUTTON_DECREMENT = 2;

        private final int MODE_PRESS = 1;
        private final int MODE_TAPPED = 2;

        private int mManagedButton;
        private int mMode;
        private int mRight;
        private int mBottom;

        PressedStateHelper() {
            mRight = getRight();
            mBottom = getBottom();
        }

        void cancel() {
            mMode = 0;
            mManagedButton = 0;
            PickerView.this.removeCallbacks(this);
            if (mIncrementVirtualButtonPressed) {
                mIncrementVirtualButtonPressed = false;
                invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
            }
            mDecrementVirtualButtonPressed = false;
        }

        void buttonPressDelayed(int button) {
            cancel();
            mMode = MODE_PRESS;
            mManagedButton = button;
            PickerView.this.postDelayed(this, ViewConfiguration.getTapTimeout());
        }

        void buttonTapped(int button) {
            cancel();
            mMode = MODE_TAPPED;
            mManagedButton = button;
            PickerView.this.post(this);
        }

        @Override
        public void run() {
            switch (mMode) {
                case MODE_PRESS: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            mIncrementVirtualButtonPressed = true;
                            invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                        }
                        break;
                        case BUTTON_DECREMENT: {
                            mDecrementVirtualButtonPressed = true;
                            invalidate(0, 0, mRight, mTopSelectionDividerTop);
                        }
                    }
                }
                break;
                case MODE_TAPPED: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            if (!mIncrementVirtualButtonPressed) {
                                PickerView.this.postDelayed(this,
                                        ViewConfiguration.getPressedStateDuration());
                            }
                            mIncrementVirtualButtonPressed ^= true;
                            invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                        }
                        break;
                        case BUTTON_DECREMENT: {
                            if (!mDecrementVirtualButtonPressed) {
                                PickerView.this.postDelayed(this,
                                        ViewConfiguration.getPressedStateDuration());
                            }
                            mDecrementVirtualButtonPressed ^= true;
                            invalidate(0, 0, mRight, mTopSelectionDividerTop);
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Command for changing the current value from a long press by one.
     */
    class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        private void setStep(boolean increment) {
            mIncrement = increment;
        }

        @Override
        public void run() {
            changeValueByOne(mIncrement);
            postDelayed(this, mLongPressUpdateInterval);
        }
    }

    /**
     * Command for beginning soft input on long press.
     */
    class BeginSoftInputOnLongPressCommand implements Runnable {

        @Override
        public void run() {
            performLongClick();
        }
    }

    /**
     * Class for managing virtual view tree rooted at this picker.
     */
    class AccessibilityNodeProviderImpl extends AccessibilityNodeProvider {
        private static final int UNDEFINED = Integer.MIN_VALUE;

        private static final int VIRTUAL_VIEW_ID_INCREMENT = 1;

        private static final int VIRTUAL_VIEW_ID_INPUT = 2;

        private static final int VIRTUAL_VIEW_ID_DECREMENT = 3;

        private final Rect mTempRect = new Rect();

        private final int[] mTempArray = new int[2];

        private int mAccessibilityFocusedView = UNDEFINED;
        private int mRight;
        private int mBottom;
        private int mLeft;
        private int mTop;
        private int mScrollX;
        private int mScrollY;

        AccessibilityNodeProviderImpl() {
            mRight = getRight();
            mBottom = getBottom();
            mLeft = getLeft();
            mTop = getTop();
            mScrollX = getScrollX();
            mScrollY = getScrollY();
        }

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            switch (virtualViewId) {
                case View.NO_ID:
                    return createAccessibilityNodeInfoForPickerView(mScrollX, mScrollY,
                            mScrollX + (mRight - mLeft), mScrollY + (mBottom - mTop));
                case VIRTUAL_VIEW_ID_DECREMENT:
                    return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_DECREMENT,
                            getVirtualDecrementButtonText(), mScrollX, mScrollY,
                            mScrollX + (mRight - mLeft),
                            mTopSelectionDividerTop + mSelectionDividerHeight);
                case VIRTUAL_VIEW_ID_INPUT:
                    return createAccessibiltyNodeInfoForInputText(mScrollX,
                            mTopSelectionDividerTop + mSelectionDividerHeight,
                            mScrollX + (mRight - mLeft),
                            mBottomSelectionDividerBottom - mSelectionDividerHeight);
                case VIRTUAL_VIEW_ID_INCREMENT:
                    return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_INCREMENT,
                            getVirtualIncrementButtonText(), mScrollX,
                            mBottomSelectionDividerBottom - mSelectionDividerHeight,
                            mScrollX + (mRight - mLeft), mScrollY + (mBottom - mTop));
            }
            return super.createAccessibilityNodeInfo(virtualViewId);
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = new ArrayList<>();
            switch (virtualViewId) {
                case View.NO_ID: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_DECREMENT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INPUT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INCREMENT, result);
                    return result;
                }
                case VIRTUAL_VIEW_ID_DECREMENT:
                case VIRTUAL_VIEW_ID_INCREMENT:
                case VIRTUAL_VIEW_ID_INPUT: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, virtualViewId, result);
                    return result;
                }
            }
            return super.findAccessibilityNodeInfosByText(searched, virtualViewId);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            switch (virtualViewId) {
                case View.NO_ID: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                requestFocus();
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                clearFocus();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                            if (PickerView.this.isEnabled() && (getWrapSelectorWheel() || getValue() < getMaxValue())) {
                                changeValueByOne(true);
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                            if (PickerView.this.isEnabled() && (getWrapSelectorWheel() || getValue() > getMinValue())) {
                                changeValueByOne(false);
                                return true;
                            }
                        }
                        return false;
                    }
                }
                break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_FOCUS: {
                            if (PickerView.this.isEnabled() && !mInputText.isFocused()) {
                                return mInputText.requestFocus();
                            }
                        }
                        break;
                        case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS: {
                            if (PickerView.this.isEnabled() && mInputText.isFocused()) {
                                mInputText.clearFocus();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (PickerView.this.isEnabled()) {
                                performClick();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_LONG_CLICK: {
                            if (PickerView.this.isEnabled()) {
                                performLongClick();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mInputText.invalidate();
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mInputText.invalidate();
                                return true;
                            }
                        }
                        return false;
                        default: {
                            return mInputText.performAccessibilityAction(action, arguments);
                        }
                    }
                }
                return false;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (PickerView.this.isEnabled()) {
                                PickerView.this.changeValueByOne(true);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        }
                        return false;
                    }
                }
                return false;
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (PickerView.this.isEnabled()) {
                                final boolean increment = false;
                                PickerView.this.changeValueByOne(increment);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        }
                        return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        }
                        return false;
                    }
                }
                return false;
            }
            return super.performAction(virtualViewId, action, arguments);
        }

        private void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    if (hasVirtualDecrementButton()) {
                        sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualDecrementButtonText());
                    }
                }
                break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    sendAccessibilityEventForVirtualText(eventType);
                }
                break;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    if (hasVirtualIncrementButton()) {
                        sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualIncrementButtonText());
                    }
                }
                break;
            }
        }

        private void sendAccessibilityEventForVirtualText(int eventType) {
            final AccessibilityManager service = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (service != null && service.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                mInputText.onInitializeAccessibilityEvent(event);
                mInputText.onPopulateAccessibilityEvent(event);
                event.setSource(PickerView.this, VIRTUAL_VIEW_ID_INPUT);
                requestSendAccessibilityEvent(PickerView.this, event);
            }
        }

        private void sendAccessibilityEventForVirtualButton(int virtualViewId, int eventType, String text) {
            final AccessibilityManager service = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (service != null && service.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setClassName(Button.class.getName());
                event.setPackageName(getContext().getPackageName());
                event.getText().add(text);
                event.setEnabled(PickerView.this.isEnabled());
                event.setSource(PickerView.this, virtualViewId);
                requestSendAccessibilityEvent(PickerView.this, event);
            }
        }

        private void findAccessibilityNodeInfosByTextInChild(String searchedLowerCase, int virtualViewId, List<AccessibilityNodeInfo> outResult) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    String text = getVirtualDecrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_DECREMENT));
                    }
                }
                break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    CharSequence text = mInputText.getText();
                    if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INPUT));
                        return;
                    }
                    CharSequence contentDesc = mInputText.getText();
                    if (!TextUtils.isEmpty(contentDesc) && contentDesc.toString().toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INPUT));
                        return;
                    }
                }
                break;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    String text = getVirtualIncrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INCREMENT));
                    }
                }
                break;
            }
        }

        private AccessibilityNodeInfo createAccessibiltyNodeInfoForInputText(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = mInputText.createAccessibilityNodeInfo();
            info.setSource(PickerView.this, VIRTUAL_VIEW_ID_INPUT);
            if (mAccessibilityFocusedView != VIRTUAL_VIEW_ID_INPUT) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == VIRTUAL_VIEW_ID_INPUT) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(isVisibleToUser(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            int[] locationOnScreen = mTempArray;
            getLocationOnScreen(locationOnScreen);
            boundsInParent.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInParent);
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForVirtualButton(int virtualViewId, String text, int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(Button.class.getName());
            info.setPackageName(getContext().getPackageName());
            info.setSource(PickerView.this, virtualViewId);
            info.setParent(PickerView.this);
            info.setText(text);
            info.setClickable(true);
            info.setLongClickable(true);
            info.setEnabled(PickerView.this.isEnabled());
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(isVisibleToUser(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            int[] locationOnScreen = mTempArray;
            getLocationOnScreen(locationOnScreen);
            boundsInParent.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInParent);

            if (mAccessibilityFocusedView != virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (PickerView.this.isEnabled()) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForPickerView(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(PickerView.class.getName());
            info.setPackageName(getContext().getPackageName());
            info.setSource(PickerView.this);

            if (hasVirtualDecrementButton()) {
                info.addChild(PickerView.this, VIRTUAL_VIEW_ID_DECREMENT);
            }
            info.addChild(PickerView.this, VIRTUAL_VIEW_ID_INPUT);
            if (hasVirtualIncrementButton()) {
                info.addChild(PickerView.this, VIRTUAL_VIEW_ID_INCREMENT);
            }

            info.setParent((View) getParentForAccessibility());
            info.setEnabled(PickerView.this.isEnabled());
            info.setScrollable(true);

            final float applicationScale = getResources().getConfiguration().fontScale;

            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setBoundsInParent(scale(boundsInParent, applicationScale));

            info.setVisibleToUser(isVisibleToUser(boundsInParent));

            int[] locationOnScreen = mTempArray;
            getLocationOnScreen(locationOnScreen);
            boundsInParent.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(scale(boundsInParent, applicationScale));

            if (mAccessibilityFocusedView != View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (PickerView.this.isEnabled()) {
                if (getWrapSelectorWheel() || getValue() < getMaxValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
                if (getWrapSelectorWheel() || getValue() > getMinValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            }

            return info;
        }

        private boolean hasVirtualDecrementButton() {
            return getWrapSelectorWheel() || getValue() > getMinValue();
        }

        private boolean hasVirtualIncrementButton() {
            return getWrapSelectorWheel() || getValue() < getMaxValue();
        }

        private String getVirtualDecrementButtonText() {
            int value = mValue - 1;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value >= mMinValue) {
                return (mDisplayedValues == null) ? formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            return null;
        }

        private String getVirtualIncrementButtonText() {
            int value = mValue + 1;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value <= mMaxValue) {
                return (mDisplayedValues == null) ? formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            return null;
        }

        private boolean isVisibleToUser(Rect localRect) {
            if (localRect != null && !localRect.isEmpty()) {
                if (getWindowVisibility() != View.VISIBLE) {
                    return false;
                } else {
                    ViewParent viewParent;
                    View view;
                    for (viewParent = getParent(); viewParent instanceof View; viewParent = view.getParent()) {
                        view = (View) viewParent;
                        if (view.getAlpha() <= 0.0F || view.getVisibility() != View.VISIBLE) {
                            return false;
                        }
                    }

                    return viewParent != null;
                }
            } else {
                return false;
            }
        }
    }

    static private String formatNumberWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    static Rect scale(@NonNull Rect rect, float scale) {
        int left = rect.left;
        int top = rect.top;
        int right = rect.right;
        int bottom = rect.bottom;
        if (scale != 1.0f) {
            left = (int) (left * scale + 0.5f);
            top = (int) (top * scale + 0.5f);
            right = (int) (right * scale + 0.5f);
            bottom = (int) (bottom * scale + 0.5f);
        }
        rect.set(left, top, right, bottom);
        return rect;
    }
}
