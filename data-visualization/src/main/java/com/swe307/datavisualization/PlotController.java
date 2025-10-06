package com.swe307.datavisualization;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.util.List;

@Controller
public class PlotController {

    @Autowired
    private DataPointRepository repository;

    private int currentIndex = 0;
    private double[] data = new double[100];
    
    // ⚡ OPTIMIZATION: Cache MongoDB data and R script (but create Context per request)
    private String rScriptTemplate;
    private List<DataPoint> dataPoints;

    @PostConstruct
    public void init() {
        try {
            // Load R script once at startup
            ClassPathResource resource = new ClassPathResource("plot.R");
            rScriptTemplate = new String(Files.readAllBytes(resource.getFile().toPath()));
            
            // Cache MongoDB data once
            dataPoints = repository.findAll();
            
            System.out.println("✅ PlotController initialized:");
            System.out.println("   - R script cached");
            System.out.println("   - " + dataPoints.size() + " data points cached from MongoDB");
            System.out.println("   - NOTE: Context created per request (GraalVM R limitation)");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Initialization failed: " + e.getMessage());
        }
    }

    @GetMapping("/plot")
    @ResponseBody
    public String plot() {
        // Use cached data instead of querying MongoDB every time
        if (dataPoints == null || dataPoints.isEmpty()) {
            return "<html><body><h1>No data! Import CSV first.</h1></body></html>";
        }

        if (currentIndex >= dataPoints.size()) {
            currentIndex = 0;
            data = new double[100];
        }

        DataPoint currentDataPoint = dataPoints.get(currentIndex);
        Double value = currentDataPoint.getCol5();

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
            // Create new Context per request (necessary for GraalVM R stability)
            // Cached data & R script still provide optimization!
            polyglot = Context.newBuilder()
                    .allowAllAccess(true)
                    .build();
            
            // Create R vectors
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

            // Execute cached R script (no file I/O!)
            String rCode = "data <- " + dataStr + "\n" +
                          "time <- " + timeStr + "\n" +
                          rScriptTemplate;

            Value result = polyglot.eval("R", rCode);
            return result.asString();

        } catch (Exception e) {
            e.printStackTrace();
            return "<div style='color: red;'>Error: " + e.getMessage() + "</div>";
        } finally {
            // Clean up Context
            if (polyglot != null) {
                polyglot.close();
            }
        }
    }

    private String buildHtmlPage(String svgPlot, int count) {
        return "<!DOCTYPE html><html><head>" +
            "<meta http-equiv='refresh' content='1'>" +
            "<meta charset='UTF-8'>" +
            "<title>SWE307 - Col-5 Data Visualization</title>" +
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
            ".fast { margin-top: 10px; color: #ff6b6b; font-weight: bold; }" +
            "</style></head><body>" +
            "<div class='container'>" +
            "<h1>📊 Real-time Data Visualization - Column 5</h1>" +
            "<div class='plot-area'>" + svgPlot + "</div>" +
            "<div class='info'>" +
            "<p>Data Points: <span class='count'>" + count + " / 100</span></p>" +
            "<p class='status'>● Auto-refreshing every 1 second</p>" +
            "<p class='fast'>⚡ Optimized: Cached MongoDB data & R script</p>" +
            "</div></div></body></html>";
    }
}