package com.campers.waiter.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;

/** Keeps the login on the phone so waiters aren't asked to sign in again mid-shift. */
public class SessionStore {
    public static final String DEFAULT_URL = "http://41.89.227.170:8082/";   // 10.0.2.2 = your PC when using the emulator

    public static class Session {
        public String baseUrl, token, fullName, role, expiresAtUtc;

        public Session(String baseUrl, String token, String fullName, String role, String expiresAtUtc) {
            this.baseUrl = baseUrl; this.token = token; this.fullName = fullName; this.role = role; this.expiresAtUtc = expiresAtUtc;
        }

        public boolean isExpired() {
            try { return Instant.parse(expiresAtUtc).isBefore(Instant.now()); }
            catch (Exception e) { return true; }
        }
    }

    private final SharedPreferences sp;

    public SessionStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public Session load() {
        String token = sp.getString("token", null);
        if (token == null) return null;
        return new Session(sp.getString("url", DEFAULT_URL), token, sp.getString("name", ""),
                sp.getString("role", ""), sp.getString("exp", ""));
    }

    public void save(Session s) {
        sp.edit().putString("url", s.baseUrl).putString("token", s.token).putString("name", s.fullName)
                .putString("role", s.role).putString("exp", s.expiresAtUtc).apply();
    }

    /** Signs out but remembers the server address. */
    public void clearLogin() {
        sp.edit().remove("token").remove("name").remove("role").remove("exp").apply();
    }

    public String lastUrl() { return sp.getString("url", DEFAULT_URL); }
}
