package com.wowax.apkextractor;

import org.json.JSONException;
import org.json.JSONObject;

public class SmbServer {
    public String name = "";
    public String host = "";
    public String share = "";
    public String username = "";
    public String password = "";
    public String domain = "";

    public String toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("host", host);
        o.put("share", share);
        o.put("username", KeystoreCrypto.encrypt(username));
        o.put("password", KeystoreCrypto.encrypt(password));
        o.put("domain", domain);
        return o.toString();
    }

    public static SmbServer fromJson(String s) {
        SmbServer server = new SmbServer();
        try {
            JSONObject o = new JSONObject(s);
            server.name = o.optString("name");
            server.host = o.optString("host");
            server.share = o.optString("share");
            server.username = KeystoreCrypto.decrypt(o.optString("username"));
            server.password = KeystoreCrypto.decrypt(o.optString("password"));
            server.domain = o.optString("domain");
        } catch (JSONException ignored) {
        }
        return server;
    }
}
