package ru.car.test.builder;

import ru.car.enums.Role;
import ru.car.model.security.SecurityUser;

/**
 * Builder for creating SecurityUser instances in tests.
 */
public class UserBuilder {

    private Long id = 1L;
    private String telephone = "79001234567";
    private Role role = Role.ROLE_USER;
    private Long authId = 1L;

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public UserBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserBuilder withTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public UserBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserBuilder withAuthId(Long authId) {
        this.authId = authId;
        return this;
    }

    public UserBuilder asAdmin() {
        this.role = Role.ROLE_ADMIN;
        return this;
    }

    public SecurityUser build() {
        return SecurityUser.builder()
                .id(id)
                .telephone(telephone)
                .role(role)
                .authId(authId)
                .build();
    }
}
