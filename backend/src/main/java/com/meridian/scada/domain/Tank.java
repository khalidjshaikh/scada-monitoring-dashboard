package com.meridian.scada.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scada_tank")
public class Tank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long siteId;

    @Column(nullable = false, length = 16)
    private String siteCode;

    @Column(nullable = false, length = 120)
    private String siteName;

    @Column(nullable = false, unique = true, length = 16)
    private String tag;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 60)
    private String product;

    private int capacityBarrels;

    private double levelPercent;

    protected Tank() {
    }

    public Tank(Long siteId, String siteCode, String siteName, String tag, String name,
                String product, int capacityBarrels, double levelPercent) {
        this.siteId = siteId;
        this.siteCode = siteCode;
        this.siteName = siteName;
        this.tag = tag;
        this.name = name;
        this.product = product;
        this.capacityBarrels = capacityBarrels;
        this.levelPercent = levelPercent;
    }

    public Long getId() {
        return id;
    }

    public Long getSiteId() {
        return siteId;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public String getProduct() {
        return product;
    }

    public int getCapacityBarrels() {
        return capacityBarrels;
    }

    public double getLevelPercent() {
        return levelPercent;
    }

    public void setLevelPercent(double levelPercent) {
        this.levelPercent = levelPercent;
    }
}
