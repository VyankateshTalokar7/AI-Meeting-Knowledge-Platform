package com.aimeetingknowledge.platform.auth.dto;

import com.aimeetingknowledge.platform.user.User;

public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
