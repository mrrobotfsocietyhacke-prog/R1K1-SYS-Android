package com.monobogdan.simpletelegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.simpletelegram.Configuration;
import com.monobogdan.simpletelegram.transport.ClientManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/** Настройки. Рабочие пункты подключены; остальные честно помечены «Скоро». */
public class SettingsView {
    public interface Host {
        void connectTdsrv();
        void disconnectTdsrv();
        void logout();
    }

    public static View build(final Activity act, final Host host) {
        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(act, 14), Ui.dp(act, 10), Ui.dp(act, 14), Ui.dp(act, 24));
        scroll.addView(root);

        Configuration.ApplicationConfiguration cfg = Configuration.getInstance().getApplicationConfiguration();
        section(act, root, "Аккаунт");
        info(act, root, "Сервер", cfg.nodeAddress == null ? "—" : cfg.nodeAddress);
        row(act, root, "Активные сеансы", "", new Runnable() {
            @Override public void run() { showSessions(act); }
        });

        section(act, root, "Чаты");
        if (Configuration.getInstance().hasTdsrv()) {
            info(act, root, "TDSrv", "подключён");
            row(act, root, "Отключить TDSrv", "", new Runnable() { @Override public void run() { host.disconnectTdsrv(); } });
        } else {
            info(act, root, "TDSrv", "не подключён");
            row(act, root, "Подключить TDSrv", "", new Runnable() { @Override public void run() { host.connectTdsrv(); } });
        }

        section(act, root, "Данные");
        row(act, root, "Очистить кэш изображений", "", new Runnable() {
            @Override public void run() { clearCache(act); }
        });

        section(act, root, "Скоро");
        soon(act, root, "Конфиденциальность");
        soon(act, root, "Уведомления и звуки");
        soon(act, root, "Внешний вид и темы");
        soon(act, root, "Язык");

        section(act, root, "О приложении");
        info(act, root, "Версия", "R1K1.SYS Android, этап 2");

        row(act, root, "Выйти из аккаунта", "", new Runnable() { @Override public void run() { host.logout(); } });
        return scroll;
    }

    private static void section(Activity a, LinearLayout root, String title) {
        TextView t = Ui.text(a, title.toUpperCase(), 12, Ui.ACTIVE, true);
        t.setPadding(Ui.dp(a, 6), Ui.dp(a, 20), 0, Ui.dp(a, 6));
        root.addView(t);
    }

    private static LinearLayout box(Activity a, LinearLayout root) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(a, 16), Ui.dp(a, 15), Ui.dp(a, 16), Ui.dp(a, 15));
        Ui.setBg(r, Ui.round(Ui.CARD, 14, a));
        root.addView(r, Ui.margins(Ui.lp(-1, -2), a, 0, 3, 0, 3));
        return r;
    }

    private static void info(Activity a, LinearLayout root, String label, String value) {
        LinearLayout r = box(a, root);
        r.addView(Ui.text(a, label, 15, Ui.TEXT, false), Ui.lp(0, -2, 1));
        TextView v = Ui.text(a, value, 13, Ui.MUTED, false);
        v.setSingleLine(true);
        r.addView(v, Ui.lp(-2, -2));
    }

    private static void row(Activity a, LinearLayout root, String label, String hint, final Runnable action) {
        LinearLayout r = box(a, root);
        r.addView(Ui.text(a, label, 15, Ui.TEXT, false), Ui.lp(0, -2, 1));
        r.addView(Ui.text(a, "\u203A", 20, Ui.MUTED, true), Ui.lp(-2, -2));
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
    }

    private static void soon(Activity a, LinearLayout root, String label) {
        LinearLayout r = box(a, root);
        r.addView(Ui.text(a, label, 15, Ui.MUTED, false), Ui.lp(0, -2, 1));
        r.addView(Ui.pill(a, "Скоро", Ui.CARD_LIGHT, Ui.MUTED, 11), Ui.lp(-2, -2));
    }

    private static void showSessions(final Activity act) {
        ClientManager.getCurrent().apiGetRaw("/sessions", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        String msg;
                        if (code == 200) {
                            StringBuilder sb = new StringBuilder();
                            try {
                                JSONArray arr = body.trim().startsWith("[") ? new JSONArray(body) : new JSONObject(body).optJSONArray("sessions");
                                for (int i = 0; arr != null && i < arr.length(); i++) {
                                    JSONObject s = arr.optJSONObject(i);
                                    if (s == null) continue;
                                    sb.append(Ui.str(s, "device", "Устройство")).append("  \u2014  ").append(Ui.str(s, "last_seen", Ui.str(s, "created_at", ""))).append('\n');
                                }
                            } catch (Exception ignored) { }
                            msg = sb.length() > 0 ? sb.toString() : "Сеансов не найдено.";
                        } else if (code == 404) msg = "Сервер пока не поддерживает список сеансов.";
                        else msg = ClientManager.errorText(code, body);
                        new AlertDialog.Builder(act).setTitle("Активные сеансы").setMessage(msg).setPositiveButton("OK", null).show();
                    }
                });
            }
        });
    }

    private static long deleteTree(File f) {
        long size = 0;
        if (f == null || !f.exists()) return 0;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) size += deleteTree(k);
        } else size = f.length();
        f.delete();
        return size;
    }

    private static void clearCache(final Activity act) {
        new AlertDialog.Builder(act).setTitle("Очистить кэш?").setMessage("Загруженные картинки будут скачаны заново.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Очистить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    long freed = 0;
                    File dir = act.getCacheDir();
                    File[] kids = dir == null ? null : dir.listFiles();
                    if (kids != null) for (File k : kids) freed += deleteTree(k);
                    Toast.makeText(act, "Освобождено: " + (freed / 1024) + " КБ", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }
}
