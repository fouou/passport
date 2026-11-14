package com.fou.passport.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * 3x3 图案解锁控件：按下拖动连接圆点，抬手后回调图案。
 */
public class PatternLockView extends View {

    public interface OnPatternListener {
        void onPatternDetected(List<Integer> dots, String patternString);
    }

    public static final int MIN_DOTS = 4;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PointF[] dotCenters = new PointF[9];
    private final List<Integer> selectedDots = new ArrayList<>();

    private float dotRadius;
    private float hitRadius;
    private float fingerX;
    private float fingerY;
    private boolean drawing;
    private boolean errorState;
    private OnPatternListener listener;
    private int colorNormal;
    private int colorActive;
    private int colorError;

    public PatternLockView(Context context) {
        this(context, null);
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PatternLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        colorNormal = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        colorActive = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary);
        colorError = MaterialColors.getColor(this, com.google.android.material.R.attr.colorError);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < dotCenters.length; i++) {
            dotCenters[i] = new PointF();
        }
        setClickable(true);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = dp(24);
        float size = Math.min(w, h) - padding * 2;
        if (size <= 0) {
            return;
        }
        float cell = size / 3f;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        dotRadius = cell * 0.13f;
        hitRadius = cell * 0.3f;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                dotCenters[row * 3 + col].set(
                        startX + cell * (col + 0.5f),
                        startY + cell * (row + 0.5f));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int activeColor = errorState ? colorError : colorActive;

        //圆点
        for (int i = 0; i < dotCenters.length; i++) {
            boolean selected = selectedDots.contains(i);
            dotPaint.setColor(selected ? activeColor : colorNormal);
            dotPaint.setAlpha(selected ? 255 : 120);
            canvas.drawCircle(dotCenters[i].x, dotCenters[i].y, dotRadius, dotPaint);
            dotPaint.setAlpha(255);
            dotPaint.setColor(selected ? activeColor : colorNormal);
            canvas.drawCircle(dotCenters[i].x, dotCenters[i].y, dotRadius * 0.45f, dotPaint);
        }

        //连接线
        if (!selectedDots.isEmpty()) {
            linePaint.setColor(activeColor);
            for (int i = 0; i < selectedDots.size() - 1; i++) {
                PointF from = dotCenters[selectedDots.get(i)];
                PointF to = dotCenters[selectedDots.get(i + 1)];
                canvas.drawLine(from.x, from.y, to.x, to.y, linePaint);
            }
            if (drawing) {
                PointF last = dotCenters[selectedDots.get(selectedDots.size() - 1)];
                canvas.drawLine(last.x, last.y, fingerX, fingerY, linePaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                selectedDots.clear();
                errorState = false;
                drawing = true;
                fingerX = event.getX();
                fingerY = event.getY();
                trySelectDot(fingerX, fingerY);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                fingerX = event.getX();
                fingerY = event.getY();
                trySelectDot(fingerX, fingerY);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                drawing = false;
                invalidate();
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        && !selectedDots.isEmpty() && listener != null) {
                    listener.onPatternDetected(new ArrayList<>(selectedDots), getPatternString());
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void trySelectDot(float x, float y) {
        for (int i = 0; i < dotCenters.length; i++) {
            if (selectedDots.contains(i)) {
                continue;
            }
            float dx = x - dotCenters[i].x;
            float dy = y - dotCenters[i].y;
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                selectedDots.add(i);
                return;
            }
        }
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    /**
     * 已连接的点数。
     */
    public int getSelectedCount() {
        return selectedDots.size();
    }

    /**
     * 点位序列，如 "0-3-6-7"。
     */
    public String getPatternString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selectedDots.size(); i++) {
            if (i > 0) {
                builder.append('-');
            }
            builder.append(selectedDots.get(i));
        }
        return builder.toString();
    }

    public void clearPattern() {
        selectedDots.clear();
        errorState = false;
        drawing = false;
        invalidate();
    }

    /**
     * 绘制错误后短暂变红，再自动清空。
     */
    public void showError() {
        errorState = true;
        invalidate();
        postDelayed(this::clearPattern, 600);
    }
}
