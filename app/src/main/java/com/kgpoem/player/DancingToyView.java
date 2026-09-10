package com.kgpoem.player;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DancingToyView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float sway = 0f;

    public DancingToyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setDancing(boolean dancing) {
        if (dancing) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(-1f, 1f);
                animator.setDuration(520L);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.addUpdateListener(valueAnimator -> {
                    sway = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                });
            }
            if (!animator.isStarted()) {
                animator.start();
            }
        } else {
            if (animator != null) {
                animator.cancel();
            }
            sway = 0f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        canvas.save();
        // Dynamic bouncy rotation & translation
        canvas.rotate(sway * 12f, cx, cy);
        canvas.translate(0f, -Math.abs(sway) * 4f);

        paint.setStyle(Paint.Style.FILL);

        // Music notes floating when dancing
        if (animator != null && animator.isRunning()) {
            paint.setColor(Color.rgb(255, 209, 102));
            float noteY = cy - 26f - (Math.abs(sway) * 8f);
            canvas.drawCircle(cx - 24f, noteY, 3.5f, paint);
            canvas.drawRect(cx - 22f, noteY - 10f, cx - 20f, noteY, paint);

            paint.setColor(Color.rgb(255, 107, 139));
            float note2Y = cy - 24f - ((1f - Math.abs(sway)) * 8f);
            canvas.drawCircle(cx + 24f, note2Y, 3.5f, paint);
            canvas.drawRect(cx + 24f, note2Y - 10f, cx + 26f, note2Y, paint);
        }

        // Head and ears
        paint.setColor(Color.rgb(255, 218, 120));
        canvas.drawCircle(cx, cy - 6f, 22f, paint);
        paint.setColor(Color.rgb(195, 128, 75));
        canvas.drawCircle(cx - 18f, cy - 24f, 9f, paint);
        canvas.drawCircle(cx + 18f, cy - 24f, 9f, paint);

        // Ear insides
        paint.setColor(Color.rgb(255, 175, 190));
        canvas.drawCircle(cx - 18f, cy - 24f, 5f, paint);
        canvas.drawCircle(cx + 18f, cy - 24f, 5f, paint);

        // Headphones headband
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(108, 92, 231));
        canvas.drawArc(cx - 23f, cy - 30f, cx + 23f, cy + 2f, 180f, 180f, false, paint);
        paint.setStyle(Paint.Style.FILL);

        // Headphones ear cushions
        paint.setColor(Color.rgb(255, 107, 139));
        canvas.drawRoundRect(cx - 26f, cy - 14f, cx - 18f, cy + 2f, 4f, 4f, paint);
        canvas.drawRoundRect(cx + 18f, cy - 14f, cx + 26f, cy + 2f, 4f, 4f, paint);

        // Eyes and highlights
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - 8f, cy - 9f, 5.5f, paint);
        canvas.drawCircle(cx + 8f, cy - 9f, 5.5f, paint);
        paint.setColor(Color.rgb(45, 52, 54));
        canvas.drawCircle(cx - 8f, cy - 9f, 3f, paint);
        canvas.drawCircle(cx + 8f, cy - 9f, 3f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - 9f, cy - 10f, 1.2f, paint);
        canvas.drawCircle(cx + 7f, cy - 10f, 1.2f, paint);

        // Rosy cheeks
        paint.setColor(Color.argb(120, 255, 107, 139));
        canvas.drawCircle(cx - 14f, cy - 4f, 4f, paint);
        canvas.drawCircle(cx + 14f, cy - 4f, 4f, paint);

        // Cute snout and nose
        paint.setColor(Color.rgb(255, 245, 220));
        canvas.drawOval(cx - 10f, cy - 4f, cx + 10f, cy + 8f, paint);
        paint.setColor(Color.rgb(45, 52, 54));
        canvas.drawCircle(cx, cy - 1f, 3f, paint);

        // Smiling mouth
        paint.setColor(Color.rgb(255, 107, 139));
        canvas.drawOval(cx - 5f, cy + 2f, cx + 5f, cy + 7f, paint);

        // Body with cute t-shirt
        paint.setColor(Color.rgb(255, 218, 120));
        canvas.drawRoundRect(cx - 26f, cy + 17f, cx + 26f, cy + 62f, 16f, 16f, paint);
        paint.setColor(Color.rgb(78, 205, 196));
        canvas.drawRoundRect(cx - 25f, cy + 20f, cx + 25f, cy + 46f, 12f, 12f, paint);

        // Arms waving
        paint.setColor(Color.rgb(195, 128, 75));
        canvas.drawOval(cx - 40f, cy + 18f, cx - 18f, cy + 32f, paint);
        canvas.drawOval(cx + 18f, cy + 18f, cx + 40f, cy + 32f, paint);

        // Feet
        paint.setColor(Color.rgb(195, 128, 75));
        canvas.drawRoundRect(cx - 24f, cy + 56f, cx - 6f, cy + 68f, 7f, 7f, paint);
        canvas.drawRoundRect(cx + 6f, cy + 56f, cx + 24f, cy + 68f, 7f, 7f, paint);

        canvas.restore();
    }
}
