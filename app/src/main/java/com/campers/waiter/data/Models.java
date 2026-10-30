package com.campers.waiter.data;

import java.util.List;

/** JSON shapes of the ASP.NET API (see docs/API.md). Field names match the JSON; Gson fills them in. */
public final class Models {
    private Models() {}

    public static class LoginRequest {
        public String username, password;
        public LoginRequest(String username, String password) { this.username = username; this.password = password; }
    }
    public static class ApiUser { public String id, fullName, username, role; }
    public static class LoginResponse { public String token, expiresAtUtc; public ApiUser user; }
    public static class Ping { public boolean ok; public String serverTimeUtc; }

    public static class Product {
        public String id, name;
        public double sellingPrice;
        public int stock;
    }

    public static class SaleLine {
        public String productId;
        public int quantity;
        public SaleLine(String productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    }
    public static class CreateSale {
        public String clientRequestId, note;
        public List<SaleLine> items;
    }
    public static class SaleItem {
        public String productId, productName;
        public int quantity;
        public double unitPrice, lineTotal;
    }
    public static class Sale {
        public String id, waiterId, waiterName, createdAtUtc, note;
        public double total;
        public List<SaleItem> items;
    }

    public static class RestockRequest {
        public String productId, note;
        public int quantity;
    }

    public static class ProductStat { public String product; public int quantity; public double revenue, sharePercent; }
    public static class DayTotal { public String date; public int salesCount; public double revenue; }
    public static class MyStats {
        public String from, to;
        public int salesCount, unitsSold;
        public double revenue;
        public List<ProductStat> topProducts;
        public List<DayTotal> perDay;
    }

    public static class ApiError { public String error, title; }
}
