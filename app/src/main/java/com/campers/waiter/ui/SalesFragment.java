package com.campers.waiter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campers.waiter.R;
import com.campers.waiter.data.Fmt;
import com.campers.waiter.data.Models.Sale;
import com.campers.waiter.data.Net;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.util.ArrayList;
import java.util.List;

public class SalesFragment extends Fragment {
    private final List<Sale> sales = new ArrayList<>();
    private Fmt.Period period = Fmt.Period.TODAY;
    private boolean loading;

    private SaleAdapter adapter;
    private TextView banner, sumCount, sumTotal, empty;
    private View progress;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_sales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        banner = v.findViewById(R.id.banner);
        sumCount = v.findViewById(R.id.sumCount);
        sumTotal = v.findViewById(R.id.sumTotal);
        empty = v.findViewById(R.id.empty);
        progress = v.findViewById(R.id.progress);

        RecyclerView list = v.findViewById(R.id.list);
        adapter = new SaleAdapter(sales);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        MaterialButtonToggleGroup group = v.findViewById(R.id.period);
        group.check(R.id.pToday);
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            period = checkedId == R.id.pWeek ? Fmt.Period.WEEK : checkedId == R.id.pMonth ? Fmt.Period.MONTH : Fmt.Period.TODAY;
            load();
        });
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) load();   // show the sale you just made
    }

    private boolean alive() { return isAdded() && getView() != null; }

    private void load() {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        banner.setVisibility(View.GONE);
        Net.run(Net.api().mySales(period.from(), period.to()), new Net.Result<List<Sale>>() {
            @Override public void onSuccess(List<Sale> list) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                sales.clear();
                sales.addAll(list);
                adapter.notifyDataSetChanged();
                double total = 0;
                for (Sale s : sales) total += s.total;
                sumCount.setText(String.valueOf(sales.size()));
                sumTotal.setText(Fmt.money(total));
                empty.setVisibility(sales.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(String message, int code) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                UiUtil.banner(banner, message, true);
            }
        });
    }
}
