package com.trax.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.trax.app.MyApp;

public class PrefManager {
    private static SharedPreferences sharedPreferences;
    public static void putString(String key, String val) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, val);
        editor.apply();
    }
    public static String getString(String key) {
        init();
        return sharedPreferences.getString(key, "");
    }
    public static void putBoolean(String key, boolean val) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, val);
        editor.apply();
    }
    public static boolean getBoolean(String key) {
        init();
        return sharedPreferences.getBoolean(key, false);
    }
    public static SharedPreferences init() {
        sharedPreferences = MyApp.getInstance().getSharedPreferences("Orbis", Context.MODE_PRIVATE);
        return sharedPreferences;
    }
    public static void clear() {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }
    public static void clearKey(String key) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.commit();
    }


}