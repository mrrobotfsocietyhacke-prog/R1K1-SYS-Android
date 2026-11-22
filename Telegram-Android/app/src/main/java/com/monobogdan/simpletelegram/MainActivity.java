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

import com.monobogdan.simpletelegram.ui.*;
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

    private TextView backButton;
    private View subView;
    private final String[] tabKeys = new String[] {"Profile", "Shop", "Gifts", "Chats"};
    private final String[] tabTitles = new String[] {"Профиль", "Магазин", "Подарки", "Чаты"};
    private final String[] tabLabels = new String[] {"Профиль", "Магазин", "Подарки", "Чаты"};
    private final TextView[] navLabels = new TextView[4];
    private final NavIcon[] navIcons = new NavIcon[4];

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Ui.CARD);
        header.setPadding(dp(8), dp(8), dp(14), dp(8));

        backButton = Ui.text(this, "\u2039", 30, Ui.ACTIVE, true);
        backButton.setPadding(dp(10), 0, dp(10), dp(4));
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeSub(); }
        });
        header.addView(backButton, new LinearLayout.LayoutParams(-2, -2));

        headerTitle = Ui.text(this, "Чаты", 20, Ui.TEXT, true);
        headerTitle.setPadding(dp(8), 0, 0, 0);
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
        nav.setBackgroundColor(Ui.CARD);
        int[] icons = new int[] {NavIcon.PROFILE, NavIcon.SHOP, NavIcon.GIFT, NavIcon.CHAT};
        for (int i = 0; i < 4; i++) {
            final String tab = tabKeys[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            navIcons[i] = new NavIcon(this, icons[i]);
            item.addView(navIcons[i], new LinearLayout.LayoutParams(dp(26), dp(26)));
            navLabels[i] = Ui.text(this, tabLabels[i], 11, Ui.MUTED, false);
            navLabels[i].setPadding(0, dp(2), 0, 0);
            item.addView(navLabels[i], new LinearLayout.LayoutParams(-2, -2));
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showTab(tab); }
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        updateNav();
    }

    private void updateNav() {
        for (int i = 0; i < 4; i++) {
            boolean on = tabKeys[i].equals(activeTab);
            navIcons[i].setActive(on);
            navLabels[i].setTextColor(on ? Ui.ACTIVE : Ui.MUTED);
        }
    }

    private String titleOf(String tab) {
        for (int i = 0; i < 4; i++) if (tabKeys[i].equals(tab)) return tabTitles[i];
        return tab;
    }

    private void openSub(String title, View v) {
        content.removeAllViews();
        subView = v;
        content.addView(v, new LinearLayout.LayoutParams(-1, -1));
        headerTitle.setText(title);
        backButton.setVisibility(View.VISIBLE);
    }

    private void closeSub() {
        if (subView == null) return;
        showTab(activeTab);
    }

    @Override public void onBackPressed() {
        if (subView != null) closeSub();
        else super.onBackPressed();
    }

    private void showTab(String tab) {
        activeTab = tab;
        subView = null;
        if (backButton != null) backButton.setVisibility(View.GONE);
        content.removeAllViews();
        headerTitle.setText(titleOf(tab));
        updateNav();
        if ("Chats".equals(tab)) showChats();
        else if ("Profile".equals(tab)) showProfile();
        else if ("Shop".equals(tab)) showShop();
        else if ("Gifts".equals(tab)) showGifts();
    }

    private void showProfile() {
        content.addView(ProfileView.build(this, new ProfileView.Host() {
            @Override public void openWallet(JSONObject me) { openSub("Деньги", WalletView.build(MainActivity.this, me)); }
            @Override public void openSettings() { openSettingsScreen(); }
            @Override public void openTrading() { startActivity(new Intent(MainActivity.this, TradingActivity.class)); }
            @Override public void findUser() { showFindUser(); }
            @Override public void emojiPacks() { showApiList("Emoji-паки", "/emoji/packs"); }
            @Override public void createBot() { showCreateBot(); }
            @Override public void logout() { confirmLogout(); }
        }), new LinearLayout.LayoutParams(-1, -1));
    }

    private void openSettingsScreen() {
        openSub("Настройки", SettingsView.build(this, new SettingsView.Host() {
            @Override public void connectTdsrv() { showTdsrvDialog(); }
            @Override public void disconnectTdsrv() {
                Configuration.ApplicationConfiguration cfg = Configuration.getInstance().getApplicationConfiguration();
                cfg.tdsrvAddress = null;
                cfg.tdsrvKey = null;
                Configuration.getInstance().save();
                ClientManager.getCurrent().setLegacyServer(null, null);
                openSettingsScreen();
            }
            @Override public void logout() { confirmLogout(); }
        }));
    }

    private void showFindUser() {
        final EditText id = new EditText(this);
        id.setSingleLine(true);
        id.setInputType(InputType.TYPE_CLASS_NUMBER);
        id.setHint("ID пользователя");
        new AlertDialog.Builder(this).setTitle("Найти профиль").setView(id)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Загрузить", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { loadProfile(id.getText().toString()); }
            }).show();
    }

    private void showShop() {
        content.addView(ShopView.build(this, new Runnable() {
            @Override public void run() { startActivity(new Intent(MainActivity.this, TradingActivity.class)); }
        }), new LinearLayout.LayoutParams(-1, -1));
    }

    private void showGifts() {
        content.addView(GiftsView.build(this), new LinearLayout.LayoutParams(-1, -1));
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

    /** Профиль берётся только с сервера (GET /me). Ничего не выдумывается на клиенте. */
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
