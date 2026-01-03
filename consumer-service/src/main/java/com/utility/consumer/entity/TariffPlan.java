package com.utility.consumer.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "tariff_plans")
public class TariffPlan {
    @Id
    private String id;
    private String utilityType;
    private String category;    
    private Double fixedCharge; 
    private Double taxPercentage;
    private String planName; 
    private String description;    
    private List<Slab> slabs;       
    
    @Data
    public static class Slab {
        private Integer minUnits;
        private Integer maxUnits;        
        private Double rate; 
        private Double ratePerUnit;
    }
}