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

    public static void saveLicensesList(java.util.List<com.trax.app.models.home.license.LicenseModel> list) {
        String json = new com.google.gson.Gson().toJson(list);
        putString("licenses_list", json);
    }

    public static java.util.List<com.trax.app.models.home.license.LicenseModel> getLicensesList() {
        String json = getString("licenses_list");
        if (json.isEmpty()) {
            return null;
        }
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<com.trax.app.models.home.license.LicenseModel>>(){}.getType();
        return new com.google.gson.Gson().fromJson(json, type);
    }

    public static void clearLicensesList() {
        clearKey("licenses_list");
    }

}