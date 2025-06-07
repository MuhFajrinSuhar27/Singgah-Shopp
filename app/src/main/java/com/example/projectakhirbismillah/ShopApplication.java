package com.example.projectakhirbismillah;

import android.app.Application;
import android.util.Log;

public class ShopApplication extends Application {
    private static final String TAG = "ShopApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application starting");

        // Hapus atau komentari baris ini karena metode initMidtrans tidak ada
        // MidtransHelper.initMidtrans(this);

        // Atau, jika masih ingin melakukan logging
        Log.d(TAG, "Midtrans will initialize when needed");
    }
}