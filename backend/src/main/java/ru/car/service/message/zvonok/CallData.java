package ru.car.service.message.zvonok;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallData {
    private String balance;
    private Long call_id;
    private LocalDateTime created;
    private String phone;
    private String pincode;
}
