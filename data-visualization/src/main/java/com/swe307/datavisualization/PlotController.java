package com.swe307.datavisualization;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
public class PlotController {

    @Autowired
    private DataPointRepository repository;

    // 🔄 REAL-TIME: Incremental data processing with context pool
    private int currentIndex = 0;
    private double[] data = new double[100];
    private String rScriptTemplate;
    private List<DataPoint> dataPoints;
    
    // Context pool for performance (but process data incrementally)
    private BlockingQueue<Context> contextPool;
    private static final int POOL_SIZE = 3;

    @PostConstruct
    public void init() {
        try {
            // Load R script once
            ClassPathResource resource = new ClassPathResource("plot.R");
            rScriptTemplate = new String(Files.readAllBytes(resource.getFile().toPath()));
            
            // Cache MongoDB data (but don't preprocess - use incrementally)
            dataPoints = repository.findAll();
            
            // Initialize Context pool for performance
            contextPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) {
                Context ctx = Context.newBuilder()
                    .allowAllAccess(true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
                contextPool.offer(ctx);
            }
            
            System.out.println("✅ PlotController REAL-TIME OPTIMIZED:");
            System.out.println("   - " + dataPoints.size() + " data points cached from MongoDB");
            System.out.println("   - " + POOL_SIZE + " R contexts pooled for speed");
            System.out.println("   - Dynamic incremental plotting enabled");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Initialization failed: " + e.getMessage());
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (contextPool != null) {
            while (!contextPool.isEmpty()) {
                Context ctx = contextPool.poll();
                if (ctx != null) ctx.close();
            }
        }
        System.out.println("✅ Context pool cleaned up");
    }

    @GetMapping("/plot")
    @ResponseBody
    public String plot() {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return "<html><body><h1>No data! Import CSV first.</h1></body></html>";
        }

        // Reset when all data is processed
        if (currentIndex >= dataPoints.size()) {
            currentIndex = 0;
            data = new double[100];
            System.out.println("🔄 Resetting to start - all 100 points displayed");
        }

        // Get next data point from MongoDB cache
        DataPoint currentDataPoint = dataPoints.get(currentIndex);
        Double value = currentDataPoint.getCol5();

        if (value == null) {
            value = 0.0; // Handle null values
        }

        // Add new data point to array
        data[currentIndex] = value;
        
        // Create arrays for current data (growing each time)
        double[] plotData = new double[currentIndex + 1];
        int[] timeData = new int[currentIndex + 1];
        
        for (int i = 0; i <= currentIndex; i++) {
            plotData[i] = data[i];
            timeData[i] = i;
        }

        currentIndex++;
        
        // Generate fresh plot with current data
        long startTime = System.currentTimeMillis();
        String svgPlot = generatePlotWithR(plotData, timeData);
        long endTime = System.currentTimeMillis();
        
        System.out.println("📊 Generated plot with " + plotData.length + " points in " + (endTime - startTime) + "ms");
        
        return buildHtmlPage(svgPlot, currentIndex);
    }

    private String generatePlotWithR(double[] data, int[] time) {
        Context polyglot = null;
        try {
            // Get context from pool (super fast!)
            polyglot = contextPool.poll(1, TimeUnit.SECONDS);
            if (polyglot == null) {
                return "<div style='color: red;'>No context available</div>";
            }
            
            // Fast vector creation with streams
            String dataStr = "c(" + Arrays.stream(data)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", ")) + ")";
            String timeStr = "c(" + Arrays.stream(time)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", ")) + ")";

            // Execute R script
            String rCode = "data <- " + dataStr + "\n" +
                          "time <- " + timeStr + "\n" +
                          rScriptTemplate;

            Value result = polyglot.eval("R", rCode);
            return result.asString();

        } catch (Exception e) {
            e.printStackTrace();
            return "<div style='color: red;'>Error: " + e.getMessage() + "</div>";
        } finally {
            // Return context to pool for reuse
            if (polyglot != null) {
                try {
                    contextPool.offer(polyglot, 1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    polyglot.close(); // Close if can't return to pool
                }
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
            "<p class='fast'>🔄 REAL-TIME: Adding new data point each second with Context Pool</p>" +
            "</div></div></body></html>";
    }
}