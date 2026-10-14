package za.co.tut.stokvelchain.scoring;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import za.co.tut.stokvelchain.dto.request.ScoringRequest;
import za.co.tut.stokvelchain.dto.response.ScoringResponse;
import za.co.tut.stokvelchain.exception.CreditScoringException;

import java.time.Duration;

@Component
public class CreditScoringClient {
    private final RestClient restClient;

    public CreditScoringClient(@Value("${scoring.service.url}") String baseUrl,
                               @Value("${scoring.service.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90)); // Render free tier can take ~60s to wake up

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .requestFactory(factory)
                .build();
    }

    public ScoringResponse score(ScoringRequest request) {
        try {
            ScoringResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ScoringResponse.class);
            if (response == null) {
                throw new CreditScoringException("Empty response from credit scoring service");
            }
            return response;
        } catch (RestClientException ex) {
            throw new CreditScoringException("Credit scoring service unavailable", ex);
        }
    }
}
