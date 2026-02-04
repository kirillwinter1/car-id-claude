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
public class AuthenticationCode {
    private Long id;
    private LocalDateTime createdDate;
    private String phoneNumber;
    private String code;
    private Long smsId;
}
