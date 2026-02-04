package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionControlDto {
    @JsonProperty("apple_current")
    private String appleCurrent;
    @JsonProperty("apple_min")
    private String appleMin;
    @JsonProperty("google_current")
    private String googleCurrent;
    @JsonProperty("google_min")
    private String googleMin;
    @JsonProperty("rustore_current")
    private String rustoreCurrent;
    @JsonProperty("rustore_min")
    private String rustoreMin;
}
