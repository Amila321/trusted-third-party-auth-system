package com.scs.dto.attack;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackSimulationRequest {

    @JsonProperty("scenario")
    @NotNull
    private AttackScenarioType scenario;
}
