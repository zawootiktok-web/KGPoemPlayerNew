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
        canvas.rotate(sway * 10f, cx, cy);
        paint.setStyle(Paint.Style.FILL);

        // Head and ears
        paint.setColor(Color.rgb(255, 218, 120));
        canvas.drawCircle(cx, cy - 6f, 22f, paint);
        paint.setColor(Color.rgb(180, 115, 65));
        canvas.drawCircle(cx - 18f, cy - 24f, 9f, paint);
        canvas.drawCircle(cx + 18f, cy - 24f, 9f, paint);

        // Eyes and nose
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - 8f, cy - 10f, 5f, paint);
        canvas.drawCircle(cx + 8f, cy - 10f, 5f, paint);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(cx - 8f, cy - 10f, 2f, paint);
        canvas.drawCircle(cx + 8f, cy - 10f, 2f, paint);
        canvas.drawCircle(cx, cy + 1f, 4f, paint);

        // Mouth: valid drawOval(left, top, right, bottom, paint) overload
        paint.setColor(Color.rgb(255, 143, 171));
        canvas.drawOval(cx - 17f, cy + 11f, cx + 17f, cy + 26f, paint);

        // Body
        paint.setColor(Color.rgb(255, 218, 120));
        canvas.drawRoundRect(cx - 27f, cy + 17f, cx + 27f, cy + 63f, 16f, 16f, paint);

        // Arms
        paint.setColor(Color.rgb(180, 115, 65));
        canvas.drawOval(cx - 42f, cy + 18f, cx - 20f, cy + 34f, paint);
        canvas.drawOval(cx + 20f, cy + 18f, cx + 42f, cy + 34f, paint);

        // Feet: use drawRoundRect rather than the invalid six-argument drawOval overload
        canvas.drawRoundRect(cx - 25f, cy + 56f, cx - 5f, cy + 69f, 8f, 8f, paint);
        canvas.drawRoundRect(cx + 5f, cy + 56f, cx + 25f, cy + 69f, 8f, 8f, paint);

        canvas.restore();
    }
}
