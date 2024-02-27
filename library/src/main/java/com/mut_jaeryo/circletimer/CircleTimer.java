package com.mut_jaeryo.circletimer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

/* 이 객체는
https://github.com/JesusM/HoloCircleSeekBar 이 링크에서 제공해준 Circle을 참조한 것이다.
*/
public class CircleTimer extends View {

    public static final float POINTER_RADIUS_DEF_VALUE = 8;
    public static final int MAX_POINT_DEF_VALUE = 60 * 60; //1시간
    public static final int START_ANGLE_DEF_VALUE = 0;

    private static final String STATE_PARENT = "parent";
    private static final String STATE_ANGLE = "angle";
    private static final int END_WHEEL_DEFAULT_VALUE = 360;
    
    private boolean isRunning = false;
    private baseTimerEndedListener baseTimerEndedListener;
    private OnCircleSeekBarChangeListener mOnCircleSeekBarChangeListener; // SeekBar 클릭 리스너 정의

    /**
     * {@code Paint} instance used to draw the color wheel.
     */
    private Paint wheelPaint;

    /**
     * {@code Paint} instance used to draw the pointer's "halo".
     */
    private Paint outLinePaint;

    /**
     * {@code Paint} instance used to draw the pointer (the selected color).
     */
    private Paint highlightWheelPaint;

    /**
     * The stroke width used to paint the color wheel (in pixels).
     */
    private int mColorWheelStrokeWidth;

    /**
     * The radius of the pointer (in pixels).
     */
    private float mPointerRadius;

    /**
     * The rectangle enclosing the color wheel.
     */
    private RectF mColorWheelRectangle = new RectF(); //원이 그려질 사각형의 크기


    /**
     * {@code true} if the user clicked on the pointer to start the move mode.
     * {@code false} once the user stops touching the screen.
     *
     * @see #onTouchEvent(MotionEvent)
     */
    private boolean mUserIsMovingPointer = false;

    /**
     * Number of pixels the origin of this view is moved in X- and Y-direction.
     * <p>
     * <p>
     * We use the center of this (quadratic) View as origin of our internal
     * coordinate system. Android uses the upper left corner as origin for the
     * View-specific coordinate system. So this is the value we use to translate
     * from one coordinate system to the other.
     * </p>
     * <p>
     * <p>
     * Note: (Re)calculated in {@link #onMeasure(int, int)}.
     * </p>
     *
     * @see #onDraw(Canvas)
     */
    private float mTranslationOffset;

    /**
     * Radius of the color wheel in pixels.
     * <p>
     * <p>
     * Note: (Re)calculated in {@link #onMeasure(int, int)}.
     * </p>
     */
    private float mColorWheelRadius;

    /**
     * The pointer's position expressed as angle (in rad).
     */
    private float angle;
    private Paint textPaint;
    private String text;
    private int maximumTime = 60 * 60;
    private Timer timer;
    private int currentTime = 0;
    private int timerTime = 0;

    private Typeface typeface; //폰트

    private int wheelBackgroundColor, wheelActiveColor, outLineColor, textColor;
    private int initPosition = -1;
    private boolean blockEnd = false;


    private float lastX;

    private int lastRadians = 0;
    private boolean blockStart = false;
    private boolean reset = true;
    private int arcFinishRadians = 360; //360도까지 돌아갈 수 있다.
    private int startArc = 0;
    private int endWheel;

    private boolean showText = true;
    private boolean counterClockwise = true; //시계방향으로
    private boolean showOutLine = true;
    private boolean clickable = true;

    private Rect textBounds = new Rect();
    private RectF contentBounds = new RectF();

    public CircleTimer(Context context) {
        super(context);
        init(null, 0);
    }

    public CircleTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircleTimer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void start() {
        isRunning = true;
        if (reset) {
            timerTime = currentTime;
            reset = false;
        }
        timer = new Timer();
        timer.schedule(new SecondTimer(currentTime), 0, 1000);
    }

    public void reset() {
        isRunning = false;
        reset = true;
        currentTime = timerTime;
        setValue(currentTime);
        setTextFromAngle(currentTime);

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public void stop() {

        isRunning = false;
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }


    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CircleTimer, defStyle, 0);

        //초기값 셋팅
        initAttributes(a);

        a.recycle();

        wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wheelPaint.setColor(wheelBackgroundColor);
        wheelPaint.setStyle(Style.STROKE);

        outLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outLinePaint.setColor(outLineColor);
        outLinePaint.setStrokeWidth(mPointerRadius + 10);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setColor(textColor);
        textPaint.setStyle(Style.FILL_AND_STROKE);
        textPaint.setTextAlign(Align.LEFT);


        highlightWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightWheelPaint.setStyle(Style.STROKE);
        highlightWheelPaint.setColor(wheelActiveColor);

        arcFinishRadians = (int) calculateAngleFromText(initPosition) - 90;

        angle = calculateAngleFromRadians(Math.min(arcFinishRadians, endWheel));
        setTextFromAngle(initPosition);

        invalidate();
    }

    public void setTextFont(Typeface textFont) {
        typeface = textFont;
        textPaint.setTypeface(typeface);
    }

    public Typeface getTextFont() {
        return typeface;
    }

    private void setTextFromAngle(int angleValue) { //화면 중앙의 Text 값을 표시하는 곳

        currentTime = angleValue;
        if (currentTime < 0) {
            currentTime = 0;
        }
        int temp = angleValue / 60;
        String minute = temp >= 10 ? String.valueOf(temp) : "0" + temp;

        temp = angleValue % (60);
        String second = temp >= 10 ? String.valueOf(temp) : "0" + temp;
        this.text = minute + ":" + second;
    }

    private void initAttributes(TypedArray a) {

        mPointerRadius = a.getDimension(
                R.styleable.CircleTimer_pointer_size, POINTER_RADIUS_DEF_VALUE);
        maximumTime = a.getInteger(R.styleable.CircleTimer_timerMaxValue, MAX_POINT_DEF_VALUE);

        String wheelBackgroundColorAttr = a
                .getString(R.styleable.CircleTimer_wheel_background_color);
        String wheelActiveColorAttr = a
                .getString(R.styleable.CircleTimer_wheel_active_color);
        String outLineColorAttr = a
                .getString(R.styleable.CircleTimer_outline_color);

        String text_color_attr = a.getString(R.styleable.CircleTimer_text_color);

        initPosition = a.getInteger(R.styleable.CircleTimer_init_position, 0);

        startArc = a.getInteger(R.styleable.CircleTimer_start_angle, START_ANGLE_DEF_VALUE);
        endWheel = a.getInteger(R.styleable.CircleTimer_end_angle, END_WHEEL_DEFAULT_VALUE);

        showText = a.getBoolean(R.styleable.CircleTimer_show_text, true);
        clickable = a.getBoolean(R.styleable.CircleTimer_isClick, true);
        counterClockwise = a.getBoolean(R.styleable.CircleTimer_counterClockWise, true);
        showOutLine = a.getBoolean(R.styleable.CircleTimer_isOutline, true);
        lastRadians = endWheel;

        if (initPosition > maximumTime) {
            initPosition = maximumTime;
        }
        if (wheelBackgroundColorAttr != null) { // 기본 배경 Wheel 색상
            try {
                wheelBackgroundColor = Color.parseColor(wheelBackgroundColorAttr);
            } catch (IllegalArgumentException e) {
                wheelBackgroundColor = Color.parseColor("#ff6c87");
            }

        } else {
            wheelBackgroundColor = Color.parseColor("#ff6c87");
        }

        if (wheelActiveColorAttr != null) { // 시간을 의미하는 Wheel 색상
            try {
                wheelActiveColor = Color.parseColor(wheelActiveColorAttr);
            } catch (IllegalArgumentException e) {
                wheelActiveColor = Color.parseColor("#ff6c87");
            }

        } else {
            wheelActiveColor = Color.parseColor("#ff6c87");
        }

        if (outLineColorAttr != null) {
            try {
                outLineColor = Color.parseColor(outLineColorAttr);

            } catch (IllegalArgumentException e) {
                outLineColor = Color.parseColor("#ff6c87");
            }

        } else {
            outLineColor = Color.parseColor("#ff6c87");
        }

        if (outLineColorAttr != null) {
            try {
                outLineColor = Color.parseColor(outLineColorAttr);

            } catch (IllegalArgumentException e) {
                outLineColor = Color.parseColor("#ff6c87");
            }

        } else {
            outLineColor = Color.parseColor("#ff6c87");
        }

        if (text_color_attr != null) {
            try {
                textColor = Color.parseColor(text_color_attr);
            } catch (IllegalArgumentException e) {
                textColor = Color.BLACK;
            }
        } else {
            textColor = Color.BLACK;
        }

    }  // 초기 Timer 설정

    @Override
    protected void onDraw(Canvas canvas) {
        // All of our positions are using our internal coordinate system.
        // Instead of translating
        // them we let Canvas do the work for us.

        canvas.save();
        canvas.translate(mTranslationOffset, mTranslationOffset);
        if (showOutLine) {
            // 외곽 그리기 시작
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            contentBounds.set(mColorWheelRectangle.right + mColorWheelStrokeWidth - 20, 3, mColorWheelRectangle.right + mColorWheelStrokeWidth, -3);

            canvas.drawRoundRect(contentBounds, 5, 5, outLinePaint);  //rx와 ry는 둥근 사각형의 모서리 정도
            for (int i = 5; i < 360; i += 5) {
                if (i % 6 == 0) {
                    contentBounds.bottom = -3;
                    contentBounds.top = 3;
                    contentBounds.right = mColorWheelRectangle.right + mColorWheelStrokeWidth;
                } else {
                    contentBounds.right = mColorWheelRectangle.right + mColorWheelStrokeWidth;
                    contentBounds.top = 1;
                    contentBounds.bottom = -1;
                }
                canvas.rotate(5, mColorWheelRectangle.centerX(), mColorWheelRectangle.centerY());

                canvas.drawRoundRect(contentBounds, 5, 5, outLinePaint);
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            //외곽 그리기 종료
        }

        canvas.restore();
        canvas.translate(mTranslationOffset, mTranslationOffset);

        // Timer Base Line Circle .. 뒷배경 원
        canvas.drawArc(mColorWheelRectangle, startArc + 270, endWheel //270을 해야 정확한 지점에 그려짐
                - (startArc), false, wheelPaint);

        // Text 값과 동일한 원
        int temp = counterClockwise ? 1 : -1;
        canvas.drawArc(mColorWheelRectangle, startArc + 270,
                temp * ((arcFinishRadians) > (endWheel) ? endWheel - (startArc) : arcFinishRadians - startArc), false, highlightWheelPaint);
  

        textPaint.getTextBounds(text, 0, text.length(), textBounds);
   
        if (showText)
            canvas.drawText(
                    text,
                    (mColorWheelRectangle.centerX())
                            - (textPaint.measureText(text) / 2),
                    mColorWheelRectangle.centerY() + (float) textBounds.height() / 2,
                    textPaint);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        //width와 height 파라미터 모두 xml에 정의된 뷰의 크기로 margin,padding이 모두 합친 값이 넘어온다.

        int height = getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        setMeasuredDimension(width, height);


        mTranslationOffset = height * 0.5f; // 캔버스 중심으로

        //확인을 위해
        mColorWheelStrokeWidth = (int) (mTranslationOffset / 3);


        //레이아웃 크기별 화면면
        ////////////////////////////////////////////////////////////
        wheelPaint.setStrokeWidth(mColorWheelStrokeWidth);
        highlightWheelPaint.setStrokeWidth(mColorWheelStrokeWidth);
        textPaint.setTextSize(mTranslationOffset / 4);


        ////////////////////////////////////////////////////////////

        mColorWheelRadius = mTranslationOffset - mColorWheelStrokeWidth - 10; // highlight color circle에 비례해서 canvas 크기를 설정


        //원을 실질적으로 그리는 사각형의 크기
        mColorWheelRectangle.set(-mColorWheelRadius, -mColorWheelRadius,
                mColorWheelRadius, mColorWheelRadius);

    } // 해당 뷰의 크기에 맞게 Canvas 크기를 재정의 하는 곳

    private int calculateValueFromAngle(float angle) { //앵글에 해당하는 값을 반환

        float m = angle - startArc - 1;


        float f = (endWheel - startArc) / m;

        return (int) (maximumTime / f);
    }

    private int calculateTextFromStartAngle(float angle) {
        float f = (endWheel - startArc) / angle;

        return (int) (maximumTime / f);
    }

    private double calculateAngleFromText(int position) {
        if (position == 0 || position >= maximumTime)
            return (float) 90;

        double f = (double) maximumTime / (double) position;

        double f_r = 360 / f;

        return f_r + 90;
    }

    private int calculateRadiansFromAngle(float angle) {
        float unit = (float) (angle / (2 * Math.PI));
        if (unit < 0) {
            unit += 1;
        }
        int radians = (int) ((unit * 360) - ((360 / 4) * 3));
        if (radians < 0)
            radians += 360;


        if (!counterClockwise)
            return 360 - radians;
        else
            return radians;

    }

    private float calculateAngleFromRadians(int radians) {  //??
        return (float) (((radians + 270) * (2 * Math.PI)) / 360);
    }

    /**
     * Get the selected value
     *
     * @return the value between 0 and max
     */
    public int getValue() {
        return currentTime;
    }

    public void setMaximumTime(int maximumTime) {
        this.maximumTime = maximumTime;
        setTextFromAngle(calculateValueFromAngle(arcFinishRadians));
        invalidate();
    }


    public void setValue(float newValue) {  // value 값으로 Timer 셋팅
        if (newValue == 0) {
            arcFinishRadians = startArc; //0
        } else if (newValue == this.maximumTime) {
            arcFinishRadians = endWheel; //360
        } else {
            float newAngle = (float) (360.0 * (newValue / maximumTime));
            arcFinishRadians = (int) calculateAngleFromRadians(calculateRadiansFromAngle(newAngle)) + 1;
        }


        angle = calculateAngleFromRadians(arcFinishRadians);
        setTextFromAngle((int) newValue);

        //화면 다시 갱신
        invalidate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Convert coordinates to our internal coordinate system

        if (!showText || !clickable || isRunning) return false;

        float x = event.getX() - mTranslationOffset;
        float y = event.getY() - mTranslationOffset;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                if (mColorWheelRectangle.contains(x, y)) {
                    // Check whether the user pressed on (or near) the pointer
                    angle = (float) Math.atan2(y, x); // 두 지점의 각도를 반환한다.

                    blockEnd = false;
                    blockStart = false;
                    mUserIsMovingPointer = true;

                    arcFinishRadians = calculateRadiansFromAngle(angle);

                    if (arcFinishRadians > endWheel) {
                        arcFinishRadians = endWheel;
                        blockEnd = true;
                    }

                    if (!blockEnd) {
                        setTextFromAngle(calculateValueFromAngle(arcFinishRadians));
                        invalidate();
                    }
                    if (mOnCircleSeekBarChangeListener != null) {
                        mOnCircleSeekBarChangeListener.onStartTrackingTouch(this);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mUserIsMovingPointer) {
                    angle = (float) Math.atan2(y, x);

                    int radians = calculateRadiansFromAngle(angle);

                    if (lastRadians > radians && radians < (360 / 6) && x > lastX
                            && lastRadians > (360 / 6)) {

                        if (!blockEnd && !blockStart)
                            blockEnd = true;
                        // if (block_start)
                        // block_start = false;
                    } else if (lastRadians >= startArc
                            && lastRadians <= (360 / 4) && radians <= (360 - 1)
                            && radians >= ((360 / 4) * 3) && x < lastX) {
                        if (!blockStart && !blockEnd)
                            blockStart = true;
                        // if (block_end)
                        // block_end = false;

                    } else if (radians >= endWheel && !blockStart
                            && lastRadians < radians) {
                        blockEnd = true;
                    } else if (radians < endWheel && blockEnd
                            && lastRadians > endWheel) {
                        blockEnd = false;
                    } else if (radians < startArc && lastRadians > radians
                            && !blockEnd) {
                        blockStart = true;
                    } else if (blockStart && lastRadians < radians
                            && radians > startArc && radians < endWheel) {
                        blockStart = false;
                    }

                    if (blockEnd) {
                        arcFinishRadians = endWheel - 1;
                        setTextFromAngle(maximumTime);
                        angle = calculateAngleFromRadians(arcFinishRadians);
                    } else if (blockStart) {
                        arcFinishRadians = startArc;
                        angle = calculateAngleFromRadians(arcFinishRadians);
                        setTextFromAngle(0);
                    } else {
                        arcFinishRadians = calculateRadiansFromAngle(angle);
                        setTextFromAngle(calculateValueFromAngle(arcFinishRadians));
                    }
                    invalidate();

                    //클릭 이벤트 발생
                    if (mOnCircleSeekBarChangeListener != null)
                        mOnCircleSeekBarChangeListener.onProgressChanged(this,
                                Integer.parseInt(text), true);

                    lastRadians = radians;

                }
                break;
            case MotionEvent.ACTION_UP: //클릭을 끝냈을 때
                mUserIsMovingPointer = false;
                if (mOnCircleSeekBarChangeListener != null) {
                    mOnCircleSeekBarChangeListener.onStopTrackingTouch(this);
                }
                break;
        }
        // Fix scrolling
        if (event.getAction() == MotionEvent.ACTION_MOVE && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        lastX = x;

        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable(STATE_PARENT, superState);
        state.putFloat(STATE_ANGLE, angle);

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;

        Parcelable superState = savedState.getParcelable(STATE_PARENT);
        super.onRestoreInstanceState(superState);

        angle = savedState.getFloat(STATE_ANGLE);
        arcFinishRadians = calculateRadiansFromAngle(angle);
        setTextFromAngle(calculateValueFromAngle(arcFinishRadians));
    }


    public void setInitPosition(int newValue) {  // value 값으로 Timer 셋팅
        if (timer != null)
            reset();
        if (newValue == 0) {
            arcFinishRadians = startArc; //0
        } else if (newValue == this.maximumTime) {
            arcFinishRadians = endWheel; //360
        } else {
            float newAngle = (float) (360.0 * ((float) newValue / maximumTime));
            arcFinishRadians = (int) calculateAngleFromRadians(calculateRadiansFromAngle(newAngle)) + 1;
        }


        timerTime = newValue;
        angle = calculateAngleFromRadians(arcFinishRadians);
        setTextFromAngle(newValue);

        //화면 다시 갱신
        invalidate();
    }

    public void setOnSeekBarChangeListener(OnCircleSeekBarChangeListener l) {
        mOnCircleSeekBarChangeListener = l;
    }

    public int getMaxValue() {
        return maximumTime;
    } //현재 Timer의 최대값을 반환하는 함수 return max

    public interface OnCircleSeekBarChangeListener {

        void onProgressChanged(CircleTimer seekBar, int progress, boolean fromUser);

        void onStartTrackingTouch(CircleTimer seekBar);

        void onStopTrackingTouch(CircleTimer seekBar);

    }

    class SecondTimer extends TimerTask {

        int value;

        public SecondTimer() {
            this.value = 0;
        }

        public SecondTimer(int value) {
            this.value = value;
        }

        @Override
        public void run() {
            if (value > 0) {

                value -= 1;
                setValue(value);
                setTextFromAngle(value);

            } else {
                CircleTimer.this.post(new Runnable() {
                    @Override
                    public void run() {
                        if (baseTimerEndedListener != null) baseTimerEndedListener.OnEnded();
                    }
                });

                this.cancel();
            }
        }
    }

    public interface baseTimerEndedListener {
        public void OnEnded();
    }

    public void setBaseTimerEndedListener(baseTimerEndedListener listener) {
        this.baseTimerEndedListener = listener;
    }
}