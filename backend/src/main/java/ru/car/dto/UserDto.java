package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    @JsonProperty("phone_number")
    @Schema(description = "Номер телефона", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;
    private String role;
    private Boolean active;
}
