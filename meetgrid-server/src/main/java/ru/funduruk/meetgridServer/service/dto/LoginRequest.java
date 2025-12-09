package ru.funduruk.meetgridServer.service.dto;

public record LoginRequest(
        String email,
        String password
) {}

