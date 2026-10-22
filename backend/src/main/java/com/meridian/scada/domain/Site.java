package com.meridian.scada.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scada_site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 120)
    private String region;

    @Column(nullable = false, length = 60)
    private String commodity;

    @Column(nullable = false, length = 80)
    private String segment;

    private double x;

    private double y;

    private double nominalPressure;

    private double pressureLow;

    private double pressureHigh;

    private double nominalFlow;

    private double nominalTemperature;

    private double temperatureWarning;

    private double temperatureCritical;

    private int sortOrder;

    protected Site() {
    }

    public Site(String code, String name, String region, String commodity, String segment,
                double x, double y, double nominalPressure, double pressureLow, double pressureHigh,
                double nominalFlow, double nominalTemperature, double temperatureWarning,
                double temperatureCritical, int sortOrder) {
        this.code = code;
        this.name = name;
        this.region = region;
        this.commodity = commodity;
        this.segment = segment;
        this.x = x;
        this.y = y;
        this.nominalPressure = nominalPressure;
        this.pressureLow = pressureLow;
        this.pressureHigh = pressureHigh;
        this.nominalFlow = nominalFlow;
        this.nominalTemperature = nominalTemperature;
        this.temperatureWarning = temperatureWarning;
        this.temperatureCritical = temperatureCritical;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getCommodity() {
        return commodity;
    }

    public String getSegment() {
        return segment;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getNominalPressure() {
        return nominalPressure;
    }

    public double getPressureLow() {
        return pressureLow;
    }

    public double getPressureHigh() {
        return pressureHigh;
    }

    public double getNominalFlow() {
        return nominalFlow;
    }

    public double getNominalTemperature() {
        return nominalTemperature;
    }

    public double getTemperatureWarning() {
        return temperatureWarning;
    }

    public double getTemperatureCritical() {
        return temperatureCritical;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
