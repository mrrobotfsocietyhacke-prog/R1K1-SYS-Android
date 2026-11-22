package com.monobogdan.simpletelegram.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.monobogdan.simpletelegram.transport.ClientManager;

import org.json.JSONObject;

/** Профиль: шапка с аватаркой, тип номера, балансы и меню. Все данные только из GET /me. */
public class ProfileView {
    public interface Host {
        void openWallet(JSONObject me);
        void openSettings();
        void openTrading();
        void findUser();
        void emojiPacks();
        void createBot();
        void logout();
    }

    /** Подпись типа номера. Приоритет у поля number_type с сервера; префикс — временная замена. */
    static String numberLabel(JSONObject o, String phone) {
        String t = Ui.str(o, "number_type", "").toLowerCase();
        if (t.equals("nft")) return "NFT-номер";
        if (t.equals("exclusive")) return "Эксклюзив";
        if (t.equals("special")) return "Особый номер";
        if (t.equals("regular")) return "Обычный номер";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("666") || digits.startsWith("777") || digits.startsWith("888") || digits.startsWith("999"))
            return "Особый номер";
        return "Обычный номер";
    }

    public static View build(final Activity act, final Host host) {
        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(act, 14), Ui.dp(act, 12), Ui.dp(act, 14), Ui.dp(act, 24));
        scroll.addView(root);
        final LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.VERTICAL);
        root.addView(head, Ui.lp(-1, -2));
        final LinearLayout menu = new LinearLayout(act);
        menu.setOrientation(LinearLayout.VERTICAL);
        root.addView(menu, Ui.lp(-1, -2));
        load(act, host, head, menu);
        return scroll;
    }

    private static void load(final Activity act, final Host host, final LinearLayout head, final LinearLayout menu) {
        head.removeAllViews();
        menu.removeAllViews();
        head.addView(Ui.text(act, "Загрузка профиля…", 14, Ui.MUTED, false));
        ClientManager.getCurrent().apiGetRaw("/me", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        head.removeAllViews();
                        if (code == 200) {
                            try { render(act, host, head, menu, new JSONObject(body)); return; }
                            catch (Exception e) { head.addView(Ui.text(act, "Не удалось прочитать ответ сервера.", 14, Ui.MUTED, false)); }
                        } else if (code != 401) {
                            head.addView(Ui.text(act, ClientManager.errorText(code, body), 14, Ui.MUTED, false));
                        }
                        TextView retry = Ui.pill(act, "Повторить", Ui.CARD_LIGHT, Ui.TEXT, 14);
                        retry.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) { load(act, host, head, menu); }
                        });
                        head.addView(retry, Ui.margins(Ui.lp(-2, -2), act, 0, 12, 0, 0));
                        addMenu(act, host, menu, null);
                    }
                });
            }
        });
    }

    private static void render(Activity act, final Host host, LinearLayout head, LinearLayout menu, final JSONObject o) {
        String username = Ui.str(o, "username", "").replace("@", "");
        String id = Ui.str(o, "r1k1_id", Ui.str(o, "tg_id", ""));
        String phone = Ui.str(o, "phone", "");
        String display = username.length() > 0 ? "@" + username : (id.length() > 0 ? "ID " + id : "R1K1");
        boolean verified = o.optBoolean("is_verified", false);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(act, 16), Ui.dp(act, 24), Ui.dp(act, 16), Ui.dp(act, 20));
        Ui.setBg(card, Ui.gradient(Color.rgb(60, 14, 64), Color.rgb(24, 10, 44), 22, act));
        head.addView(card, Ui.lp(-1, -2));

        String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "R";
        TextView av = Ui.text(act, initial, 36, Color.WHITE, true);
        av.setGravity(Gravity.CENTER);
        Ui.setBg(av, Ui.circle(Ui.RED, Ui.VIOLET));
        card.addView(av, Ui.lp(Ui.dp(act, 88), Ui.dp(act, 88)));

        LinearLayout nameRow = new LinearLayout(act);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER);
        nameRow.addView(Ui.text(act, display, 22, Color.WHITE, true));
        if (verified) {
            TextView badge = Ui.text(act, " \u2714", 20, Ui.BLUE, true);
            nameRow.addView(badge);
        }
        if (o.optBoolean("premium", false)) nameRow.addView(Ui.text(act, " \u265B", 20, Ui.GOLD2, true));
        card.addView(nameRow, Ui.margins(Ui.lp(-2, -2), act, 0, 12, 0, 0));

        if (id.length() > 0) card.addView(Ui.text(act, "R1K1 ID: " + id, 13, Color.argb(190, 255, 255, 255), false));
        if (phone.length() > 0) {
            card.addView(Ui.pill(act, numberLabel(o, phone), Color.argb(60, 255, 255, 255), Color.WHITE, 11),
                Ui.margins(Ui.lp(-2, -2), act, 0, 14, 0, 4));
            card.addView(Ui.text(act, phone, 16, Color.WHITE, true));
        }
        String bio = Ui.str(o, "bio", "");
        if (bio.length() > 0) card.addView(Ui.text(act, bio, 13, Color.argb(200, 255, 255, 255), false),
            Ui.margins(Ui.lp(-2, -2), act, 0, 10, 0, 0));

        LinearLayout bal = new LinearLayout(act);
        bal.setOrientation(LinearLayout.HORIZONTAL);
        bal.addView(balance(act, "Bytes", Ui.str(o, "statiki", "0"), Ui.RED, o, host), Ui.margins(Ui.lp(0, -2, 1), act, 0, 0, 5, 0));
        bal.addView(balance(act, "Coins", Ui.str(o, "koiny", "0"), Ui.GOLD2, o, host), Ui.margins(Ui.lp(0, -2, 1), act, 5, 0, 0, 0));
        head.addView(bal, Ui.margins(Ui.lp(-1, -2), act, 0, 12, 0, 6));
        addMenu(act, host, menu, o);
    }

    private static View balance(Activity act, String label, String value, int accent, final JSONObject me, final Host host) {
        LinearLayout b = new LinearLayout(act);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(Ui.dp(act, 16), Ui.dp(act, 14), Ui.dp(act, 16), Ui.dp(act, 14));
        Ui.setBg(b, Ui.round(Ui.CARD, 16, act));
        b.addView(Ui.text(act, label, 12, Ui.MUTED, false));
        b.addView(Ui.text(act, value, 22, accent, true));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.openWallet(me); }
        });
        return b;
    }

    private static void addMenu(Activity act, final Host host, LinearLayout menu, final JSONObject me) {
        if (me != null) item(act, menu, "Деньги", new Runnable() { @Override public void run() { host.openWallet(me); } });
        item(act, menu, "Настройки", new Runnable() { @Override public void run() { host.openSettings(); } });
        item(act, menu, "Продать NFT / подарок", new Runnable() { @Override public void run() { host.openTrading(); } });
        item(act, menu, "Найти профиль по ID", new Runnable() { @Override public void run() { host.findUser(); } });
        item(act, menu, "Emoji-паки", new Runnable() { @Override public void run() { host.emojiPacks(); } });
        item(act, menu, "R1K1 Father \u2014 создать бота", new Runnable() { @Override public void run() { host.createBot(); } });
    }

    private static void item(Activity act, LinearLayout menu, String label, final Runnable action) {
        LinearLayout r = new LinearLayout(act);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(act, 16), Ui.dp(act, 16), Ui.dp(act, 16), Ui.dp(act, 16));
        Ui.setBg(r, Ui.round(Ui.CARD, 14, act));
        r.addView(Ui.text(act, label, 15, Ui.TEXT, false), Ui.lp(0, -2, 1));
        r.addView(Ui.text(act, "\u203A", 20, Ui.MUTED, true), Ui.lp(-2, -2));
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        menu.addView(r, Ui.margins(Ui.lp(-1, -2), act, 0, 4, 0, 4));
    }
}
