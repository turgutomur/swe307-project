package com.swe307.datavisualization;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "datapoints")
public class DataPoint {
    
    @Id
    private String id;
    
    @Field("Col-1")
    private Double col1;

    public DataPoint() {
    }

    public DataPoint(Double col1) {
        this.col1 = col1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getCol1() {
        return col1;
    }

    public void setCol1(Double col1) {
        this.col1 = col1;
    }

    @Override
    public String toString() {
        return "DataPoint{" +
                "id='" + id + '\'' +
                ", col1=" + col1 +
                '}';
    }
}