package ru.funduruk.meetgridServer.service.dto;

public record AuthResponse (
    String accessToken,
    String refreshToken
) {}
