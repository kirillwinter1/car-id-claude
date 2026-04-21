package ru.car.test.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.car.enums.Role;
import ru.car.model.security.SecurityUser;
import ru.car.test.builder.SecurityUserBuilder;

/**
 * Utility class for setting up security context in tests.
 */
public final class SecurityTestUtils {

    private SecurityTestUtils() {
        // Utility class
    }

    /**
     * Sets up security context with a test user.
     */
    public static void setAuthenticatedUser(SecurityUser user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Sets up security context with a default test user.
     */
    public static SecurityUser setDefaultAuthenticatedUser() {
        SecurityUser user = SecurityUserBuilder.aSecurityUser()
                .withId(1L)
                .withTelephone("79001234567")
                .withRole(Role.ROLE_USER)
                .withAuthId(1L)
                .build();
        setAuthenticatedUser(user);
        return user;
    }

    /**
     * Sets up security context with a test user with specified ID.
     */
    public static SecurityUser setAuthenticatedUser(Long userId) {
        SecurityUser user = SecurityUserBuilder.aSecurityUser()
                .withId(userId)
                .withTelephone("79001234567")
                .withRole(Role.ROLE_USER)
                .withAuthId(1L)
                .build();
        setAuthenticatedUser(user);
        return user;
    }

    /**
     * Sets up security context with an admin user.
     */
    public static SecurityUser setAuthenticatedAdmin() {
        SecurityUser user = SecurityUserBuilder.aSecurityUser()
                .withId(1L)
                .withTelephone("79001234567")
                .asAdmin()
                .withAuthId(1L)
                .build();
        setAuthenticatedUser(user);
        return user;
    }

    /**
     * Clears the security context.
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
