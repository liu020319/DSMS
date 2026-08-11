package com.medicine.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {
    private static final String ADMIN_HASH = "$2a$10$TuITECcS.Ty.C7GdmPbp5evsiS3AMDH9F83OHzFIeotRr45XyxFvK";

    @Test
    void publishedAdminHashMatchesOnlyRequestedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("admin", ADMIN_HASH));
        assertFalse(encoder.matches("admin123", ADMIN_HASH));
        assertFalse(encoder.matches("wrong-password", ADMIN_HASH));
    }
}
