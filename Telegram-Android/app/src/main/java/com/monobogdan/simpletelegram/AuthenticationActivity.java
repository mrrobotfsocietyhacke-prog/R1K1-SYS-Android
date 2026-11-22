package com.monobogdan.simpletelegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.monobogdan.simpletelegram.transport.ClientManager;
import com.monobogdan.simpletelegram.ui.Ui;
import com.monobogdan.simpletelegram.utils.SoundManager;

import org.json.JSONObject;

public class AuthenticationActivity extends Activity {
    private static final int CODE_LENGTH = 5;
    private static final int RESEND_SECONDS = 60;

    private EditText serverField, phoneField, codeField;
    private View stepPhone, stepCode;
    private Button requestButton, verifyButton, resendButton;
    private TextView statusView, codeInfo;
    private final TextView[] cells = new TextView[CODE_LENGTH];
    private CountDownTimer timer;
    private boolean verifying;

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

    private String cleartextWarning() {
        return ClientManager.isCleartext(serverValue())
            ? "\n\u26A0 Соединение без шифрования (http). Только для теста, для реальной сети нужен https."
            : "";
    }

    // ===== шаг 1: номер =====

    public void onRequestCode(View sender) { requestCode(false); }

    public void onResend(View sender) { requestCode(true); }

    private void requestCode(final boolean resend) {
        if (!validateServerAndPhone()) return;
        requestButton.setEnabled(false);
        resendButton.setEnabled(false);
        setStatus("Запрашиваю код…");
        ClientManager.getCurrent().requestCode(serverValue(), phoneValue(), new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        requestButton.setEnabled(true);
                        if (code == 200) {
                            String message = "";
                            try {
                                JSONObject o = new JSONObject(body);
                                if (!o.optBoolean("telegram_sent", true)) message = o.optString("message", "");
                            } catch (Exception ignored) { }
                            showCodeStep(message + cleartextWarning());
                        } else {
                            resendButton.setEnabled(true);
                            setStatus(ClientManager.errorText(code, body));
                        }
                    }
                });
            }
        });
    }

    // ===== шаг 2: код =====

    private void showCodeStep(String extra) {
        codeInfo.setText("Код отправлен ботом R1K1.SYS в Telegram.\nНомер: " + phoneValue());
        setStatus(extra == null ? "" : extra.trim());
        codeField.setText("");
        if (stepCode.getVisibility() != View.VISIBLE) {
            stepPhone.setVisibility(View.GONE);
            stepCode.setAlpha(0f);
            stepCode.setVisibility(View.VISIBLE);
            stepCode.animate().alpha(1f).setDuration(300).start();
        }
        codeField.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(codeField, InputMethodManager.SHOW_IMPLICIT);
        startTimer();
    }

    public void onChangeNumber(View sender) {
        if (timer != null) timer.cancel();
        stepCode.setVisibility(View.GONE);
        stepPhone.setAlpha(0f);
        stepPhone.setVisibility(View.VISIBLE);
        stepPhone.animate().alpha(1f).setDuration(300).start();
        setStatus("");
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        resendButton.setEnabled(false);
        timer = new CountDownTimer(RESEND_SECONDS * 1000L, 1000) {
            @Override public void onTick(long left) {
                resendButton.setText("Отправить повторно через " + (left / 1000 + 1) + " с");
            }
            @Override public void onFinish() {
                resendButton.setText("Отправить код снова");
                resendButton.setEnabled(true);
            }
        };
        timer.start();
    }

    private void updateCells(String value) {
        for (int i = 0; i < CODE_LENGTH; i++) {
            boolean filled = i < value.length();
            cells[i].setText(filled ? String.valueOf(value.charAt(i)) : "");
            boolean current = i == value.length();
            Ui.setBg(cells[i], Ui.round(Ui.CARD, 12, this));
            android.graphics.drawable.GradientDrawable d = Ui.round(Color.rgb(10, 10, 16), 12, this);
            d.setStroke(Ui.dp(this, current || filled ? 2 : 1), current ? Ui.VIOLET : (filled ? Ui.ACTIVE : Ui.LINE));
            Ui.setBg(cells[i], d);
        }
    }

    public void onVerify(View sender) { verify(); }

    private void verify() {
        if (verifying) return;
        if (!validateServerAndPhone()) return;
        final String codeValue = codeField.getText().toString().trim();
        if (codeValue.length() < CODE_LENGTH) {
            setStatus("Введи " + CODE_LENGTH + " цифр кода.");
            return;
        }
        verifying = true;
        verifyButton.setEnabled(false);
        setStatus("Проверяю код…");
        ClientManager.getCurrent().verifyCode(serverValue(), phoneValue(), codeValue, new ClientManager.ApiCallback() {
            @Override public void onResult(final int code, final String body) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        verifying = false;
                        verifyButton.setEnabled(true);
                        if (code != 200) {
                            setStatus(ClientManager.errorText(code, body));
                            codeField.setText("");
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
                        if (timer != null) timer.cancel();
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
                "3. Код придёт в чат с ботом. Он введётся сам, как только набраны все цифры.\n\n" +
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
        stepPhone = findViewById(R.id.step_phone);
        stepCode = findViewById(R.id.step_code);
        requestButton = (Button) findViewById(R.id.btn_request);
        verifyButton = (Button) findViewById(R.id.btn_verify);
        resendButton = (Button) findViewById(R.id.btn_resend);
        statusView = (TextView) findViewById(R.id.status);
        codeInfo = (TextView) findViewById(R.id.code_info);

        LinearLayout cellRow = (LinearLayout) findViewById(R.id.cells);
        for (int i = 0; i < CODE_LENGTH; i++) {
            cells[i] = Ui.text(this, "", 26, Color.WHITE, true);
            cells[i].setGravity(Gravity.CENTER);
            cellRow.addView(cells[i], Ui.margins(Ui.lp(Ui.dp(this, 46), Ui.dp(this, 56)), this, 4, 0, 4, 0));
        }
        updateCells("");
        codeField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                String v = e.toString();
                updateCells(v);
                if (v.length() == CODE_LENGTH) verify();
            }
        });

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

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
