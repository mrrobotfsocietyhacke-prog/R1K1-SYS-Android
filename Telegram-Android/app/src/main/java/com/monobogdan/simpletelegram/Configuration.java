package com.monobogdan.simpletelegram;

import android.content.Context;
import android.content.SharedPreferences;

public class Configuration {
    public class ApplicationConfiguration {
        /** Адрес Node.js backend R1K1.SYS. */
        public String nodeAddress;
        /** Токен сессии, полученный из /auth/verify. */
        public String token;
        /** Старый TDSrv (чаты): адрес и ключ — необязательны. */
        public String tdsrvAddress;
        public String tdsrvKey;

        public void load(SharedPreferences prefs) {
            nodeAddress = prefs.getString("node_address", null);
            token = prefs.getString("session_token", null);
            tdsrvAddress = prefs.getString("tdsrv_address", null);
            tdsrvKey = prefs.getString("tdsrv_key", null);
        }

        public void save(SharedPreferences prefs) {
            SharedPreferences.Editor edit = prefs.edit();
            put(edit, "node_address", nodeAddress);
            put(edit, "session_token", token);
            put(edit, "tdsrv_address", tdsrvAddress);
            put(edit, "tdsrv_key", tdsrvKey);
            edit.apply();
        }

        private void put(SharedPreferences.Editor edit, String key, String value) {
            if (value == null || value.length() == 0) edit.remove(key);
            else edit.putString(key, value);
        }
    }

    private static String SHARED_PREFS_KEY = "configuration";
    private static Configuration instance;

    public static Configuration getInstance() {
        return instance;
    }

    public static void initialize(Context ctx) {
        instance = new Configuration(ctx);
    }

    private Context context;
    private SharedPreferences sharedPrefs;

    private ApplicationConfiguration applicationConfiguration;

    private Configuration(Context ctx) {
        context = ctx;

        sharedPrefs = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        migrateLegacy();
        applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.load(sharedPrefs);
    }

    /** Старые версии хранили TDSrv-ключ как auth_key и адрес как node_address. Переносим их в TDSrv-поля. */
    private void migrateLegacy() {
        if (sharedPrefs.contains("auth_key")) {
            SharedPreferences.Editor e = sharedPrefs.edit();
            if (!sharedPrefs.contains("tdsrv_key")) {
                e.putString("tdsrv_key", sharedPrefs.getString("auth_key", ""));
                String oldNode = sharedPrefs.getString("node_address", null);
                if (oldNode != null) e.putString("tdsrv_address", oldNode);
            }
            e.remove("auth_key");
            e.remove("node_address"); // теперь это адрес Node.js backend, вход нужно пройти заново
            e.apply();
        }
    }

    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    public boolean isAuthorized() {
        ApplicationConfiguration c = applicationConfiguration;
        return c.token != null && c.token.length() > 0 && c.nodeAddress != null && c.nodeAddress.length() > 0;
    }

    public boolean hasTdsrv() {
        ApplicationConfiguration c = applicationConfiguration;
        return c.tdsrvAddress != null && c.tdsrvAddress.length() > 0 && c.tdsrvKey != null && c.tdsrvKey.length() > 0;
    }

    /** Выход из аккаунта: токен удаляется, адрес сервера остаётся для удобства. */
    public void clearSession() {
        applicationConfiguration.token = null;
        applicationConfiguration.save(sharedPrefs);
    }

    public void load() {
        applicationConfiguration.load(sharedPrefs);
    }

    public void save() {
        applicationConfiguration.save(sharedPrefs);
    }
}
