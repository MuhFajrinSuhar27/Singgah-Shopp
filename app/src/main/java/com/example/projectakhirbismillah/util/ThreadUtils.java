package com.example.projectakhirbismillah.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Task<T> {
        T execute() throws Exception;
    }

    public interface Callback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }

    public static <T> void runInBackground(Task<T> task, Callback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.execute();
                mainHandler.post(() -> callback.onComplete(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void runInBackground(Runnable runnable) {
        executor.execute(runnable);
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
}