package com.monobogdan.simpletelegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Иконки нижней панели, нарисованные кодом (24x24 условных единицы). */
public class NavIcon extends View {
    public static final int CHAT = 0, GIFT = 1, SHOP = 2, PROFILE = 3;
    private final int type;
    private boolean active;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public NavIcon(Context c, int type) {
        super(c);
        this.type = type;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setActive(boolean a) { active = a; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        canvas.save();
        canvas.translate((getWidth() - 24 * s) / 2f, (getHeight() - 24 * s) / 2f);
        canvas.scale(s, s);
        paint.setColor(active ? Ui.ACTIVE : Ui.MUTED);
        paint.setStrokeWidth(active ? 2.2f : 1.9f);
        if (type == CHAT) {
            canvas.drawRoundRect(new RectF(3.5f, 4.5f, 20.5f, 16.5f), 4f, 4f, paint);
            Path tail = new Path();
            tail.moveTo(8f, 16.5f); tail.lineTo(7f, 20.5f); tail.lineTo(12.5f, 16.5f);
            canvas.drawPath(tail, paint);
        } else if (type == GIFT) {
            canvas.drawRect(4f, 10f, 20f, 20.5f, paint);
            canvas.drawRect(3f, 7f, 21f, 10f, paint);
            canvas.drawLine(12f, 7f, 12f, 20.5f, paint);
            canvas.drawArc(new RectF(6.5f, 2.5f, 12f, 7f), 90, 270, false, paint);
            canvas.drawArc(new RectF(12f, 2.5f, 17.5f, 7f), 180, 270, false, paint);
        } else if (type == SHOP) {
            canvas.drawRoundRect(new RectF(4.5f, 8f, 19.5f, 20.5f), 2.5f, 2.5f, paint);
            canvas.drawArc(new RectF(8f, 3f, 16f, 13f), 180, 180, false, paint);
        } else {
            canvas.drawCircle(12f, 8.5f, 3.8f, paint);
            canvas.drawArc(new RectF(4.5f, 14f, 19.5f, 27f), 180, 180, false, paint);
        }
        canvas.restore();
    }
}
