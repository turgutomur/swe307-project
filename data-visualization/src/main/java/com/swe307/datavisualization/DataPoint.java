package com.swe307.datavisualization;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "datapoints")
public class DataPoint {
    
    @Id
    private String id;
    
    @Field("Col-5")
    private Double col5;

    public DataPoint() {
    }

    public DataPoint(Double col5) {
        this.col5 = col5;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getCol5() {
        return col5;
    }

    public void setCol5(Double col5) {
        this.col5 = col5;
    }

    @Override
    public String toString() {
        return "DataPoint{" +
                "id='" + id + '\'' +
                ", col5=" + col5 +
                '}';
    }
}