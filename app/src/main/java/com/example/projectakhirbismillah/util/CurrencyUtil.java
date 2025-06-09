package com.example.projectakhirbismillah.util;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtil {
    // Kurs Dollar ke Rupiah (1 USD = 15,500 IDR)
    private static final double USD_TO_IDR_RATE = 15500.0;

    // Format mata uang ke Rupiah dengan pembulatan ke ribuan terdekat tanpa desimal
    public static String formatToRupiah(double priceInUSD) {
        // Konversi ke Rupiah
        double priceInIDR = priceInUSD * USD_TO_IDR_RATE;

        // Pembulatan ke ribuan terdekat (untuk menghasilkan angka yang lebih "enak dilihat")
        long roundedPrice = Math.round(priceInIDR / 1000) * 1000;

        // Format sebagai mata uang Rupiah
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0); // Menghilangkan angka desimal
        
        return formatRupiah.format(roundedPrice);
    }

    // Format mata uang ke Rupiah tanpa pembulatan dan tanpa desimal
    public static String formatToRupiahExact(double priceInIDR) {
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0); // Menghilangkan angka desimal
        
        return formatRupiah.format(priceInIDR);
    }

    // Mendapatkan harga dalam IDR yang sudah dibulatkan ke ribuan terdekat (untuk kalkulasi)
    public static int getIDRPriceRoundedToThousand(double priceInUSD) {
        double priceInIDR = priceInUSD * USD_TO_IDR_RATE;
        return (int) (Math.round(priceInIDR / 1000) * 1000);
    }
    
    // Mendapatkan harga dalam IDR yang sudah dibulatkan ke atas (untuk keamanan pembayaran)
    public static int getIDRPriceRoundedUp(double priceInUSD) {
        double priceInIDR = priceInUSD * USD_TO_IDR_RATE;
        return (int) Math.ceil(priceInIDR);
    }
}