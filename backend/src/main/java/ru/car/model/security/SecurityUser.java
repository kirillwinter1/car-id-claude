package ru.car.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.car.enums.Role;

import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUser implements UserDetails {
    private static final List<GrantedAuthority> ADMIN_ROLES = List.of(new SimpleGrantedAuthority(Role.ROLE_ADMIN.name()), new SimpleGrantedAuthority(Role.ROLE_USER.name()));
    private static final List<GrantedAuthority> USER_ROLES = List.of(new SimpleGrantedAuthority(Role.ROLE_USER.name()));

    private Long id;
    private String telephone;
    private Role role;
    private Long authId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Role.ROLE_ADMIN.equals(role) ? ADMIN_ROLES : USER_ROLES;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return telephone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
