package ru.funduruk.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    @Getter
    @Setter
    private static String token = null;
    @Getter
    private static String currentUsername = null;
    @Getter
    private static String currentTag = null;

    public static Map<String, Object> post(String path, Map<String, ?> body) throws Exception {
        String json = mapper.writeValueAsString(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), Map.class);
    }

    public static Map<String, Object> get(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), Map.class);
    }

    public static Map<String, Object> login(String username, String password) throws Exception {
        Map<String, Object> result = post("/api/auth/login",
                Map.of("username", username, "password", password));

        if (result.containsKey("token")) {
            token = (String) result.get("token");
            currentUsername = (String) result.get("username");
            currentTag = (String) result.get("tag");
        }
        return result;
    }

    public static Map<String, Object> register(String username, String email, String password) throws Exception {
        Map<String, Object> result = post("/api/auth/register",
                Map.of("username", username, "email", email, "password", password));

        if (result.containsKey("token")) {
            token = (String) result.get("token");
            currentUsername = (String) result.get("username");
            currentTag = (String) result.get("tag");
        }
        return result;
    }

    public static String getRaw(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String postRaw(String path, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void delete(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .DELETE();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}