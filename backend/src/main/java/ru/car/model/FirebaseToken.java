package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseToken {
    private Long id;
    private Long authId;
    private Long userId;
    private String token;
    private LocalDateTime createdDate;
}
