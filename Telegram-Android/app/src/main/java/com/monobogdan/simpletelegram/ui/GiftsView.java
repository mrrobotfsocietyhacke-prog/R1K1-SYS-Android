package com.monobogdan.simpletelegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
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

import java.util.ArrayList;
import java.util.List;

/** Экран «Купить подарок»: сетка 3 в ряд. Цены окончательно проверяет сервер. */
public class GiftsView {
    private static class Gift {
        String id, name, emoji, rarity;
        int price;
        Gift(String id, String name, String emoji, int price, String rarity) {
            this.id = id; this.name = name; this.emoji = emoji; this.price = price; this.rarity = rarity;
        }
    }

    /** Показывается, пока сервер не отдаёт GET /gifts. Только для отображения. */
    private static List<Gift> defaults() {
        List<Gift> l = new ArrayList<Gift>();
        l.add(new Gift("heart", "Сердце", "\uD83D\uDC96", 15, "common"));
        l.add(new Gift("bear", "Мишка", "\uD83E\uDDF8", 15, "common"));
        l.add(new Gift("giftbox", "Подарок", "\uD83C\uDF81", 25, "common"));
        l.add(new Gift("rose", "Роза", "\uD83C\uDF39", 25, "common"));
        l.add(new Gift("cake", "Торт", "\uD83C\uDF82", 50, "common"));
        l.add(new Gift("bouquet", "Букет", "\uD83D\uDC90", 50, "common"));
        l.add(new Gift("rocket", "Ракета", "\uD83D\uDE80", 50, "common"));
        l.add(new Gift("champagne", "Шампанское", "\uD83C\uDF7E", 50, "common"));
        l.add(new Gift("trophy", "Кубок", "\uD83C\uDFC6", 100, "common"));
        l.add(new Gift("ring", "Кольцо", "\uD83D\uDC8D", 100, "common"));
        l.add(new Gift("diamond", "Алмаз", "\uD83D\uDC8E", 100, "common"));
        return l;
    }

    private static String emojiFor(String key) {
        String k = key == null ? "" : key.toLowerCase();
        for (Gift g : defaults()) if (k.equals(g.id) || k.equals(g.name.toLowerCase())) return g.emoji;
        if (k.equals("star")) return "\u2B50";
        return "\uD83C\uDF81";
    }

    private final Activity act;
    private final LinearLayout grid;
    private final TextView note;

    private GiftsView(Activity a, LinearLayout grid, TextView note) { this.act = a; this.grid = grid; this.note = note; }

    public static View build(final Activity act) {
        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 20));
        scroll.addView(root);
        TextView title = Ui.text(act, "Купить подарок", 20, Ui.TEXT, true);
        title.setPadding(Ui.dp(act, 8), 0, 0, Ui.dp(act, 4));
        root.addView(title);
        TextView note = Ui.text(act, "", 12, Ui.MUTED, false);
        note.setPadding(Ui.dp(act, 8), 0, Ui.dp(act, 8), Ui.dp(act, 10));
        root.addView(note);
        LinearLayout grid = new LinearLayout(act);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid);
        final GiftsView v = new GiftsView(act, grid, note);
        v.render(defaults(), "Загружаю каталог…");
        v.load();
        return scroll;
    }

    private void load() {
        ClientManager.getCurrent().apiGetRaw("/gifts", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        List<Gift> parsed = code == 200 ? parse(body) : null;
                        if (parsed != null && !parsed.isEmpty()) render(parsed, "");
                        else render(defaults(), "Каталог по умолчанию. Цены и покупку подтверждает сервер.");
                    }
                });
            }
        });
    }

    private List<Gift> parse(String body) {
        try {
            JSONArray arr;
            String t = body.trim();
            if (t.startsWith("[")) arr = new JSONArray(t);
            else {
                JSONObject o = new JSONObject(t);
                arr = o.optJSONArray("gifts");
                if (arr == null) arr = o.optJSONArray("items");
                if (arr == null) return null;
            }
            List<Gift> out = new ArrayList<Gift>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject g = arr.optJSONObject(i);
                if (g == null) continue;
                String id = Ui.str(g, "id", Ui.str(g, "gift_id", Ui.str(g, "name", "")));
                String name = Ui.str(g, "name", id);
                String emoji = Ui.str(g, "emoji", emojiFor(id.length() > 0 ? id : name));
                int price = (int) g.optDouble("price", 0);
                out.add(new Gift(id, name, emoji, price, Ui.str(g, "rarity", "common")));
            }
            return out;
        } catch (Exception e) { return null; }
    }

    private void render(List<Gift> gifts, String noteText) {
        note.setText(noteText);
        note.setVisibility(noteText.length() == 0 ? View.GONE : View.VISIBLE);
        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < gifts.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(act);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, Ui.lp(-1, -2));
            }
            row.addView(cell(gifts.get(i)), Ui.lp(0, -2, 1));
        }
        int rest = gifts.size() % 3;
        if (rest != 0) for (int i = rest; i < 3; i++) row.addView(new View(act), Ui.lp(0, 1, 1));
    }

    private View cell(final Gift g) {
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(Ui.dp(act, 4), Ui.dp(act, 12), Ui.dp(act, 4), Ui.dp(act, 12));
        TextView e = Ui.text(act, g.emoji, 44, Ui.TEXT, false);
        e.setGravity(Gravity.CENTER);
        c.addView(e, Ui.lp(-2, -2));
        TextView price = Ui.pill(act, "\u2B50 " + g.price, Ui.CARD_LIGHT, Ui.GOLD2, 14);
        price.setText("\u25C6 " + g.price);
        c.addView(price, Ui.margins(Ui.lp(-2, -2), act, 0, 8, 0, 0));
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGift(g); }
        });
        return c;
    }

    private void openGift(final Gift g) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 10), Ui.dp(act, 20), 0);
        TextView e = Ui.text(act, g.emoji, 64, Ui.TEXT, false);
        e.setGravity(Gravity.CENTER);
        box.addView(e, Ui.lp(-1, -2));
        final EditText to = new EditText(act);
        to.setSingleLine(true);
        to.setInputType(InputType.TYPE_CLASS_NUMBER);
        to.setHint("R1K1 ID получателя");
        box.addView(to, Ui.lp(-1, -2));
        new AlertDialog.Builder(act).setTitle(g.name + " \u2014 " + g.price + " Bytes").setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { send(g, to.getText().toString().trim()); }
            }).show();
    }

    private void send(Gift g, String userId) {
        if (userId.length() == 0) { Toast.makeText(act, "Введи ID получателя", Toast.LENGTH_SHORT).show(); return; }
        try {
            // Цену не отправляем: её определяет и списывает сервер.
            String body = new JSONObject().put("gift_id", g.id).put("user_id", userId).toString();
            ClientManager.getCurrent().apiPost("/gifts/send", body, new ClientManager.Response() {
                @Override public void onReady(String s) {
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() { Toast.makeText(act, "Подарок отправлен", Toast.LENGTH_SHORT).show(); }
                    });
                }
            });
        } catch (Exception ex) { Toast.makeText(act, ex.toString(), Toast.LENGTH_LONG).show(); }
    }
}
