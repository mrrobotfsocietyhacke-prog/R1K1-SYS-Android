package com.monobogdan.simpletelegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** Общие цвета и мелкие помощники оформления (красно-фиолетовый стиль R1K1.SYS). */
public final class Ui {
    public static final int BG = Color.rgb(8, 8, 12);
    public static final int CARD = Color.rgb(18, 18, 26);
    public static final int CARD_LIGHT = Color.rgb(28, 24, 40);
    public static final int RED = Color.rgb(255, 35, 65);
    public static final int VIOLET = Color.rgb(176, 38, 255);
    public static final int ACTIVE = Color.rgb(196, 60, 240);
    public static final int TEXT = Color.rgb(242, 242, 247);
    public static final int MUTED = Color.rgb(138, 122, 153);
    public static final int LINE = Color.rgb(59, 29, 82);
    public static final int BLUE = Color.rgb(64, 160, 255);
    public static final int GOLD1 = Color.rgb(200, 128, 10);
    public static final int GOLD2 = Color.rgb(245, 176, 20);

    private Ui() { }

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable gradient(int c1, int c2, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {c1, c2});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable circle(int c1, int c2) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {c1, c2});
        d.setShape(GradientDrawable.OVAL);
        return d;
    }

    @SuppressWarnings("deprecation")
    public static void setBg(View v, Drawable d) { v.setBackgroundDrawable(d); }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static TextView pill(Context c, String value, int bg, int fg, float sp) {
        TextView t = text(c, value, sp, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        setBg(t, round(bg, 20, c));
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    public static LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, Context c, float l, float t, float r, float b) {
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    public static String str(JSONObject o, String key, String def) {
        if (o == null || !o.has(key) || o.isNull(key)) return def;
        String v = String.valueOf(o.opt(key)).trim();
        return v.length() == 0 ? def : v;
    }

    /** Цвет плитки по редкости. */
    public static int[] rarityColors(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        if (r.contains("ultra") || r.contains("ультра") || r.contains("mega") || r.contains("мега"))
            return new int[] {Color.rgb(255, 35, 120), Color.rgb(140, 30, 255)};
        if (r.contains("legend") || r.contains("легенд")) return new int[] {Color.rgb(245, 176, 20), Color.rgb(200, 90, 10)};
        if (r.contains("epic") || r.contains("эпич")) return new int[] {Color.rgb(176, 38, 255), Color.rgb(90, 30, 200)};
        if (r.contains("rare") || r.contains("редк")) return new int[] {Color.rgb(64, 140, 255), Color.rgb(40, 70, 200)};
        return new int[] {Color.rgb(255, 80, 50), Color.rgb(220, 45, 40)};
    }
}
