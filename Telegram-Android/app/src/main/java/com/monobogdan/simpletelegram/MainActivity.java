package com.monobogdan.simpletelegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.simpletelegram.transport.ClientManager;
import com.monobogdan.simpletelegram.transport.Packets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 8, 12);
    private static final int PANEL = Color.rgb(20, 20, 27);
    private static final int RED = Color.rgb(255, 35, 65);
    private static final int TEXT = Color.rgb(242, 242, 247);
    private LinearLayout root;
    private LinearLayout content;
    private TextView headerTitle;
    private String activeTab = "Chats";
    private boolean adminOpen = false;

    private class DialogAdapter extends BaseAdapter {
        private List<Packets.Chat> chats;
        public void setChats(List<Packets.Chat> value) { chats = value; notifyDataSetChanged(); }
        @Override public int getCount() { return chats == null ? 0 : chats.size(); }
        @Override public Object getItem(int i) { return chats.get(i); }
        @Override public long getItemId(int i) { return chats.get(i).ID; }

        private void setChatData(ViewGroup view, Packets.Chat chat) {
            ((TextView)view.findViewById(R.id.dialog_sender)).setText(chat.Name == null ? "Unknown" : chat.Name);
            ((TextView)view.findViewById(R.id.dialog_message)).setText(chat.Text == null ? "" : chat.Text);
            if (chat.Avatar != null) ((ImageView)view.findViewById(R.id.dialog_avatar_preview)).setImageBitmap(chat.Avatar);
            view.setTag(chat);
        }
        @Override public View getView(int idx, View view, ViewGroup parent) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.dialog_item, parent, false);
                AlphaAnimation anim = new AlphaAnimation(0, 1);
                anim.setDuration(250);
                view.startAnimation(anim);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { openDialog((Packets.Chat)v.getTag()); }
                });
            }
            setChatData((ViewGroup)view, chats.get(idx));
            return view;
        }
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(TEXT);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(4), dp(5), dp(4), dp(5));
        return t;
    }

    private Button button(String label, final Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setBackgroundColor(PANEL);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (action != null) action.run(); }
        });
        return b;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(8), dp(12), dp(8));
        return l;
    }

    private void addRow(LinearLayout target, View child) {
        target.addView(child, new LinearLayout.LayoutParams(-1, -2));
    }

    private void openDialog(Packets.Chat chat) {
        Intent intent = new Intent(this, DialogActivity.class);
        intent.putExtra("id", chat.ID);
        intent.putExtra("name", chat.Name);
        startActivity(intent);
    }

    private void prepareClientManager() {
        Configuration.ApplicationConfiguration cfg = Configuration.getInstance().getApplicationConfiguration();
        ClientManager cm = ClientManager.getCurrent();
        cm.setNodeAddress(cfg.nodeAddress);
        cm.setAuthorizationToken(cfg.token);
        cm.setLegacyServer(cfg.tdsrvAddress, cfg.tdsrvKey);
        cm.setUnauthorizedListener(new ClientManager.UnauthorizedListener() {
            @Override public void onUnauthorized() {
                runOnUiThread(new Runnable() {
                    @Override public void run() { performLogout("Сессия истекла. Войди заново."); }
                });
            }
        });
        cm.setErrorHandler(new ClientManager.NetworkErrorHandler() {
            @Override public void onError(final String message, int code) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(MainActivity.this, "Сеть: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performLogout(String message) {
        Configuration.getInstance().clearSession();
        ClientManager.getCurrent().setAuthorizationToken(null);
        if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this).setTitle("Выйти из аккаунта?")
            .setMessage("Токен сессии будет удалён с этого устройства.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Выйти", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { performLogout(null); }
            }).show();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(PANEL);
        header.setPadding(dp(14), dp(6), dp(14), dp(6));

        TextView logo = text("R1K1", 22, true);
        logo.setTextColor(RED);
        logo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { showAdminPanel(); return true; }
        });
        header.addView(logo, new LinearLayout.LayoutParams(-2, -2));

        headerTitle = text("SYS // Chats", 16, true);
        headerTitle.setGravity(Gravity.CENTER_VERTICAL);
        headerTitle.setPadding(dp(14), 0, 0, 0);
        headerTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { showAdminPanel(); return true; }
        });
        header.addView(headerTitle, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(PANEL);
        String[] tabs = new String[] {"Profile", "Shop", "Gifts", "Chats"};
        for (final String tab : tabs) {
            Button b = new Button(this);
            b.setText(tab);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setTextColor(tab.equals(activeTab) ? RED : TEXT);
            b.setBackgroundColor(PANEL);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showTab(tab); }
            });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(54), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void showTab(String tab) {
        activeTab = tab;
        content.removeAllViews();
        headerTitle.setText("SYS // " + tab);
        if ("Chats".equals(tab)) showChats();
        else if ("Profile".equals(tab)) showProfile();
        else if ("Shop".equals(tab)) showShop();
        else if ("Gifts".equals(tab)) showGifts();
    }

    private void showTdsrvSetup() {
        LinearLayout panel = vertical();
        addRow(panel, text("Чаты работают через сервер TDSrv. Он ещё не подключён.", 15, false));
        addRow(panel, button("Подключить TDSrv", new Runnable() {
            @Override public void run() { showTdsrvDialog(); }
        }));
        content.addView(panel, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showTdsrvDialog() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        final EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("адрес:13377");
        final EditText key = new EditText(this);
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setHint("ключ TDSrv");
        fields.addView(address);
        fields.addView(key);
        new AlertDialog.Builder(this).setTitle("Подключить TDSrv").setView(fields)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Проверить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    final String node = address.getText().toString().trim();
                    final String authKey = key.getText().toString().trim();
                    if (node.length() == 0 || authKey.length() == 0) return;
                    ClientManager.getCurrent().checkCredentialsValidity(node, authKey, new ClientManager.CredentialsResponse() {
                        @Override public void onReady(final boolean isValid) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    if (!isValid) {
                                        Toast.makeText(MainActivity.this, "Ключ TDSrv не подошёл", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    Configuration.ApplicationConfiguration cfg = Configuration.getInstance().getApplicationConfiguration();
                                    cfg.tdsrvAddress = node;
                                    cfg.tdsrvKey = authKey;
                                    Configuration.getInstance().save();
                                    ClientManager.getCurrent().setLegacyServer(node, authKey);
                                    showTab("Chats");
                                }
                            });
                        }
                    });
                }
            }).show();
    }

    private void showChats() {
        if (!Configuration.getInstance().hasTdsrv()) { showTdsrvSetup(); return; }
        ListView list = new ListView(this);
        list.setId(R.id.messages_view);
        list.setBackgroundColor(BG);
        content.addView(list, new LinearLayout.LayoutParams(-1, -1));
        loadChats(list);
    }

    private void loadChats(final ListView list) {
        ClientManager.getCurrent().queryChats(50, new ClientManager.Response() {
            @Override public void onReady(final String response) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            final List<Packets.Chat> chats = Packets.parseChatListFromQueryResponse(response);
                            final DialogAdapter adapter = new DialogAdapter();
                            adapter.setChats(chats);
                            list.setAdapter(adapter);
                            for (final Packets.Chat chat : chats) {
                                if (chat.Photo == null || chat.Photo.length() == 0) continue;
                                PhotoCache.getInstance().scheduleDownload(ClientManager.getCurrent().getPhotoUrl(chat.Photo), new PhotoCache.Response() {
                                    @Override public void onReady(Bitmap bmp) {
                                        chat.Avatar = bmp;
                                        runOnUiThread(new Runnable() { @Override public void run() { adapter.notifyDataSetChanged(); } });
                                    }
                                    @Override public void onFailed() { }
                                });
                            }
                        } catch (Exception e) {
                            TextView error = text("Не удалось загрузить чаты. Проверь API сервера.\n\n" + e.getMessage(), 14, false);
                            content.removeAllViews();
                            addRow(content, error);
                        }
                    }
                });
            }
        });
    }

    private static String field(JSONObject o, String key, String def) {
        if (o == null || !o.has(key) || o.isNull(key)) return def;
        String v = String.valueOf(o.opt(key)).trim();
        return v.length() == 0 ? def : v;
    }

    private void showProfile() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = vertical();
        scroll.setBackgroundColor(BG);
        scroll.addView(panel);
        TextView title = text("R1K1.SYS // PROFILE", 20, true);
        title.setTextColor(RED);
        addRow(panel, title);
        final LinearLayout card = vertical();
        addRow(panel, card);
        addRow(panel, button("Продать NFT / подарок", new Runnable() {
            @Override public void run() {
                Intent intent = new Intent(MainActivity.this, TradingActivity.class);
                intent.putExtra("open_sell", true);
                startActivity(intent);
            }
        }));
        addRow(panel, button("Найти профиль по ID", new Runnable() {
            @Override public void run() {
                final EditText id = new EditText(MainActivity.this);
                id.setSingleLine(true);
                id.setInputType(InputType.TYPE_CLASS_NUMBER);
                id.setHint("Telegram ID");
                new AlertDialog.Builder(MainActivity.this).setTitle("ID пользователя").setView(id)
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Загрузить", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { loadProfile(id.getText().toString()); }
                    })
                    .show();
            }
        }));
        addRow(panel, button("Emoji-паки", new Runnable() {
            @Override public void run() { showApiList("Emoji-паки", "/emoji/packs"); }
        }));
        addRow(panel, button("R1K1 Father — создать бота", new Runnable() {
            @Override public void run() { showCreateBot(); }
        }));
        addRow(panel, button("Выйти из аккаунта", new Runnable() {
            @Override public void run() { confirmLogout(); }
        }));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        loadMyProfile(card);
    }

    /** Профиль берётся только с сервера (GET /me). Ничего не выдумывается на клиенте. */
    private void loadMyProfile(final LinearLayout card) {
        card.removeAllViews();
        addRow(card, text("Загрузка профиля…", 14, false));
        ClientManager.getCurrent().apiGetRaw("/me", new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (!"Profile".equals(activeTab)) return;
                        card.removeAllViews();
                        if (code == 200) {
                            try { renderProfile(card, new JSONObject(body)); return; }
                            catch (Exception e) { addRow(card, text("Не удалось прочитать ответ сервера.", 14, false)); }
                        } else if (code != 401) {
                            addRow(card, text(ClientManager.errorText(code, body), 14, false));
                        }
                        addRow(card, button("Повторить", new Runnable() {
                            @Override public void run() { loadMyProfile(card); }
                        }));
                    }
                });
            }
        });
    }

    private void renderProfile(LinearLayout card, JSONObject o) {
        String username = field(o, "username", "");
        String id = field(o, "tg_id", "");
        String display = username.length() > 0 ? "@" + username.replace("@", "") : (id.length() > 0 ? "ID " + id : "—");
        String initial = username.length() > 0 ? username.replace("@", "").substring(0, Math.min(1, username.replace("@", "").length())).toUpperCase() : "◉";

        TextView avatar = text(initial, 54, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(RED);
        addRow(card, avatar);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER);
        nameRow.addView(text(display, 20, true));
        if (o.optBoolean("is_verified", false)) {
            TextView badge = text("✔", 20, true);
            badge.setTextColor(Color.rgb(64, 160, 255));
            nameRow.addView(badge);
        }
        addRow(card, nameRow);

        String phone = field(o, "phone", "");
        if (id.length() > 0) addRow(card, text("ID: " + id + (phone.length() > 0 ? "   |   " + phone : ""), 14, false));
        addRow(card, text("Bytes: " + field(o, "statiki", "—") + "     Coins: " + field(o, "koiny", "—"), 16, true));
        if (o.optBoolean("is_verified", false)) addRow(card, text("Подтверждённый аккаунт", 14, true));
        if (o.optBoolean("premium", false)) addRow(card, text("♛ Premium", 14, true));
        String nft = field(o, "nft", "");
        if (nft.length() > 0) addRow(card, text("NFT: " + nft, 14, false));
        String bio = field(o, "bio", "");
        if (bio.length() > 0) addRow(card, text("Bio: " + bio, 14, false));
    }

    private static String prettyJson(String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (sb.length() > 0) sb.append('\n');
                sb.append(k).append(": ").append(o.isNull(k) ? "—" : String.valueOf(o.opt(k)));
            }
            return sb.toString();
        } catch (Exception e) { return raw; }
    }

    private void loadProfile(String userId) {
        if (userId == null || userId.trim().length() == 0) return;
        final String path = "/user/" + userId.trim();
        ClientManager.getCurrent().apiGet(path, new ClientManager.Response() {
            @Override public void onReady(final String response) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        new AlertDialog.Builder(MainActivity.this).setTitle("Профиль")
                            .setMessage(prettyJson(response)).setPositiveButton("OK", null).show();
                    }
                });
            }
        });
    }

    private void showShop() {
        LinearLayout panel = vertical();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        TextView title = text("R1K1.SYS // SHOP", 20, true);
        title.setTextColor(RED);
        addRow(panel, title);
        addRow(panel, button("Открыть R1K1 Market // Трейдинг", new Runnable() {
            @Override public void run() { startActivity(new Intent(MainActivity.this, TradingActivity.class)); }
        }));
        addRow(panel, text("NFT: R1K1 • VOID • GHOST", 16, true));
        addRow(panel, text("Оплата Bytes/Coins и наличие предметов зависят от API сервера.", 13, false));
        addRow(panel, button("Загрузить магазин (GET /shop)", new Runnable() {
            @Override public void run() { showApiList("Магазин", "/shop"); }
        }));
        addRow(panel, button("NFT-ники (GET /usernames/shop)", new Runnable() {
            @Override public void run() { showApiList("NFT-ники", "/usernames/shop"); }
        }));
        addRow(panel, button("Купить username", new Runnable() {
            @Override public void run() { showBuyUsername(); }
        }));
        addRow(panel, button("Крафт: 3 обычных NFT → 1 редкий", new Runnable() {
            @Override public void run() { postJsonDialog("Крафт NFT", "/craft", "{\"count\":3}"); }
        }));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showGifts() {
        LinearLayout panel = vertical();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        TextView title = text("R1K1.SYS // GIFTS", 20, true);
        title.setTextColor(RED);
        addRow(panel, title);
        addRow(panel, text("Rose  •  Heart  •  Star", 16, true));
        addRow(panel, text("Редкости: Обычный, Необычный, Редкий, Эпический, Легендарный, Уникальный, Мега, Ультра", 13, false));
        addRow(panel, button("Загрузить подарки (GET /gifts)", new Runnable() {
            @Override public void run() { showApiList("Подарки", "/gifts"); }
        }));
        addRow(panel, button("Отправить подарок", new Runnable() {
            @Override public void run() { showSendGift(); }
        }));
        addRow(panel, button("Розыгрыш — участвовать", new Runnable() {
            @Override public void run() { showJoinGiveaway(); }
        }));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showJoinGiveaway() {
        final EditText giveawayId = new EditText(this);
        giveawayId.setSingleLine(true);
        giveawayId.setHint("ID розыгрыша");
        new AlertDialog.Builder(this).setTitle("Участвовать в розыгрыше").setView(giveawayId)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Участвовать", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    String id = giveawayId.getText().toString().trim().replace("\"", "");
                    postJson("/giveaways/join", "{\"giveaway_id\":\"" + id + "\"}", "Розыгрыш");
                }
            }).show();
    }

    private void showApiList(final String title, final String path) {
        ClientManager.getCurrent().apiGet(path, new ClientManager.Response() {
            @Override public void onReady(final String response) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        new AlertDialog.Builder(MainActivity.this).setTitle(title).setMessage(response)
                            .setPositiveButton("OK", null).show();
                    }
                });
            }
        });
    }

    private void showBuyUsername() {
        final EditText username = new EditText(this);
        username.setSingleLine(true);
        username.setHint("@username");
        new AlertDialog.Builder(this).setTitle("Покупка username").setView(username)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Купить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    String name = username.getText().toString().trim().replace("\"", "");
                    if (name.startsWith("@")) name = name.substring(1);
                    postJson("/usernames/buy", "{\"username\":\"" + name + "\"}", "Покупка username");
                }
            }).show();
    }

    private void showSendGift() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        final EditText user = new EditText(this);
        user.setHint("ID получателя");
        user.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText gift = new EditText(this);
        gift.setHint("Rose / Heart / Star");
        fields.addView(user);
        fields.addView(gift);
        new AlertDialog.Builder(this).setTitle("Отправить подарок").setView(fields)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    postJson("/gifts/send", "{\"user_id\":\"" + user.getText().toString() +
                        "\",\"gift\":\"" + gift.getText().toString().replace("\"", "") + "\"}", "Подарок");
                }
            }).show();
    }

    private void showCreateBot() {
        final EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Имя бота");
        new AlertDialog.Builder(this).setTitle("/newbot").setView(name)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    postJson("/bots/create", "{\"command\":\"/newbot\",\"name\":\"" +
                        name.getText().toString().replace("\"", "") + "\"}", "R1K1 Father");
                }
            }).show();
    }

    private void postJsonDialog(String title, String path, String body) {
        postJson(path, body, title);
    }

    private void postJson(final String path, String body, final String title) {
        ClientManager.getCurrent().apiPost(path, body, new ClientManager.Response() {
            @Override public void onReady(final String response) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        new AlertDialog.Builder(MainActivity.this).setTitle(title).setMessage(response)
                            .setPositiveButton("OK", null).show();
                    }
                });
            }
        });
    }

    private void showAdminPanel() {
        final String[] actions = {"Create NFT", "Create Gift", "Launch Giveaway"};
        new AlertDialog.Builder(this).setTitle("R1K1.SYS // ADMIN")
            .setItems(actions, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) postJson("/admin/create_nft", "{\"name\":\"R1K1\"}", "Create NFT");
                    if (which == 1) postJson("/admin/create_gift", "{\"name\":\"Rose\"}", "Create Gift");
                    if (which == 2) postJson("/admin/launch_giveaway", "{}", "Launch Giveaway");
                }
            }).show();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Configuration.getInstance() == null) Configuration.initialize(getApplicationContext());
        if (!Configuration.getInstance().isAuthorized()) {
            Intent intent = new Intent(this, AuthenticationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        PhotoCache.initialize(getApplicationContext().getCacheDir().getAbsolutePath());
        prepareClientManager();
        buildShell();
        showTab("Chats");
    }
}
