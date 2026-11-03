package com.gym.system.service;

/** Thrown when the signed-in user is not allowed to perform an operation. */
public class AccessDeniedException extends SecurityException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
