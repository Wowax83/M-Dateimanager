package com.wowax.apkextractor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SmbCredentialStore {

    private static final String PREFS = "smb_servers";
    private static final String KEY = "servers";

    public static List<SmbServer> load(Context c) {
        List<SmbServer> list = new ArrayList<>();
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(SmbServer.fromJson(arr.getString(i)));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public static void save(Context c, List<SmbServer> servers) {
        JSONArray arr = new JSONArray();
        for (SmbServer s : servers) {
            try {
                arr.put(s.toJson());
            } catch (JSONException ignored) {
            }
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }
}
