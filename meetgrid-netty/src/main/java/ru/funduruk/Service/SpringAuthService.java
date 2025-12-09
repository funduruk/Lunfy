package ru.funduruk.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import ru.funduruk.dto.LoginRequest;
import ru.funduruk.dto.RegisterRequest;

import java.io.IOException;
import java.util.function.Consumer;

public class SpringAuthService {

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpringAuthService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void register(RegisterRequest request, Callback callback) throws IOException {
        String json = mapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/register")
                .post(body)
                .build();

        client.newCall(httpRequest).enqueue(callback);
    }

    public void login(LoginRequest request, Callback callback) throws IOException {
        String json = mapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/login")
                .post(body)
                .build();

        client.newCall(httpRequest).enqueue(callback);
    }

    public void validateTokenAsync(String token, Consumer<Boolean> callback) {
        RequestBody body = RequestBody.create(token, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auth/validate")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.accept(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                boolean valid = Boolean.parseBoolean(response.body().string());
                callback.accept(valid);
            }
        });
    }
}