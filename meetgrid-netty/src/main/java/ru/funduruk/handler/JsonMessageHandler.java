package ru.funduruk.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import ru.funduruk.Service.SpringAuthService;
import ru.funduruk.dto.AuthResponse;
import ru.funduruk.dto.LoginRequest;
import ru.funduruk.dto.RegisterRequest;

import java.io.IOException;

public class JsonMessageHandler extends SimpleChannelInboundHandler<String> {

    private final SpringAuthService authService = new SpringAuthService("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();
    private static final AttributeKey<String> JWT = AttributeKey.valueOf("JWT");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        JsonNode json = mapper.readTree(msg);
        String type = json.get("type").asText();

        if (!type.equals("LOGIN") && !type.equals("REGISTER")) {
            String token = ctx.channel().attr(JWT).get();
            if (token == null || token.isEmpty()) {
                ctx.writeAndFlush("{\"error\":\"Unauthorized\"}");
                return;
            }

            authService.validateTokenAsync(token, valid -> {
                if (!valid) {
                    ctx.writeAndFlush("{\"error\":\"Unauthorized\"}");
                    return;
                }
                handleCommand(ctx, type, json);
            });
            return;
        }


        handleCommand(ctx, type, json);
    }

    private void handleCommand(ChannelHandlerContext ctx, String type, JsonNode json) {
        try {
            switch (type) {
                case "REGISTER" -> handleRegister(ctx, json);
                case "LOGIN" -> handleLogin(ctx, json);
                case "JOIN_SERVER" -> handleJoinServer(ctx, json);
                case "SEND_MESSAGE" -> handleSendMessage(ctx, json);
                case "JOIN_VOICE" -> handleJoinVoice(ctx, json);
                case "LEAVE_VOICE" -> handleLeaveVoice(ctx, json);
                case "TOGGLE_SCREEN_SHARE" -> handleToggleScreen(ctx, json);
                default -> ctx.writeAndFlush("{\"error\":\"Unknown command\"}");
            }
        } catch (Exception e) {
            ctx.writeAndFlush("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, JsonNode json) {
        try {
            RegisterRequest request = new RegisterRequest(
                    json.get("username").asText(),
                    json.get("email").asText(),
                    json.get("password").asText()
            );
            authService.register(request, createCallback(ctx));
        } catch (Exception e) {
            ctx.writeAndFlush("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, JsonNode json) {
        try {
            LoginRequest request = new LoginRequest(
                    json.get("email").asText(),
                    json.get("password").asText()
            );
            authService.login(request, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    ctx.writeAndFlush("{\"error\":\"" + e.getMessage() + "\"}");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body().string();
                    AuthResponse authResponse = mapper.readValue(body, AuthResponse.class);
                    ctx.channel().attr(JWT).set(authResponse.token());
                    ctx.writeAndFlush(body);
                }
            });
        } catch (Exception e) {
            ctx.writeAndFlush("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleJoinServer(ChannelHandlerContext ctx, JsonNode json) {
        String serverId = json.get("serverId").asText();
        // Тут вызываем Spring API или локальный сервис
        ctx.writeAndFlush("{\"success\":\"Joined server " + serverId + "\"}");
    }

    private void handleSendMessage(ChannelHandlerContext ctx, JsonNode json) {
        String channelId = json.get("channelId").asText();
        String content = json.get("content").asText();
        // Вызов Spring API для отправки сообщения
        ctx.writeAndFlush("{\"success\":\"Message sent to " + channelId + "\"}");
    }

    private void handleJoinVoice(ChannelHandlerContext ctx, JsonNode json) {
        String channelId = json.get("channelId").asText();
        // Логика подключения к голосовой сессии
        ctx.writeAndFlush("{\"success\":\"Joined voice channel " + channelId + "\"}");
    }

    private void handleLeaveVoice(ChannelHandlerContext ctx, JsonNode json) {
        String channelId = json.get("channelId").asText();
        // Логика выхода из голосовой сессии
        ctx.writeAndFlush("{\"success\":\"Left voice channel " + channelId + "\"}");
    }

    private void handleToggleScreen(ChannelHandlerContext ctx, JsonNode json) {
        String channelId = json.get("channelId").asText();
        // Логика включения/выключения демонстрации экрана
        ctx.writeAndFlush("{\"success\":\"Toggled screen share for " + channelId + "\"}");
    }

    private Callback createCallback(ChannelHandlerContext ctx) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ctx.writeAndFlush("{\"error\":\"" + e.getMessage() + "\"}");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                ctx.writeAndFlush(body);
            }
        };
    }
}




