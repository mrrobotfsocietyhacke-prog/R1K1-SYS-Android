package com.monobogdan.simpletelegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.simpletelegram.transport.ClientManager;

import org.json.JSONArray;
import org.json.JSONObject;

/** Магазин: баннер трейдинга, вкладки «Предметы» и «NFT-ники», карточки вместо кнопок с GET-запросами. */
public class ShopView {
    private final Activity act;
    private final LinearLayout list;
    private TextView tabItems, tabNames;

    private ShopView(Activity a, LinearLayout list) { this.act = a; this.list = list; }

    public static View build(final Activity act, final Runnable openTrading) {
        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(act, 14), Ui.dp(act, 12), Ui.dp(act, 14), Ui.dp(act, 24));
        scroll.addView(root);

        LinearLayout banner = new LinearLayout(act);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(Ui.dp(act, 18), Ui.dp(act, 18), Ui.dp(act, 18), Ui.dp(act, 18));
        Ui.setBg(banner, Ui.gradient(Ui.RED, Ui.VIOLET, 20, act));
        banner.addView(Ui.text(act, "R1K1 Market", 20, Color.WHITE, true));
        banner.addView(Ui.text(act, "Трейдинг: покупай и продавай NFT и подарки", 13, Color.argb(220, 255, 255, 255), false));
        banner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openTrading.run(); }
        });
        root.addView(banner, Ui.lp(-1, -2));

        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        final ShopView sv = new ShopView(act, list);
        LinearLayout tabs = new LinearLayout(act);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, Ui.dp(act, 16), 0, Ui.dp(act, 8));
        sv.tabItems = sv.tab("Предметы", 0);
        sv.tabNames = sv.tab("NFT-ники", 1);
        tabs.addView(sv.tabItems, Ui.lp(0, -2, 1));
        tabs.addView(sv.tabNames, Ui.lp(0, -2, 1));
        root.addView(tabs, Ui.lp(-1, -2));
        root.addView(list, Ui.lp(-1, -2));
        sv.show(0);
        return scroll;
    }

    private TextView tab(String label, final int which) {
        TextView t = Ui.text(act, label, 15, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 10));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(which); }
        });
        return t;
    }

    private void note(String msg) {
        list.removeAllViews();
        TextView t = Ui.text(act, msg, 14, Ui.MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(act, 20), Ui.dp(act, 30), Ui.dp(act, 20), 0);
        list.addView(t, Ui.lp(-1, -2));
    }

    private void show(final int which) {
        Ui.setBg(tabItems, which == 0 ? Ui.round(Ui.CARD_LIGHT, 20, act) : Ui.round(Color.TRANSPARENT, 20, act));
        Ui.setBg(tabNames, which == 1 ? Ui.round(Ui.CARD_LIGHT, 20, act) : Ui.round(Color.TRANSPARENT, 20, act));
        tabItems.setTextColor(which == 0 ? Ui.TEXT : Ui.MUTED);
        tabNames.setTextColor(which == 1 ? Ui.TEXT : Ui.MUTED);
        note("Загрузка…");
        ClientManager.getCurrent().apiGetRaw(which == 0 ? "/shop" : "/usernames/shop", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() { fill(which, code, body); }
                });
            }
        });
    }

    private JSONArray items(String body) {
        try {
            String t = body.trim();
            if (t.startsWith("[")) return new JSONArray(t);
            JSONObject o = new JSONObject(t);
            JSONArray a = o.optJSONArray("items");
            if (a == null) a = o.optJSONArray("usernames");
            if (a == null) a = o.optJSONArray("shop");
            return a;
        } catch (Exception e) { return null; }
    }

    private void fill(int which, int code, String body) {
        if (code == 404 || code == 0) { note("Этот раздел появится, когда сервер подключит магазин."); return; }
        if (code != 200) { note(ClientManager.errorText(code, body)); return; }
        JSONArray arr = items(body);
        if (arr == null || arr.length() == 0) { note("Пока ничего нет."); return; }
        list.removeAllViews();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                String name = arr.optString(i, "");
                if (name.length() == 0) continue;
                try { o = new JSONObject().put("name", name); } catch (Exception e) { continue; }
            }
            list.addView(card(which, o), Ui.margins(Ui.lp(-1, -2), act, 0, 5, 0, 5));
        }
    }

    private View card(final int which, final JSONObject o) {
        final String name = Ui.str(o, "username", Ui.str(o, "name", Ui.str(o, "title", "Предмет")));
        int[] col = Ui.rarityColors(Ui.str(o, "rarity", ""));
        LinearLayout r = new LinearLayout(act);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(act, 12), Ui.dp(act, 12), Ui.dp(act, 12), Ui.dp(act, 12));
        Ui.setBg(r, Ui.round(Ui.CARD, 16, act));

        TextView tile = Ui.text(act, which == 1 ? "@" : name.substring(0, 1).toUpperCase(), 24, Color.WHITE, true);
        tile.setGravity(Gravity.CENTER);
        Ui.setBg(tile, Ui.gradient(col[0], col[1], 14, act));
        r.addView(tile, Ui.margins(Ui.lp(Ui.dp(act, 56), Ui.dp(act, 56)), act, 0, 0, 12, 0));

        LinearLayout mid = new LinearLayout(act);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(Ui.text(act, which == 1 ? "@" + name.replace("@", "") : name, 16, Ui.TEXT, true));
        String rarity = Ui.str(o, "rarity", "");
        if (rarity.length() > 0) mid.addView(Ui.text(act, rarity, 12, Ui.MUTED, false));
        r.addView(mid, Ui.lp(0, -2, 1));

        String price = Ui.str(o, "price", "");
        if (price.length() > 0) {
            String cur = Ui.str(o, "currency", "Bytes");
            r.addView(Ui.pill(act, price + " " + cur, Ui.CARD_LIGHT, Ui.GOLD2, 13), Ui.lp(-2, -2));
        }
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { open(which, name); }
        });
        return r;
    }

    private void open(int which, final String name) {
        if (which == 1) {
            final String clean = name.replace("@", "");
            new AlertDialog.Builder(act).setTitle("@" + clean).setMessage("Купить этот username?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Купить", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            String body = new JSONObject().put("username", clean).toString();
                            ClientManager.getCurrent().apiPost("/usernames/buy", body, new ClientManager.Response() {
                                @Override public void onReady(String s) {
                                    act.runOnUiThread(new Runnable() {
                                        @Override public void run() { Toast.makeText(act, "Куплено", Toast.LENGTH_SHORT).show(); }
                                    });
                                }
                            });
                        } catch (Exception ignored) { }
                    }
                }).show();
        } else {
            Toast.makeText(act, "Покупка предметов подключится вместе с сервером", Toast.LENGTH_SHORT).show();
        }
    }
}
