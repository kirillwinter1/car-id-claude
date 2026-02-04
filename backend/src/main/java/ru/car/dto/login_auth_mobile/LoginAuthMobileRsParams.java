package ru.car.dto.login_auth_mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuthMobileRsParams {
    @JsonProperty("time_to_next_request_sec")
    @Builder.Default
    private Integer timeToNextRequestSec = 30;
}
