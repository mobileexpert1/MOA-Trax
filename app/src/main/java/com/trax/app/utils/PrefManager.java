package com.trax.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trax.app.MyApp;
import com.trax.app.models.home.license.LicenseModel;
import com.trax.app.models.track.GetTracksResponse;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrefManager {
    private static SharedPreferences sharedPreferences;

    //==============================================================================
    // Initialization & Generic SharedPreferences Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes the shared preference file instance.
    //--------------------------------------------------
    public static SharedPreferences init() {
        if (sharedPreferences == null) {
            sharedPreferences = MyApp.getInstance().getSharedPreferences("Orbis", Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    //--------------------------------------------------
    // Persists a string value to preferences storage.
    //--------------------------------------------------
    public static void putString(String key, String val) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, val);
        editor.apply();
    }

    //--------------------------------------------------
    // Retrieves a persisted string or defaults to empty.
    //--------------------------------------------------
    public static String getString(String key) {
        init();
        return sharedPreferences.getString(key, "");
    }

    //--------------------------------------------------
    // Persists a boolean flag.
    //--------------------------------------------------
    public static void putBoolean(String key, boolean val) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, val);
        editor.apply();
    }

    //--------------------------------------------------
    // Retrieves a persisted boolean flag.
    //--------------------------------------------------
    public static boolean getBoolean(String key) {
        init();
        return sharedPreferences.getBoolean(key, false);
    }

    //--------------------------------------------------
    // Erases all cached properties from SharedPreferences.
    //--------------------------------------------------
    public static void clear() {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    //--------------------------------------------------
    // Deletes a specific key profile from SharedPreferences.
    //--------------------------------------------------
    public static void clearKey(String key) {
        init();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.commit();
    }

    //==============================================================================
    // Specialized Object Caching Methods
    //==============================================================================

    //--------------------------------------------------
    // Serializes and saves properties licenses list.
    //--------------------------------------------------
    public static void saveLicensesList(List<LicenseModel> list) {
        String json = new Gson().toJson(list);
        putString("licenses_list", json);
    }

    //--------------------------------------------------
    // Deserializes and returns saved licenses array list.
    //--------------------------------------------------
    public static List<LicenseModel> getLicensesList() {
        String json = getString("licenses_list");
        if (json.isEmpty()) {
            return null;
        }
        Type type = new TypeToken<List<LicenseModel>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    //--------------------------------------------------
    // Deletes the serialized licenses list.
    //--------------------------------------------------
    public static void clearLicensesList() {
        clearKey("licenses_list");
    }

    //--------------------------------------------------
    // Caches the tracks lists network response payload structure.
    //--------------------------------------------------
    public static void saveGetTracksResponse(GetTracksResponse response) {
        if (response == null) return;
        String json = new Gson().toJson(response);
        putString("cached_get_tracks_response", json);
    }

    //--------------------------------------------------
    // Deserializes the saved tracks response payload structure.
    //--------------------------------------------------
    public static GetTracksResponse getCachedGetTracksResponse() {
        String json = getString("cached_get_tracks_response");
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return new Gson().fromJson(json, GetTracksResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    //--------------------------------------------------
    // Adds a product number to the downloaded offline products registry set.
    //--------------------------------------------------
    public static void saveDownloadedProduct(String productNo) {
        if (productNo == null || productNo.isEmpty()) return;
        Set<String> set = getDownloadedProducts();
        set.add(productNo);
        String json = new Gson().toJson(set);
        putString("downloaded_products_set", json);
    }

    //--------------------------------------------------
    // Removes a product number from the offline downloaded set database.
    //--------------------------------------------------
    public static void removeDownloadedProduct(String productNo) {
        if (productNo == null || productNo.isEmpty()) return;
        Set<String> set = getDownloadedProducts();
        set.remove(productNo);
        String json = new Gson().toJson(set);
        putString("downloaded_products_set", json);
    }

    //--------------------------------------------------
    // Evaluates if a specific product map has completed local storage down.
    //--------------------------------------------------
    public static boolean isProductDownloaded(String productNo) {
        if (productNo == null || productNo.isEmpty()) return false;
        return getDownloadedProducts().contains(productNo);
    }

    //--------------------------------------------------
    // Extracts the registry set of all downloaded property product numbers.
    //--------------------------------------------------
    public static Set<String> getDownloadedProducts() {
        String json = getString("downloaded_products_set");
        if (json == null || json.isEmpty()) {
            return new HashSet<>();
        }
        try {
            Type type = new TypeToken<Set<String>>(){}.getType();
            Set<String> set = new Gson().fromJson(json, type);
            return set != null ? set : new HashSet<>();
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
}