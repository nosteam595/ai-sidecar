package com.example.ai_sidecar.client;

import com.example.ai_sidecar.model.PrometheusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Component
public class PrometheusClient {
    private final WebClient webClient;

    public PrometheusClient(WebClient.Builder webClientBuilder, @Value("${prometheus.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public List<Double> fetchMetrics(String query, int minutes) {
        long now = System.currentTimeMillis() / 1000;
        long start = now - (minutes * 60);

        return webClient.get()
                .uri(uri -> uri.path("/api/v1/query_range")
                        .queryParam("query", query)
                        .queryParam("start", start)
                        .queryParam("end", now)
                        .queryParam("step", "1m")
                        .build())
                .retrieve()
                .bodyToMono(PrometheusResponse.class)
                .map(response -> response.data().result().get(0).values().stream()
                        .map(v -> Double.parseDouble(v.get(1).toString()))
                        .toList())
                .block();
    }
}
