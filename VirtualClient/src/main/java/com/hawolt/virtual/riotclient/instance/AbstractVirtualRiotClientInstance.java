package com.hawolt.virtual.riotclient.instance;

import com.hawolt.authentication.CookieType;
import com.hawolt.authentication.ICookieSupplier;
import com.hawolt.generic.Constant;
import com.hawolt.generic.data.QueryTokenParser;
import com.hawolt.generic.token.impl.StringTokenSupplier;
import com.hawolt.http.Diffuser;
import com.hawolt.http.Gateway;
import com.hawolt.http.OkHttp3Client;
import com.hawolt.version.local.LocalRiotFileVersion;
import com.hawolt.virtual.riotclient.client.VirtualRiotClient;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created: 07/08/2023 16:44
 * Author: Twitter @hawolt
 **/

public abstract class AbstractVirtualRiotClientInstance implements IVirtualRiotClientInstance {
    private final LocalRiotFileVersion localRiotFileVersion;
    private final ICookieSupplier cookieSupplier;
    private final Gateway gateway;

    public AbstractVirtualRiotClientInstance(Gateway gateway, ICookieSupplier cookieSupplier, boolean selfUpdate) {
        this.localRiotFileVersion = new LocalRiotFileVersion(Arrays.asList("RiotClientFoundation.dll", "RiotGamesApi.dll"));
        if (selfUpdate) localRiotFileVersion.schedule(15, 15, TimeUnit.MINUTES);
        this.cookieSupplier = cookieSupplier;
        this.gateway = gateway;
    }

    public String payload(String username, String password) {
        JSONObject object = new JSONObject();
        object.put("type", "auth");
        object.put("username", username);
        object.put("password", password);
        object.put("remember", false);
        object.put("language", "en_GB");
        object.put("region", JSONObject.NULL);
        return object.toString();
    }

    @Override
    public String get(String username, String password, String cookie, Gateway gateway) throws IOException {
        Diffuser.add(password);
        String body = payload(username, password);
        RequestBody put = RequestBody.create(Constant.APPLICATION_JSON, body);
        String minor = localRiotFileVersion.getVersionValue("RiotGamesApi.dll");
        Request request = new Request.Builder()
                .url("https://auth.riotgames.com/api/v1/authorization")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent",
                        String.format(
                                "RiotClient/%s%s rso-auth (Windows;10;;Professional, x64)",
                                localRiotFileVersion.getVersionValue("RiotClientFoundation.dll"),
                                minor.substring(minor.lastIndexOf('.'))
                        )
                )
                .addHeader("Pragma", "no-cache")
                .put(put)
                .build();
        Call call = OkHttp3Client.perform(request, gateway);
        try (Response response = call.execute()) {
            return response.body().string();
        }
    }

    @Override
    public VirtualRiotClient login(String username, String password) throws IOException {
        return new VirtualRiotClient(this, username, password, getRiotClientSupplier(gateway, username, password));
    }

    @Override
    public StringTokenSupplier getRiotClientSupplier(Gateway gateway, String username, String password) throws IOException {
        String riotClientCookie = cookieSupplier.getClientCookie(localRiotFileVersion, CookieType.RIOT_CLIENT, null, gateway);
        return QueryTokenParser.getTokens("riot-client", get(username, password, riotClientCookie, gateway));
    }

    @Override
    public LocalRiotFileVersion getLocalRiotFileVersion() {
        return localRiotFileVersion;
    }

    @Override
    public ICookieSupplier getCookieSupplier() {
        return cookieSupplier;
    }

    @Override
    public Gateway getGateway() {
        return gateway;
    }
}
