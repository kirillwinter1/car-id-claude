package ru.car.service.vk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VkUserInfoResponse {
    private VkUser user;

    @Data
    @NoArgsConstructor
    public static class VkUser {
        @JsonProperty("user_id")
        private String userId;
        private String phone;
    }
}
