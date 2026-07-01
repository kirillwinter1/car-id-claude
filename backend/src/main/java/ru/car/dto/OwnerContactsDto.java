package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** BF6: опубликованные владельцем контакты для развилки на скане (готовые ссылки). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerContactsDto {
    private String phone;
    private String telegram;
    private String vk;
    private String max;
}
