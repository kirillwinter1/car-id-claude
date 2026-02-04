package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.Role;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String phoneNumber;
    private Role role;
    private Boolean active;
    private LocalDateTime createDate;
    private Long telegramChatId;

    public boolean isAdmin() {
        return Role.ROLE_ADMIN.equals(role);
    }
}
