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
import com.campers.waiter.data.Models.Product;
import com.campers.waiter.data.Models.RestockRequest;
import com.campers.waiter.data.Net;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RestockFragment extends Fragment {
    private final List<Product> all = new ArrayList<>();
    private final List<Product> shown = new ArrayList<>();
    private String selectedId;
    private boolean loading, submitting;

    private PickAdapter adapter;
    private TextInputEditText search, qty, note;
    private TextView banner, selectedLabel;
    private Button btnAdd;
    private View progress;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_restock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        search = v.findViewById(R.id.search);
        qty = v.findViewById(R.id.qty);
        note = v.findViewById(R.id.note);
        banner = v.findViewById(R.id.banner);
        selectedLabel = v.findViewById(R.id.selected);
        btnAdd = v.findViewById(R.id.btnAdd);
        progress = v.findViewById(R.id.progress);

        RecyclerView list = v.findViewById(R.id.list);
        adapter = new PickAdapter(shown, p -> {
            selectedId = p.id;
            adapter.setSelectedId(p.id);
            selectedLabel.setText("Adding to: " + p.name);
            banner.setVisibility(View.GONE);
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new SimpleWatcher(this::applyFilter));
        v.findViewById(R.id.btnRefresh).setOnClickListener(x -> load());
        btnAdd.setOnClickListener(x -> submit());
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) load();
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
                applyFilter();
            }
            @Override public void onError(String message, int code) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                UiUtil.banner(banner, message, true);
            }
        });
    }

    private void applyFilter() {
        String q = UiUtil.text(search).trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (Product p : all) {
            if (q.isEmpty() || p.name.toLowerCase(Locale.getDefault()).contains(q)) shown.add(p);
        }
        adapter.notifyDataSetChanged();
    }

    private void submit() {
        if (submitting) return;
        if (selectedId == null) { UiUtil.banner(banner, "Pick a product first.", true); return; }
        int q;
        try { q = Integer.parseInt(UiUtil.text(qty).trim()); } catch (NumberFormatException e) { q = 0; }
        if (q <= 0) { UiUtil.banner(banner, "Enter how many bottles you added.", true); return; }

        RestockRequest req = new RestockRequest();
        req.productId = selectedId;
        req.quantity = q;
        String n = UiUtil.text(note).trim();
        req.note = n.isEmpty() ? null : n;

        final int added = q;
        submitting = true;
        btnAdd.setEnabled(false);
        btnAdd.setText("Saving…");
        Net.run(Net.api().restock(req), new Net.Result<Product>() {
            @Override public void onSuccess(Product p) {
                submitting = false;
                if (!alive()) return;
                btnAdd.setEnabled(true);
                btnAdd.setText("Add stock");
                for (int i = 0; i < all.size(); i++) if (all.get(i).id.equals(p.id)) all.set(i, p);
                qty.setText("");
                note.setText("");
                applyFilter();
                UiUtil.banner(banner, "Added " + added + " to " + p.name + ". Stock is now " + p.stock + ".", false);
            }
            @Override public void onError(String message, int code) {
                submitting = false;
                if (!alive()) return;
                btnAdd.setEnabled(true);
                btnAdd.setText("Add stock");
                UiUtil.banner(banner, message, true);
            }
        });
    }
}
