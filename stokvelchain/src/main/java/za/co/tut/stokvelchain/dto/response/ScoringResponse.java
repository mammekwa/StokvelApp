package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringResponse {
    double probability;
    double score;
    double threshold;
    boolean eligible;
    List<ScoreReason> reasons;
    String modelVersion;
}