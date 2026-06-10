package com.scs.dto.attack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scs.dto.auth.TtpAuthenticationDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackSimulationResponse {

    @JsonProperty("scenario")
    private AttackScenarioType scenario;

    @JsonProperty("attack_name")
    private String attackName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("expected_rejection")
    private boolean expectedRejection;

    @JsonProperty("rejected")
    private boolean rejected;

    @JsonProperty("ttp_decision")
    private TtpAuthenticationDecision ttpDecision;

    @JsonProperty("evidence")
    private Map<String, Object> evidence;
}
