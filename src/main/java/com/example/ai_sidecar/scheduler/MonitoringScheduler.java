package com.example.ai_sidecar.scheduler;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.ai_sidecar.client.PrometheusClient;
import com.example.ai_sidecar.service.AnomalyEngine;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringScheduler {
    private final PrometheusClient prometheusClient;
    private final AnomalyEngine anomalyEngine;

    @Value("${pushgateway.url}")
    private String pushgatewayUrl;

    private static final Gauge anomalyGauge = Gauge.build()
            .name("auth_service_anomaly_detected")
            .help("Статус аномалии (1.0 - обнаружена, 0.0 - норма)")
            .labelNames("metric_name")
            .register();

    @Scheduled(fixedRateString = "${monitoring.interval:60000}")
    public void runAnalysis() {
        try {
            log.info("Начало цикла анализа метрик...");

            String metricQuery = "rate(spring_kafka_listener_seconds_sum[1m])";
            List<Double> metrics = prometheusClient.fetchMetrics(metricQuery, 120);

            if (metrics.isEmpty()) {
                log.warn("Метрики не получены от Prometheus. Пропуск цикла.");
                return;
            }

            double score = anomalyEngine.calculateAnomalyScore(metrics);
            log.info("Текущий score аномальности: {}", score);

            double status = (score > 0.5) ? 1.0 : 0.0;
            anomalyGauge.labels("kafka_latency").set(status);

            if (status > 0) {
                log.error("!!! ВНИМАНИЕ: Обнаружена аномалия в работе микросервиса !!!");
            }

            pushToGrafana();

        } catch (Exception e) {
            log.error("Ошибка в цикле мониторинга: ", e);
        }
    }

    private void pushToGrafana() {
        try {
            PushGateway pg = new PushGateway(pushgatewayUrl);
            pg.pushAdd(CollectorRegistry.defaultRegistry, "ai_anomaly_job");
            log.info("Метрики успешно отправлены в Pushgateway по адресу: {}", pushgatewayUrl);
        } catch (IOException e) {
            log.error("Не удалось отправить метрики в Pushgateway: {}", e.getMessage());
        }
    }
}
