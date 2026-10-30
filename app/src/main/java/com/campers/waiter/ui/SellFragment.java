package com.campers.waiter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campers.waiter.R;
import com.campers.waiter.data.Fmt;
import com.campers.waiter.data.Models.CreateSale;
import com.campers.waiter.data.Models.Product;
import com.campers.waiter.data.Models.Sale;
import com.campers.waiter.data.Models.SaleLine;
import com.campers.waiter.data.Net;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SellFragment extends Fragment {
    private final List<Product> all = new ArrayList<>();
    private final List<Product> shown = new ArrayList<>();
    private final Map<String, Integer> cart = new LinkedHashMap<>();

    // Sent with the sale. If the network drops after the server saved it and the waiter taps SELL again,
    // the server recognises this id and returns the same sale instead of selling twice.
    private String cartId = UUID.randomUUID().toString();

    private boolean loading, submitting;
    private ProductAdapter adapter;
    private TextInputEditText search, note;
    private TextView banner, cartCount, cartTotal;
    private Button btnSell;
    private View progress;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        search = v.findViewById(R.id.search);
        note = v.findViewById(R.id.note);
        banner = v.findViewById(R.id.banner);
        cartCount = v.findViewById(R.id.cartCount);
        cartTotal = v.findViewById(R.id.cartTotal);
        btnSell = v.findViewById(R.id.btnSell);
        progress = v.findViewById(R.id.progress);

        RecyclerView list = v.findViewById(R.id.list);
        adapter = new ProductAdapter(shown, cart, this::change);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new SimpleWatcher(this::applyFilter));
        v.findViewById(R.id.btnRefresh).setOnClickListener(x -> load());
        btnSell.setOnClickListener(x -> submit());

        updateCart();
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) load();   // stock may have changed while this tab was hidden
    }

    private boolean alive() { return isAdded() && getView() != null; }

    private void load() {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        Net.run(Net.api().products(), new Net.Result<List<Product>>() {
            @Override public void onSuccess(List<Product> list) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                all.clear();
                all.addAll(list);
                clampCart();
                applyFilter();
                updateCart();
            }
            @Override public void onError(String message, int code) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                UiUtil.banner(banner, message, true);
            }
        });
    }

    /** After a refresh, never keep more in the basket than is now in stock. */
    private void clampCart() {
        for (Iterator<Map.Entry<String, Integer>> it = cart.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> e = it.next();
            Product p = find(e.getKey());
            if (p == null || p.stock <= 0) { it.remove(); continue; }
            if (e.getValue() > p.stock) e.setValue(p.stock);
        }
    }

    private Product find(String id) {
        for (Product p : all) if (p.id.equals(id)) return p;
        return null;
    }

    private void applyFilter() {
        String q = UiUtil.text(search).trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (Product p : all) {
            if (q.isEmpty() || p.name.toLowerCase(Locale.getDefault()).contains(q)) shown.add(p);
        }
        adapter.notifyDataSetChanged();
    }

    private void change(Product p, int delta) {
        int cur = cart.containsKey(p.id) ? cart.get(p.id) : 0;
        int n = Math.max(0, Math.min(p.stock, cur + delta));
        if (n == cur) return;
        cartId = UUID.randomUUID().toString();   // a different basket is a different sale
        if (n == 0) cart.remove(p.id); else cart.put(p.id, n);
        banner.setVisibility(View.GONE);
        int pos = shown.indexOf(p);
        if (pos >= 0) adapter.notifyItemChanged(pos); else adapter.notifyDataSetChanged();
        updateCart();
    }

    private void updateCart() {
        int count = 0;
        double total = 0;
        for (Product p : all) {
            Integer q = cart.get(p.id);
            if (q != null) { count += q; total += q * p.sellingPrice; }
        }
        cartCount.setText(count + " item(s)");
        cartTotal.setText("Total  " + Fmt.money(total));
        btnSell.setEnabled(count > 0 && !submitting);
    }

    private void submit() {
        if (submitting) return;
        List<SaleLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            if (e.getValue() > 0) lines.add(new SaleLine(e.getKey(), e.getValue()));
        }
        if (lines.isEmpty()) { UiUtil.banner(banner, "Add at least one item.", true); return; }

        CreateSale req = new CreateSale();
        req.clientRequestId = cartId;
        String n = UiUtil.text(note).trim();
        req.note = n.isEmpty() ? null : n;
        req.items = lines;

        submitting = true;
        btnSell.setText("Selling…");
        updateCart();
        Net.run(Net.api().sell(req), new Net.Result<Sale>() {
            @Override public void onSuccess(Sale sale) {
                submitting = false;
                if (!alive()) return;
                cart.clear();
                cartId = UUID.randomUUID().toString();
                note.setText("");
                btnSell.setText("SELL");
                UiUtil.banner(banner, "Sold ✔  Total " + Fmt.money(sale.total), false);
                adapter.notifyDataSetChanged();
                updateCart();
                load();
            }
            @Override public void onError(String message, int code) {
                submitting = false;
                if (!alive()) return;
                btnSell.setText("SELL");
                UiUtil.banner(banner, message, true);
                updateCart();
                if (code == 409) load();   // stock changed: refresh the numbers
            }
        });
    }
}
