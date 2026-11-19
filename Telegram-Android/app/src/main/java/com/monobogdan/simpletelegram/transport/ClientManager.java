package com.monobogdan.simpletelegram.transport;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Два независимых канала:
 *  1) Node.js backend R1K1.SYS (вход, профиль, магазин...) — заголовок Authorization: Bearer.
 *  2) Старый TDSrv (чаты) — ключ auth_key в URL, как в оригинальном microtg.
 */
public class ClientManager {
    public interface CredentialsResponse { void onReady(boolean isValid); }
    public interface Response { void onReady(String str); }
    public interface NetworkErrorHandler { void onError(String str, int code); }
    /** code == 0 означает сетевую ошибку (в body — текст ошибки). Вызывается НЕ в UI-потоке. */
    public interface ApiCallback { void onResult(int code, String body); }
    public interface UnauthorizedListener { void onUnauthorized(); }

    private static ClientManager instance;
    private static final String USER_AGENT = "R1K1.SYS/Android";
    private String token;
    private String nodeAddress;
    private String legacyAddress;
    private String legacyKey;
    private NetworkErrorHandler errorHandler;
    private UnauthorizedListener unauthorizedListener;
    private final ExecutorService executorService;

    public static ClientManager getCurrent() {
        return (instance == null) ? (instance = new ClientManager()) : instance;
    }

    private ClientManager() { executorService = Executors.newSingleThreadExecutor(); }

    public void setAuthorizationToken(String token) { this.token = token; }
    public void setNodeAddress(String nodeAddress) { this.nodeAddress = normalizeBase(nodeAddress); }
    public void setLegacyServer(String address, String key) {
        this.legacyAddress = normalizeBase(address);
        this.legacyKey = key;
    }
    public boolean hasLegacyServer() {
        return legacyAddress != null && legacyAddress.length() > 0 && legacyKey != null && legacyKey.length() > 0;
    }
    public void setErrorHandler(NetworkErrorHandler errorHandler) { this.errorHandler = errorHandler; }
    public void setUnauthorizedListener(UnauthorizedListener l) { this.unauthorizedListener = l; }

    /** Без схемы: IP/localhost/адрес с портом -> http (для разработки), иначе https. */
    public static String normalizeBase(String value) {
        if (value == null) return "";
        String base = value.trim();
        if (base.length() == 0) return "";
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            boolean plain = base.matches("^(localhost|\\d{1,3}(\\.\\d{1,3}){3})(:\\d+)?(/.*)?$") || base.contains(":");
            base = (plain ? "http://" : "https://") + base;
        }
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    /** true, если соединение идёт по открытому http (трафик и код входа не шифруются). */
    public static boolean isCleartext(String value) {
        return normalizeBase(value).startsWith("http://");
    }

    /** Достаёт читаемый текст ошибки из JSON {"error":..., "hint":...} или возвращает как есть. */
    public static String errorText(int code, String body) {
        if (code == 0) return "Нет связи с сервером: " + (body == null ? "" : body);
        String text = body == null ? "" : body.trim();
        try {
            JSONObject o = new JSONObject(text);
            String err = o.optString("error", "");
            String hint = o.optString("hint", "");
            if (err.length() > 0) return hint.length() > 0 ? err + "\n" + hint : err;
        } catch (Exception ignored) { }
        return "HTTP " + code + (text.length() > 0 ? ": " + text : "");
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private void execute(final String url, final String method, final String body,
                         final String bearer, final ApiCallback cb) {
        if (url == null || url.length() < 1) throw new IllegalArgumentException("Request can't be null");
        executorService.submit(new Runnable() {
            @Override public void run() {
                HttpURLConnection conn = null;
                int code = 0;
                String result;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(15000);
                    conn.setDoInput(true);
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("User-Agent", USER_AGENT);
                    if (bearer != null && bearer.length() > 0)
                        conn.setRequestProperty("Authorization", "Bearer " + bearer);
                    if (body != null) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        byte[] data = body.getBytes("UTF-8");
                        OutputStream out = conn.getOutputStream();
                        out.write(data);
                        out.flush();
                        out.close();
                    }
                    code = conn.getResponseCode();
                    InputStream stream = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
                    StringBuilder response = new StringBuilder();
                    if (stream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (response.length() > 0) response.append('\n');
                            response.append(line);
                        }
                        reader.close();
                    }
                    result = response.toString();
                } catch (Exception e) {
                    code = 0;
                    result = e.getLocalizedMessage() == null ? e.toString() : e.getLocalizedMessage();
                } finally {
                    if (conn != null) conn.disconnect();
                }
                if (code == 401 && bearer != null && bearer.length() > 0) {
                    UnauthorizedListener l = unauthorizedListener;
                    if (l != null) l.onUnauthorized();
                }
                if (cb != null) cb.onResult(code, result);
            }
        });
    }

    /** Старый стиль: успех -> Response, ошибка -> NetworkErrorHandler. */
    private void request(String url, String method, String body, final String bearer, final Response resp) {
        execute(url, method, body, bearer, new ApiCallback() {
            @Override public void onResult(int code, String text) {
                if (code >= 200 && code < 400) {
                    if (resp != null) resp.onReady(text);
                } else if (code == 401 && bearer != null && bearer.length() > 0) {
                    // сессия истекла — обрабатывает UnauthorizedListener, второй тост не нужен
                } else {
                    reportError(errorText(code, text), code);
                }
            }
        });
    }

    private void reportError(final String message, final int code) {
        final NetworkErrorHandler handler = errorHandler;
        if (handler != null) handler.onError(message, code);
    }

    // ===== Node.js backend: вход =====

    public void requestCode(String node, String phone, ApiCallback cb) {
        try {
            String body = new JSONObject().put("phone", phone).toString();
            execute(normalizeBase(node) + "/auth/request", "POST", body, null, cb);
        } catch (Exception e) { cb.onResult(0, e.toString()); }
    }

    public void verifyCode(String node, String phone, String code, ApiCallback cb) {
        try {
            String body = new JSONObject().put("phone", phone).put("code", code).toString();
            execute(normalizeBase(node) + "/auth/verify", "POST", body, null, cb);
        } catch (Exception e) { cb.onResult(0, e.toString()); }
    }

    // ===== Node.js backend: защищённые вызовы (Bearer) =====

    private String apiUrl(String path) {
        return normalizeBase(nodeAddress) + (path.startsWith("/") ? path : "/" + path);
    }

    public void apiGet(String path, Response resp) { request(apiUrl(path), "GET", null, token, resp); }
    public void apiPost(String path, String jsonBody, Response resp) {
        request(apiUrl(path), "POST", jsonBody == null ? "{}" : jsonBody, token, resp);
    }
    /** То же, но с кодом ответа — чтобы экран мог показать причину ошибки. */
    public void apiGetRaw(String path, ApiCallback cb) { execute(apiUrl(path), "GET", null, token, cb); }

    // ===== Старый TDSrv (чаты) =====

    private String legacyUrl(String path) {
        String url = legacyAddress + (path.startsWith("/") ? path : "/" + path);
        try { url += (url.contains("?") ? "&" : "?") + "auth_key=" + encode(legacyKey); }
        catch (Exception ignored) { }
        return url;
    }

    public void checkCredentialsValidity(String node, String accessKey, final CredentialsResponse resp) {
        String base = normalizeBase(node);
        final String url;
        try { url = base + "/CheckCredentials?auth_key=" + encode(accessKey); }
        catch (Exception e) { resp.onReady(false); return; }
        request(url, "GET", null, null, new Response() {
            @Override public void onReady(String str) {
                resp.onReady("OK".equals(str == null ? "" : str.trim()));
            }
        });
    }

    public void sendRequest(String req, Response resp) { request(req, "GET", null, null, resp); }

    public void queryChats(int count, Response resp) {
        sendRequest(legacyUrl("/QueryChats?count=" + count), resp);
    }
    public void queryMessages(Packets.Chat chat, long count, Response resp) {
        sendRequest(legacyUrl("/QueryMessages?chat_id=" + chat.ID + "&last_message_id=" + chat.MsgId + "&count=" + count), resp);
    }
    public void sendTextMessage(long chat, String msg, long replyTo, Response resp) {
        try { sendRequest(legacyUrl("/SendMessage?chat_id=" + chat + "&text=" + encode(msg) + "&reply_to=" + replyTo), resp); }
        catch (Exception e) { reportError(e.toString(), 0); }
    }
    public String getPhotoUrl(String photo) { return legacyAddress + "/photos/" + photo; }
    public boolean isResponseSucceeded(String response) { return response != null && response.contains("OK"); }
}
