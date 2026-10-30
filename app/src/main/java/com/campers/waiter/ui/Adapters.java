package com.campers.waiter.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.campers.waiter.R;
import com.campers.waiter.data.Fmt;
import com.campers.waiter.data.Models.Product;
import com.campers.waiter.data.Models.Sale;
import com.campers.waiter.data.Models.SaleItem;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.Map;

/** Sell screen rows: name, price/stock and a - qty + stepper. */
class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
    interface Listener { void onChange(Product p, int delta); }

    private final List<Product> items;
    private final Map<String, Integer> cart;
    private final Listener listener;

    ProductAdapter(List<Product> items, Map<String, Integer> cart, Listener listener) {
        this.items = items; this.cart = cart; this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, qty;
        final Button minus, plus;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.name); sub = v.findViewById(R.id.sub); qty = v.findViewById(R.id.qty);
            minus = v.findViewById(R.id.minus); plus = v.findViewById(R.id.plus);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);
        int qty = cart.getOrDefault(p.id, 0);
        boolean sellable = p.stock > 0 && p.sellingPrice > 0;

        h.name.setText(p.name);
        String sub;
        if (p.sellingPrice <= 0) sub = "Price not set";
        else if (p.stock <= 0) sub = "Out of stock";
        else sub = Fmt.money(p.sellingPrice) + "  •  stock " + p.stock;
        h.sub.setText(sub);
        h.sub.setTextColor(ContextCompat.getColor(h.itemView.getContext(), sellable ? R.color.muted : R.color.danger));
        h.qty.setText(String.valueOf(qty));
        h.minus.setEnabled(sellable && qty > 0);
        h.plus.setEnabled(sellable && qty < p.stock);
        h.minus.setOnClickListener(v -> listener.onChange(p, -1));
        h.plus.setOnClickListener(v -> listener.onChange(p, 1));
    }

    @Override public int getItemCount() { return items.size(); }
}

/** Restock screen rows: tap one to choose it. */
class PickAdapter extends RecyclerView.Adapter<PickAdapter.VH> {
    interface Listener { void onPick(Product p); }

    private final List<Product> items;
    private final Listener listener;
    private String selectedId;

    PickAdapter(List<Product> items, Listener listener) { this.items = items; this.listener = listener; }

    void setSelectedId(String id) { selectedId = id; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, stock;
        VH(@NonNull View v) { super(v); name = v.findViewById(R.id.name); stock = v.findViewById(R.id.stock); }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pick, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);
        boolean selected = p.id.equals(selectedId);
        MaterialCardView card = (MaterialCardView) h.itemView;
        card.setStrokeWidth(selected ? UiUtil.dp(card, 2) : 0);
        card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.teal));
        card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), selected ? R.color.selected_bg : R.color.white));
        h.name.setText(p.name);
        h.stock.setText("stock " + p.stock);
        h.itemView.setOnClickListener(v -> listener.onPick(p));
    }

    @Override public int getItemCount() { return items.size(); }
}

/** My Sales rows. */
class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.VH> {
    private final List<Sale> items;

    SaleAdapter(List<Sale> items) { this.items = items; }

    static class VH extends RecyclerView.ViewHolder {
        final TextView time, total, itemsText, note;
        VH(@NonNull View v) {
            super(v);
            time = v.findViewById(R.id.time); total = v.findViewById(R.id.total);
            itemsText = v.findViewById(R.id.items); note = v.findViewById(R.id.note);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Sale s = items.get(position);
        h.time.setText(Fmt.localTime(s.createdAtUtc));
        h.total.setText(Fmt.money(s.total));
        StringBuilder sb = new StringBuilder();
        if (s.items != null) {
            for (SaleItem i : s.items) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(i.quantity).append("× ").append(i.productName);
            }
        }
        h.itemsText.setText(sb.toString());
        if (s.note != null && !s.note.trim().isEmpty()) {
            h.note.setText("📝 " + s.note);
            h.note.setVisibility(View.VISIBLE);
        } else {
            h.note.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return items.size(); }
}
