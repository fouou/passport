package com.fou.passport.ui.settingfragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 自定义色盘：外圈选色相，内方块选饱和度与明度。
 * 选出的颜色作为种子色交给 Material 动态取色生成整套色板。
 */
public class ColorWheelView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private static final float HUE_STEPS = 361f;
    private static final int MARKER_SIZE_DP = 7;
    private static final int RING_GAP_DP = 6;

    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF squareRect = new RectF();
    private final float density;

    private float centerX;
    private float centerY;
    private float outerRadius;
    private float ringWidth;
    private float hue = 0f;
    private float saturation = 0.9f;
    private float value = 0.9f;
    private OnColorChangedListener listener;

    public ColorWheelView(Context context) {
        this(context, null);
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(Color.WHITE);
        markerStrokePaint.setStyle(Paint.Style.STROKE);
        markerStrokePaint.setStrokeWidth(dp(1.2f));
        markerStrokePaint.setColor(0x66000000);
        markerPaint.setShadowLayer(dp(2), 0, dp(1), 0x55000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        requestLayout();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        outerRadius = Math.min(w, h) / 2f - dp(4);
        ringWidth = outerRadius * 0.24f;
        float innerRadius = outerRadius - ringWidth - dp(RING_GAP_DP);
        float side = innerRadius * 1.25f;
        squareRect.set(centerX - side / 2, centerY - side / 2, centerX + side / 2, centerY + side / 2);
        buildHueShader();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, outerRadius - ringWidth / 2, huePaint);
        rebuildSaturationShader();
        canvas.drawRect(squareRect, saturationPaint);
        valuePaint.setShader(new LinearGradient(0, squareRect.top, 0, squareRect.bottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRect(squareRect, valuePaint);

        float angle = (float) Math.toRadians(hue);
        float markerRadius = outerRadius - ringWidth / 2;
        float markerX = centerX + (float) Math.cos(angle) * markerRadius;
        float markerY = centerY + (float) Math.sin(angle) * markerRadius;
        canvas.drawCircle(markerX, markerY, dp(MARKER_SIZE_DP), markerPaint);
        canvas.drawCircle(markerX, markerY, dp(MARKER_SIZE_DP), markerStrokePaint);

        float touchX = squareRect.left + saturation * squareRect.width();
        float touchY = squareRect.bottom - value * squareRect.height();
        canvas.drawCircle(touchX, touchY, dp(MARKER_SIZE_DP), markerPaint);
        canvas.drawCircle(touchX, touchY, dp(MARKER_SIZE_DP), markerStrokePaint);
    }

    private void buildHueShader() {
        int[] colors = new int[(int) HUE_STEPS];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }
        float[] positions = new float[colors.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = i / (HUE_STEPS - 1f);
        }
        huePaint.setStyle(Paint.Style.STROKE);
        huePaint.setStrokeWidth(ringWidth);
        huePaint.setShader(new SweepGradient(centerX, centerY, colors, positions));
    }

    private void rebuildSaturationShader() {
        int pureHue = Color.HSVToColor(new float[]{hue, 1f, 1f});
        saturationPaint.setShader(new LinearGradient(squareRect.left, 0, squareRect.right, 0,
                Color.WHITE, pureHue, Shader.TileMode.CLAMP));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateFromTouch(event.getX(), event.getY());
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateFromTouch(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.hypot(dx, dy);
        if (squareRect.contains(x, y)) {
            saturation = Math.max(0f, Math.min(1f, (x - squareRect.left) / squareRect.width()));
            value = Math.max(0f, Math.min(1f,
                    1f - (y - squareRect.top) / squareRect.height()));
        } else if (distance <= outerRadius + dp(2) && distance >= outerRadius - ringWidth - dp(2)) {
            hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360f) % 360f);
        } else {
            return;
        }
        invalidate();
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
    }

    private float dp(float value) {
        return value * density;
    }
}
