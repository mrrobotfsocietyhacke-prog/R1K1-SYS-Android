package com.monobogdan.simpletelegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.simpletelegram.transport.ClientManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/** Экран «Деньги»: карточки Bytes / Coins, вкладки «Транзакции» и «Коллекции». */
public class WalletView {
    private final Activity act;
    private final JSONObject me;
    private final LinearLayout list;
    private TextView tabTx, tabCol;
    private TextView sendButton;
    private int page = 0;

    private WalletView(Activity a, JSONObject me, LinearLayout list) { this.act = a; this.me = me; this.list = list; }

    private String balance(int idx) { return Ui.str(me, idx == 0 ? "statiki" : "koiny", "0"); }
    private String ownerId() { return Ui.str(me, "r1k1_id", Ui.str(me, "tg_id", "")); }

    public static View build(final Activity act, JSONObject me) {
        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(act, 12), 0, Ui.dp(act, 20));
        scroll.addView(root);
        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        WalletView w = new WalletView(act, me, list);
        w.buildInto(root);
        return scroll;
    }

    private View card(final int idx) {
        int cardW = act.getResources().getDisplayMetrics().widthPixels - Ui.dp(act, 56);
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(Ui.dp(act, 22), Ui.dp(act, 28), Ui.dp(act, 22), Ui.dp(act, 20));
        Ui.setBg(c, idx == 0 ? Ui.gradient(Ui.RED, Ui.VIOLET, 22, act) : Ui.gradient(Ui.GOLD1, Ui.GOLD2, 22, act));
        String unit = idx == 0 ? "BYTES" : "COINS";
        c.addView(Ui.text(act, balance(idx) + " " + unit, 30, Color.WHITE, true));
        TextView num = Ui.text(act, unit + "  " + ownerId(), 13, Color.argb(200, 255, 255, 255), false);
        num.setPadding(0, Ui.dp(act, 4), 0, 0);
        c.addView(num);
        View gap = new View(act);
        c.addView(gap, Ui.lp(1, Ui.dp(act, 46)));
        String name = Ui.str(me, "username", "");
        c.addView(Ui.text(act, (name.length() > 0 ? "@" + name.replace("@", "") : "R1K1") + "  #R1K1", 15, Color.WHITE, true));
        LinearLayout.LayoutParams p = Ui.lp(cardW, Ui.dp(act, 190));
        Ui.margins(p, act, 8, 0, 8, 0);
        c.setLayoutParams(p);
        return c;
    }

    private void buildInto(LinearLayout root) {
        final HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(act);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setPadding(Ui.dp(act, 20), 0, Ui.dp(act, 20), 0);
        cards.addView(card(0));
        cards.addView(card(1));
        hs.addView(cards);
        root.addView(hs, Ui.lp(-1, -2));

        final int step = act.getResources().getDisplayMetrics().widthPixels - Ui.dp(act, 56) + Ui.dp(act, 16);
        final Runnable snap = new Runnable() {
            @Override public void run() {
                int idx = Math.max(0, Math.min(1, Math.round(hs.getScrollX() / (float) step)));
                hs.smoothScrollTo(idx * step, 0);
                setPage(idx);
            }
        };
        hs.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) hs.postDelayed(snap, 140);
                return false;
            }
        });

        sendButton = Ui.text(act, "Отправить Bytes", 16, Color.WHITE, true);
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setPadding(0, Ui.dp(act, 15), 0, Ui.dp(act, 15));
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openTransfer(); }
        });
        root.addView(sendButton, Ui.margins(Ui.lp(-1, -2), act, 18, 16, 18, 0));
        setPage(0);

        LinearLayout tabs = new LinearLayout(act);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(Ui.dp(act, 18), Ui.dp(act, 20), Ui.dp(act, 18), Ui.dp(act, 8));
        tabTx = tab("Транзакции", true);
        tabCol = tab("Коллекции", false);
        tabs.addView(tabTx, Ui.lp(0, -2, 1));
        tabs.addView(tabCol, Ui.lp(0, -2, 1));
        root.addView(tabs, Ui.lp(-1, -2));
        root.addView(list, Ui.lp(-1, -2));
        showTab(0);
    }

    private TextView tab(String label, final boolean first) {
        TextView t = Ui.text(act, label, 15, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 10));
        final int which = first ? 0 : 1;
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(which); }
        });
        return t;
    }

    private void setPage(int idx) {
        page = idx;
        sendButton.setText(idx == 0 ? "Отправить Bytes" : "Отправить Coins");
        if (idx == 0) { Ui.setBg(sendButton, Ui.gradient(Ui.RED, Ui.VIOLET, 28, act)); sendButton.setTextColor(Color.WHITE); }
        else { Ui.setBg(sendButton, Ui.gradient(Ui.GOLD2, Ui.GOLD2, 28, act)); sendButton.setTextColor(Color.rgb(30, 20, 0)); }
    }

    private void showTab(int which) {
        Ui.setBg(tabTx, which == 0 ? Ui.round(Ui.CARD_LIGHT, 20, act) : Ui.round(Color.TRANSPARENT, 20, act));
        Ui.setBg(tabCol, which == 1 ? Ui.round(Ui.CARD_LIGHT, 20, act) : Ui.round(Color.TRANSPARENT, 20, act));
        tabTx.setTextColor(which == 0 ? Ui.TEXT : Ui.MUTED);
        tabCol.setTextColor(which == 1 ? Ui.TEXT : Ui.MUTED);
        list.removeAllViews();
        list.addView(Ui.text(act, "Загрузка…", 14, Ui.MUTED, false), Ui.margins(Ui.lp(-2, -2), act, 22, 10, 0, 0));
        final boolean tx = which == 0;
        ClientManager.getCurrent().apiGetRaw(tx ? "/transactions" : "/collections", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() { fill(tx, code, body); }
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
            if (a == null) a = o.optJSONArray("transactions");
            if (a == null) a = o.optJSONArray("collections");
            return a;
        } catch (Exception e) { return null; }
    }

    private void empty(String msg) {
        list.removeAllViews();
        TextView t = Ui.text(act, msg, 14, Ui.MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(act, 24), Ui.dp(act, 30), Ui.dp(act, 24), 0);
        list.addView(t, Ui.lp(-1, -2));
    }

    private void fill(boolean tx, int code, String body) {
        if (code == 404 || code == 0) { empty(tx ? "История операций появится, когда сервер её подключит." : "Коллекции появятся, когда сервер их подключит."); return; }
        if (code != 200) { empty(ClientManager.errorText(code, body)); return; }
        JSONArray arr = items(body);
        if (arr == null || arr.length() == 0) { empty(tx ? "Операций пока нет." : "Коллекция пуста."); return; }
        list.removeAllViews();
        if (tx) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                list.addView(txRow(o), Ui.margins(Ui.lp(-1, -2), act, 14, 4, 14, 4));
            }
        } else {
            LinearLayout row = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (row == null || row.getChildCount() == 3) {
                    row = new LinearLayout(act);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    list.addView(row, Ui.margins(Ui.lp(-1, -2), act, 8, 4, 8, 4));
                }
                row.addView(tile(o), Ui.margins(Ui.lp(0, Ui.dp(act, 120), 1), act, 4, 0, 4, 0));
            }
            if (row != null) while (row.getChildCount() < 3) row.addView(new View(act), Ui.lp(0, 1, 1));
        }
    }

    private View txRow(JSONObject o) {
        LinearLayout r = new LinearLayout(act);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(act, 14), Ui.dp(act, 12), Ui.dp(act, 14), Ui.dp(act, 12));
        Ui.setBg(r, Ui.round(Ui.CARD, 14, act));
        LinearLayout left = new LinearLayout(act);
        left.setOrientation(LinearLayout.VERTICAL);
        String title = Ui.str(o, "title", Ui.str(o, "type", "Операция"));
        left.addView(Ui.text(act, title, 15, Ui.TEXT, true));
        String sub = Ui.str(o, "counterparty", Ui.str(o, "created_at", ""));
        if (sub.length() > 0) left.addView(Ui.text(act, sub, 12, Ui.MUTED, false));
        r.addView(left, Ui.lp(0, -2, 1));
        double amount = o.optDouble("amount", 0);
        String cur = Ui.str(o, "currency", "");
        boolean plus = amount >= 0;
        String shown = (plus ? "+" : "") + (amount == Math.rint(amount) ? String.valueOf((long) amount) : String.valueOf(amount));
        r.addView(Ui.text(act, shown + " " + cur, 15, plus ? Color.rgb(70, 210, 120) : Ui.RED, true));
        return r;
    }

    private View tile(JSONObject o) {
        int[] col = Ui.rarityColors(Ui.str(o, "rarity", ""));
        LinearLayout t = new LinearLayout(act);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(act, 6), Ui.dp(act, 6), Ui.dp(act, 6), Ui.dp(act, 6));
        Ui.setBg(t, Ui.gradient(col[0], col[1], 16, act));
        boolean active = o.optBoolean("is_active", false);
        TextView name = Ui.pill(act, Ui.str(o, "name", "NFT"), Color.WHITE, col[1], 13);
        name.setSingleLine(true);
        t.addView(name, Ui.lp(-2, -2));
        TextView sub = Ui.text(act, active ? "активный" : Ui.str(o, "kind", ""), 10, Color.argb(220, 255, 255, 255), false);
        sub.setGravity(Gravity.CENTER);
        t.addView(sub, Ui.margins(Ui.lp(-2, -2), act, 0, 6, 0, 0));
        return t;
    }

    private void openTransfer() {
        final String currency = page == 0 ? "bytes" : "coins";
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 8), Ui.dp(act, 20), 0);
        final EditText to = new EditText(act);
        to.setSingleLine(true);
        to.setInputType(InputType.TYPE_CLASS_NUMBER);
        to.setHint("R1K1 ID получателя");
        final EditText amount = new EditText(act);
        amount.setSingleLine(true);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        amount.setHint("Сумма");
        box.addView(to);
        box.addView(amount);
        new AlertDialog.Builder(act).setTitle(page == 0 ? "Отправить Bytes" : "Отправить Coins").setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    try {
                        long sum = Long.parseLong(amount.getText().toString().trim());
                        // request_id защищает от двойного списания при повторной отправке
                        String body = new JSONObject().put("to", to.getText().toString().trim())
                            .put("amount", sum).put("currency", currency)
                            .put("request_id", UUID.randomUUID().toString()).toString();
                        ClientManager.getCurrent().apiPost("/transfer", body, new ClientManager.Response() {
                            @Override public void onReady(String s) {
                                act.runOnUiThread(new Runnable() {
                                    @Override public void run() { Toast.makeText(act, "Перевод отправлен", Toast.LENGTH_SHORT).show(); showTab(0); }
                                });
                            }
                        });
                    } catch (Exception e) { Toast.makeText(act, "Проверь ID и сумму", Toast.LENGTH_SHORT).show(); }
                }
            }).show();
    }
}
