package com.campers.waiter.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.campers.waiter.R;

final class UiUtil {
    private UiUtil() {}

    /** Centered message strip; tap it to dismiss. */
    static void banner(TextView tv, String msg, boolean error) {
        tv.setText(msg);
        tv.setBackgroundResource(error ? R.drawable.bg_banner_err : R.drawable.bg_banner_ok);
        tv.setTextColor(error ? 0xFFA93226 : 0xFF1F4A32);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(15);
        tv.setPadding(24, 20, 24, 20);
        tv.setVisibility(View.VISIBLE);
        tv.setOnClickListener(v -> v.setVisibility(View.GONE));
    }

    static String text(TextView tv) {
        CharSequence c = tv.getText();
        return c == null ? "" : c.toString();
    }

    static int dp(View v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }
}

/** TextWatcher that just runs something when the text changes. */
class SimpleWatcher implements TextWatcher {
    private final Runnable onChange;
    SimpleWatcher(Runnable onChange) { this.onChange = onChange; }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.run(); }
    @Override public void afterTextChanged(Editable s) { }
}
