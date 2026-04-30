package com.example.ai_sidecar.model;

import java.util.List;

public record PrometheusResponse(String status, Data data) {
    public record Data(List<Result> result) {}
    public record Result(Metric metric, List<List<Object>> values) {}
    public record Metric(String instance) {}
}
