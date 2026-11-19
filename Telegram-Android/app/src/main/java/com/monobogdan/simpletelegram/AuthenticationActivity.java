package com.monobogdan.simpletelegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.monobogdan.simpletelegram.transport.ClientManager;
import com.monobogdan.simpletelegram.utils.SoundManager;

import org.json.JSONObject;

public class AuthenticationActivity extends Activity {
    private EditText serverField;
    private EditText phoneField;
    private EditText codeField;
    private View codeBlock;
    private Button requestButton;
    private Button verifyButton;
    private TextView statusView;

    private void openMainActivity() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setStatus(String text) { statusView.setText(text); }

    private String serverValue() { return serverField.getText().toString().trim(); }
    private String phoneValue() { return phoneField.getText().toString().trim(); }

    private boolean validateServerAndPhone() {
        if (serverValue().length() < 1 || phoneValue().length() < 1) {
            setStatus("Заполни адрес сервера и номер телефона.");
            return false;
        }
        return true;
    }

    /** Предупреждение, если трафик пойдёт по открытому http. */
    private String cleartextWarning() {
        return ClientManager.isCleartext(serverValue())
            ? "\n⚠ Соединение без шифрования (http). Используй только для теста, в реальной сети нужен https."
            : "";
    }

    public void onRequestCode(View sender) {
        if (!validateServerAndPhone()) return;
        requestButton.setEnabled(false);
        setStatus("Запрашиваю код…");

        ClientManager.getCurrent().requestCode(serverValue(), phoneValue(), new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        requestButton.setEnabled(true);
                        if (code == 200) {
                            String message = "Код отправлен в Telegram. Введи его ниже.";
                            try {
                                JSONObject o = new JSONObject(body);
                                if (!o.optBoolean("telegram_sent", true)) message = o.optString("message", message);
                            } catch (Exception ignored) { }
                            if (codeBlock.getVisibility() != View.VISIBLE) {
                                codeBlock.setAlpha(0f);
                                codeBlock.setVisibility(View.VISIBLE);
                                codeBlock.animate().alpha(1f).setDuration(350).start();
                            }
                            codeField.requestFocus();
                            setStatus(message + cleartextWarning());
                        } else {
                            setStatus(ClientManager.errorText(code, body));
                        }
                    }
                });
            }
        });
    }

    public void onVerify(View sender) {
        if (!validateServerAndPhone()) return;
        final String codeValue = codeField.getText().toString().trim();
        if (codeValue.length() < 1) {
            setStatus("Введи код из Telegram.");
            return;
        }
        verifyButton.setEnabled(false);
        setStatus("Проверяю код…");

        ClientManager.getCurrent().verifyCode(serverValue(), phoneValue(), codeValue, new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        verifyButton.setEnabled(true);
                        if (code != 200) {
                            setStatus(ClientManager.errorText(code, body));
                            return;
                        }
                        String token = null;
                        try { token = new JSONObject(body).optString("token", null); }
                        catch (Exception ignored) { }
                        if (token == null || token.length() == 0) {
                            setStatus("Сервер не вернул токен сессии.");
                            return;
                        }
                        Configuration.ApplicationConfiguration cfg = Configuration.getInstance().getApplicationConfiguration();
                        cfg.nodeAddress = ClientManager.normalizeBase(serverValue());
                        cfg.token = token;
                        Configuration.getInstance().save();
                        openMainActivity();
                    }
                });
            }
        });
    }

    public void onViewHelp(View sender) {
        new AlertDialog.Builder(this)
            .setTitle("Как войти")
            .setMessage("1. В Telegram-боте R1K1.SYS получи номер (\"Мои номера\").\n" +
                "2. Введи здесь адрес сервера и этот номер, нажми \"Получить код\".\n" +
                "3. Код придёт в чат с ботом. Введи его и нажми \"Войти\".\n\n" +
                "Если код не пришёл: в боте \"Мои номера\" → \"Получить код входа\".")
            .setPositiveButton("OK", null)
            .show();
    }

    // TODO: Move to other place. Right now AuthActivity is used because it's first activity to load.
    private void initializeStaticResources() {
        SoundManager.initialize(getApplicationContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeStaticResources();

        Configuration.initialize(getApplicationContext());

        if(Configuration.getInstance().isAuthorized()) {
            openMainActivity();

            return;
        }

        setContentView(R.layout.auth_activity);
        serverField = (EditText) findViewById(R.id.server);
        phoneField = (EditText) findViewById(R.id.phone);
        codeField = (EditText) findViewById(R.id.code);
        codeBlock = findViewById(R.id.code_block);
        requestButton = (Button) findViewById(R.id.btn_request);
        verifyButton = (Button) findViewById(R.id.btn_verify);
        statusView = (TextView) findViewById(R.id.status);

        // Градиент красный -> фиолетовый на логотипе
        final TextView logo = (TextView) findViewById(R.id.logo);
        logo.post(new Runnable() {
            @Override public void run() {
                float w = Math.max(1f, logo.getPaint().measureText(logo.getText().toString()));
                logo.getPaint().setShader(new LinearGradient(0, 0, w, 0,
                    Color.parseColor("#FF2341"), Color.parseColor("#B026FF"), Shader.TileMode.CLAMP));
                logo.invalidate();
            }
        });

        String lastNode = Configuration.getInstance().getApplicationConfiguration().nodeAddress;
        if (lastNode != null) serverField.setText(lastNode);
    }
}
