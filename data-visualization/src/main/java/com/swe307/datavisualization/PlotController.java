package com.swe307.datavisualization;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Controller
public class PlotController {

    @Autowired
    private DataPointRepository repository;

    private int currentIndex = 0;
    private double[] data = new double[100];

    public PlotController() {
        // Context will be created per request to avoid disposal issues
    }

    @GetMapping("/plot")
    @ResponseBody
    public String plot() {
        List<DataPoint> dataPoints = repository.findAll();

        if (dataPoints.isEmpty()) {
            return "<html><body><h1>No data! Import CSV first.</h1></body></html>";
        }

        if (currentIndex >= dataPoints.size()) {
            currentIndex = 0;
            data = new double[100];
        }

        DataPoint currentDataPoint = dataPoints.get(currentIndex);
        Double value = currentDataPoint.getCol1();

        if (value == null) {
            return "<html><body><h1>Data is null at index " + currentIndex + "</h1></body></html>";
        }

        data[currentIndex] = value;

        double[] plotData = new double[currentIndex + 1];
        int[] timeData = new int[currentIndex + 1];
        
        for (int i = 0; i <= currentIndex; i++) {
            plotData[i] = data[i];
            timeData[i] = i;
        }

        currentIndex++;

        String svgPlot = generatePlotWithR(plotData, timeData);
        
        return buildHtmlPage(svgPlot, currentIndex);
    }

    private String generatePlotWithR(double[] data, int[] time) {
        Context polyglot = null;
        try {
            // Create a new Context for each request to avoid disposal issues
            polyglot = Context.newBuilder()
                    .allowAllAccess(true)
                    .build();
            
            // Create R vectors for data and time
            StringBuilder dataStr = new StringBuilder("c(");
            StringBuilder timeStr = new StringBuilder("c(");
            
            for (int i = 0; i < data.length; i++) {
                if (i > 0) {
                    dataStr.append(", ");
                    timeStr.append(", ");
                }
                dataStr.append(data[i]);
                timeStr.append(time[i]);
            }
            dataStr.append(")");
            timeStr.append(")");

            // Load plot.R file from resources
            ClassPathResource resource = new ClassPathResource("plot.R");
            String plotRScript = new String(Files.readAllBytes(resource.getFile().toPath()));
            
            // Prepare data and time variables, then execute plot.R
            String rCode = "data <- " + dataStr + "\n" +
                          "time <- " + timeStr + "\n" +
                          plotRScript;

            Value result = polyglot.eval("R", rCode);
            return result.asString();

        } catch (IOException e) {
            e.printStackTrace();
            return "<div style='color: red;'>Error reading plot.R: " + e.getMessage() + "</div>";
        } catch (Exception e) {
            e.printStackTrace();
            return "<div style='color: red;'>Error: " + e.getMessage() + "</div>";
        } finally {
            // Close the context after use
            if (polyglot != null) {
                polyglot.close();
            }
        }
    }

    private String buildHtmlPage(String svgPlot, int count) {
        return "<!DOCTYPE html><html><head>" +
            "<meta http-equiv='refresh' content='1'>" +
            "<meta charset='UTF-8'>" +
            "<title>Random Number Plot</title>" +
            "<style>" +
            "body { font-family: Arial; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
            "min-height: 100vh; display: flex; justify-content: center; align-items: center; margin: 0; padding: 20px; }" +
            ".container { background: white; padding: 40px; border-radius: 15px; " +
            "box-shadow: 0 10px 40px rgba(0,0,0,0.3); max-width: 1200px; }" +
            "h1 { text-align: center; color: #333; margin-bottom: 30px; }" +
            ".plot-area { display: flex; justify-content: center; background: #f9f9f9; " +
            "padding: 20px; border-radius: 10px; }" +
            ".info { text-align: center; margin-top: 20px; color: #666; }" +
            ".count { font-size: 1.2em; color: #667eea; font-weight: bold; }" +
            ".status { margin-top: 15px; color: #28a745; }" +
            "</style></head><body>" +
            "<div class='container'>" +
            "<h1>📊 Random Number Plot</h1>" +
            "<div class='plot-area'>" + svgPlot + "</div>" +
            "<div class='info'>" +
            "<p>Data Points: <span class='count'>" + count + " / 100</span></p>" +
            "<p class='status'>● Auto-refreshing every 1 second...</p>" +
            "</div></div></body></html>";
    }
}