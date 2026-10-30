package com.campers.waiter.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campers.waiter.R;
import com.campers.waiter.data.Models;
import com.campers.waiter.data.Net;
import com.campers.waiter.data.SessionStore;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private SessionStore store;
    private TextInputEditText inputUser, inputPass;
    private TextView status;
    private Button btnLogin;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SessionStore(this);

        SessionStore.Session s = store.load();
        if (s != null && !s.isExpired()) { startMain(); return; }   // already signed in

        setContentView(R.layout.activity_login);
        inputUser = findViewById(R.id.inputUser);
        inputPass = findViewById(R.id.inputPass);
        status = findViewById(R.id.status);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> doLogin());

        String msg = getIntent().getStringExtra("message");
        if (msg != null) showStatus(msg, true);
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showStatus(String msg, boolean error) {
        status.setText(msg);
        status.setTextColor(getColor(error ? R.color.danger : R.color.forest));
        status.setVisibility(android.view.View.VISIBLE);
    }

    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Please wait…" : "Sign In");
    }

    private void doLogin() {
        String base = store.lastUrl();
        String user = UiUtil.text(inputUser).trim();
        String pass = UiUtil.text(inputPass);
        if (user.isEmpty() || pass.isEmpty()) {
            showStatus("Enter your username and password.", true);
            return;
        }
        setBusy(true);
        status.setVisibility(android.view.View.GONE);
        Net.run(Net.api(base).login(new Models.LoginRequest(user, pass)), new Net.Result<Models.LoginResponse>() {
            @Override public void onSuccess(Models.LoginResponse r) {
                if (isFinishing()) return;
                store.save(new SessionStore.Session(base, r.token, r.user.fullName, r.user.role, r.expiresAtUtc));
                startMain();
            }
            @Override public void onError(String message, int httpCode) {
                if (isFinishing()) return;
                setBusy(false);
                showStatus(message, true);
            }
        });
    }
}
