package ru.funduruk.meetgridServer.service.dto;

public record RegisterRequest (
    String username,
    String email,
    String password
    ) {}
