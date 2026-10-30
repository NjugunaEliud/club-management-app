package com.campers.waiter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.campers.waiter.R;
import com.campers.waiter.data.Fmt;
import com.campers.waiter.data.Models.DayTotal;
import com.campers.waiter.data.Models.MyStats;
import com.campers.waiter.data.Models.ProductStat;
import com.campers.waiter.data.Net;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class StatsFragment extends Fragment {
    private Fmt.Period period = Fmt.Period.TODAY;
    private boolean loading;

    private TextView banner, statRevenue, statSales, statUnits, bestName, bestDetail;
    private View progress, bestCard;
    private LinearLayout topList, dayList;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        banner = v.findViewById(R.id.banner);
        statRevenue = v.findViewById(R.id.statRevenue);
        statSales = v.findViewById(R.id.statSales);
        statUnits = v.findViewById(R.id.statUnits);
        bestName = v.findViewById(R.id.bestName);
        bestDetail = v.findViewById(R.id.bestDetail);
        bestCard = v.findViewById(R.id.bestCard);
        progress = v.findViewById(R.id.progress);
        topList = v.findViewById(R.id.topList);
        dayList = v.findViewById(R.id.dayList);

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
        if (!hidden && getView() != null) load();
    }

    private boolean alive() { return isAdded() && getView() != null; }

    private void load() {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        banner.setVisibility(View.GONE);
        Net.run(Net.api().stats(period.from(), period.to()), new Net.Result<MyStats>() {
            @Override public void onSuccess(MyStats s) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                render(s);
            }
            @Override public void onError(String message, int code) {
                loading = false;
                if (!alive()) return;
                progress.setVisibility(View.GONE);
                UiUtil.banner(banner, message, true);
            }
        });
    }

    private void render(MyStats s) {
        statRevenue.setText(Fmt.money(s.revenue));
        statSales.setText(String.valueOf(s.salesCount));
        statUnits.setText(String.valueOf(s.unitsSold));

        topList.removeAllViews();
        dayList.removeAllViews();

        if (s.topProducts != null && !s.topProducts.isEmpty()) {
            ProductStat best = s.topProducts.get(0);
            bestCard.setVisibility(View.VISIBLE);
            bestName.setText(best.product);
            bestDetail.setText(best.quantity + " sold  •  " + Fmt.money(best.revenue) + "  •  "
                    + Fmt.money(best.sharePercent) + "% of your units");

            int max = 1;
            for (ProductStat p : s.topProducts) max = Math.max(max, p.quantity);
            for (ProductStat p : s.topProducts) addBar(topList, p.product, String.valueOf(p.quantity), 100.0 * p.quantity / max);
        } else {
            bestCard.setVisibility(View.GONE);
            addNote(topList);
        }

        if (s.perDay != null && !s.perDay.isEmpty()) {
            double max = 1;
            for (DayTotal d : s.perDay) max = Math.max(max, d.revenue);
            for (DayTotal d : s.perDay) addBar(dayList, d.date, Fmt.money(d.revenue), 100.0 * d.revenue / max);
        } else {
            addNote(dayList);
        }
    }

    private void addBar(LinearLayout parent, String label, String value, double percent) {
        View row = getLayoutInflater().inflate(R.layout.item_bar, parent, false);
        ((TextView) row.findViewById(R.id.label)).setText(label);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        LinearProgressIndicator bar = row.findViewById(R.id.bar);
        bar.setProgressCompat((int) Math.max(2, Math.round(percent)), false);
        parent.addView(row);
    }

    private void addNote(LinearLayout parent) {
        TextView t = new TextView(requireContext());
        t.setText("No sales yet.");
        t.setTextColor(requireContext().getColor(R.color.muted));
        t.setPadding(UiUtil.dp(parent, 16), UiUtil.dp(parent, 6), UiUtil.dp(parent, 16), UiUtil.dp(parent, 6));
        parent.addView(t);
    }
}
