package com.meridian.scada.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "scada_equipment")
public class Equipment {

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

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentMode mode = EquipmentMode.REMOTE;

    @Column(nullable = false)
    private boolean flowControl = true;

    @Column(length = 40)
    private String lastCommand;

    private Instant lastCommandAt;

    protected Equipment() {
    }

    public Equipment(Long siteId, String siteCode, String siteName, String tag, String name,
                     EquipmentType type, EquipmentState state, EquipmentMode mode, boolean flowControl,
                     String lastCommand, Instant lastCommandAt) {
        this.siteId = siteId;
        this.siteCode = siteCode;
        this.siteName = siteName;
        this.tag = tag;
        this.name = name;
        this.type = type;
        this.state = state;
        this.mode = mode;
        this.flowControl = flowControl;
        this.lastCommand = lastCommand;
        this.lastCommandAt = lastCommandAt;
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

    public EquipmentType getType() {
        return type;
    }

    public EquipmentState getState() {
        return state;
    }

    public void setState(EquipmentState state) {
        this.state = state;
    }

    public EquipmentMode getMode() {
        return mode;
    }

    public boolean isFlowControl() {
        return flowControl;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    public Instant getLastCommandAt() {
        return lastCommandAt;
    }

    public void setLastCommandAt(Instant lastCommandAt) {
        this.lastCommandAt = lastCommandAt;
    }
}
