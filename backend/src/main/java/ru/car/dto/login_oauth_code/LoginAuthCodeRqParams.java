package ru.car.dto.login_oauth_code;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuthCodeRqParams {
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String code;
}
