package com.campers.waiter.data;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Fmt {
    private Fmt() {}

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    public static String money(double v) { return new DecimalFormat("#,##0.##").format(v); }

    /** Server sends UTC; show the waiter's own local time. */
    public static String localTime(String utc) {
        try { return TIME.format(Instant.parse(utc).atZone(ZoneId.systemDefault())); }
        catch (Exception e) { return utc; }
    }

    public enum Period {
        TODAY(0), WEEK(6), MONTH(29);

        private final int daysBack;
        Period(int daysBack) { this.daysBack = daysBack; }

        /** "Today" sends no dates so the server uses its own 06:00-06:00 business day. */
        public String from() { return daysBack == 0 ? null : LocalDate.now().minusDays(daysBack).toString(); }
        public String to() { return daysBack == 0 ? null : LocalDate.now().toString(); }
    }
}
