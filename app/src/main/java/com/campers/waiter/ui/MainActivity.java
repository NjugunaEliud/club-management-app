package com.campers.waiter.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.campers.waiter.R;
import com.campers.waiter.data.Net;
import com.campers.waiter.data.SessionStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String[] TAGS = {"sell", "restock", "sales", "stats"};

    private SessionStore store;
    private int selected = 0;
    private boolean leaving = false;
    private final Fragment[] frags = new Fragment[4];

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SessionStore(this);

        SessionStore.Session s = store.load();
        if (s == null || s.isExpired()) { goLogin(null); return; }
        Net.setSession(s.baseUrl, s.token);   // also restores the token after Android killed the process
        Net.setOnUnauthorized(() -> {
            store.clearLogin();
            Net.clearSession();
            goLogin("Your session expired. Please sign in again.");
        });

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Campers Nature Nest");
        toolbar.setSubtitle(s.fullName);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                store.clearLogin();
                Net.clearSession();
                goLogin(null);
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) selected = savedInstanceState.getInt("selected", 0);

        // All four screens live at once (show/hide) so a half-built cart survives tab switches.
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null) {
                f = create(i);
                tx.add(R.id.container, f, TAGS[i]);
            }
            frags[i] = f;
            if (i == selected) tx.show(f); else tx.hide(f);
        }
        tx.commit();

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.getMenu().getItem(selected).setChecked(true);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            select(id == R.id.nav_sell ? 0 : id == R.id.nav_restock ? 1 : id == R.id.nav_sales ? 2 : 3);
            return true;
        });
    }

    private Fragment create(int i) {
        switch (i) {
            case 0: return new SellFragment();
            case 1: return new RestockFragment();
            case 2: return new SalesFragment();
            default: return new StatsFragment();
        }
    }

    private void select(int idx) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < frags.length; i++) {
            if (i == idx) tx.show(frags[i]); else tx.hide(frags[i]);
        }
        tx.commit();
        selected = idx;
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("selected", selected);
    }

    @Override protected void onDestroy() {
        Net.setOnUnauthorized(null);
        super.onDestroy();
    }

    private void goLogin(String message) {
        if (leaving) return;
        leaving = true;
        Intent i = new Intent(this, LoginActivity.class);
        if (message != null) i.putExtra("message", message);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
