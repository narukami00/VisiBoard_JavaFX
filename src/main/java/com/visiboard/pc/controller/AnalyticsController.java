package com.visiboard.pc.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class AnalyticsController {

    @FXML
    private BarChart<String, Number> engagementChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    private final com.visiboard.pc.service.ApiService apiService;

    public AnalyticsController() {
        this.apiService = new com.visiboard.pc.service.ApiService();
    }

    @FXML
    private void initialize() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("User Engagement (Notes Created)");
        
        apiService.getWeeklyEngagement().thenAccept(data -> {
            javafx.application.Platform.runLater(() -> {
                // Order days correctly
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                for (String day : days) {
                    series.getData().add(new XYChart.Data<>(day, data.getOrDefault(day, 0L)));
                }
                engagementChart.getData().add(series);
            });
        });
    }
}
