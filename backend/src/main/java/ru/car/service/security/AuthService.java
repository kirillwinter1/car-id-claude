package ru.car.service.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.car.model.security.SecurityUser;

import java.util.Objects;

@Component
public class AuthService {
    public Long getUserId() {
        return ((SecurityUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    public Long getUserIdOrNull() {
        if (Objects.nonNull(SecurityContextHolder.getContext().getAuthentication()) &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof SecurityUser user) {
            return user.getId();
        }
        return null;
    }

    public String getPhoneNumber() {
        return ((SecurityUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
    }

    public Long getAuthId() {
        return ((SecurityUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getAuthId();
    }
}
