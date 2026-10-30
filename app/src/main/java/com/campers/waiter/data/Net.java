package com.campers.waiter.data;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** One shared HTTP client: adds the bearer token and tells the app when the server says 401. */
public final class Net {
    private Net() {}

    public interface Result<T> {
        void onSuccess(T data);
        void onError(String message, int httpCode);   // httpCode 0 = network problem
    }

    private static volatile String token;
    private static volatile String baseUrl = "";
    private static volatile Runnable onUnauthorized;
    private static String cachedUrl;
    private static ApiService cachedApi;

    private static final Gson GSON = new Gson();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request.Builder b = chain.request().newBuilder();
                String t = token;
                if (t != null) b.header("Authorization", "Bearer " + t);
                Request req = b.build();
                okhttp3.Response res = chain.proceed(req);
                if (res.code() == 401 && t != null && !req.url().encodedPath().endsWith("auth/login")) {
                    Runnable r = onUnauthorized;
                    if (r != null) MAIN.post(r);
                }
                return res;
            })
            .build();

    public static void setSession(String url, String bearer) { baseUrl = url; token = bearer; }
    public static void clearSession() { token = null; }
    public static void setOnUnauthorized(Runnable r) { onUnauthorized = r; }

    public static String normalize(String input) {
        String s = input == null ? "" : input.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://" + s;
        if (!s.endsWith("/")) s += "/";
        return s;
    }

    public static ApiService api() { return api(baseUrl); }

    public static synchronized ApiService api(String url) {
        String base = normalize(url);
        if (cachedApi == null || !base.equals(cachedUrl)) {
            cachedApi = new Retrofit.Builder()
                    .baseUrl(base)
                    .client(CLIENT)
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build()
                    .create(ApiService.class);
            cachedUrl = base;
        }
        return cachedApi;
    }

    /** Runs a call; the callback always arrives on the main thread. */
    public static <T> void run(Call<T> call, Result<T> cb) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError(httpMessage(r), r.code());
            }
            @Override public void onFailure(Call<T> c, Throwable t) { cb.onError(friendly(t), 0); }
        });
    }

    private static String httpMessage(Response<?> r) {
        String body = null;
        try { if (r.errorBody() != null) body = r.errorBody().string(); } catch (IOException ignored) { }
        if (body != null) {
            try {
                Models.ApiError e = GSON.fromJson(body, Models.ApiError.class);
                if (e != null && e.error != null) return e.error;
                if (e != null && e.title != null) return e.title;
            } catch (Exception ignored) { }
        }
        switch (r.code()) {
            case 401: return "Wrong username or password, or your session expired.";
            case 403: return "You are not allowed to do that.";
            case 404: return "Not found on the server. Is the server address right?";
            case 429: return "Too many attempts. Wait a minute and try again.";
            default:  return "Server error (" + r.code() + ").";
        }
    }

    /** Turns any failure into a sentence a waiter can act on. */
    public static String friendly(Throwable t) {
        if (t instanceof UnknownHostException || t instanceof ConnectException || t instanceof SocketTimeoutException)
            return "Can't reach the server. Check the Wi-Fi and the server address.";
        if (t instanceof JsonParseException)
            return "The server replied with something unexpected. Check the server address.";
        if (t instanceof IOException) return "Network problem. Check your connection and try again.";
        return t.getMessage() != null ? t.getMessage() : "Something went wrong.";
    }
}
