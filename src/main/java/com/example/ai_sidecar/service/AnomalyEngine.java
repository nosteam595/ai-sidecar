package com.example.ai_sidecar.service;

import org.springframework.stereotype.Service;
import smile.anomaly.IsolationForest;
import java.util.List;

@Service
public class AnomalyEngine {

    public double calculateAnomalyScore(List<Double> dataPoints) {
        if (dataPoints.size() < 10) return 0.0;

        double[][] trainingData = dataPoints.stream()
                .map(d -> new double[]{d})
                .toArray(double[][]::new);

        IsolationForest forest = IsolationForest.fit(trainingData);

        double[] lastPoint = trainingData[trainingData.length - 1];
        return forest.score(lastPoint);
    }
}
