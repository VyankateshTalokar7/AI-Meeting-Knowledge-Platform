package com.aimeetingknowledge.platform.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User account was not found.");
    }
}
