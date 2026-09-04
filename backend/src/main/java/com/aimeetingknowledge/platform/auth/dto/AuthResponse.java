package com.aimeetingknowledge.platform.auth.dto;

public record AuthResponse(String token, Long id, String name, String email) {
}
