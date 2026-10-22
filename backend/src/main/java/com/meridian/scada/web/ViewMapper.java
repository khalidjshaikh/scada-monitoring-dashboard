package com.meridian.scada.web;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentMode;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.domain.TankLevelState;
import com.meridian.scada.web.dto.AlarmView;
import com.meridian.scada.web.dto.EquipmentView;
import com.meridian.scada.web.dto.TankView;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ViewMapper {

    private ViewMapper() {
    }

    public static TankView tank(Tank tank) {
        double volume = tank.getCapacityBarrels() * tank.getLevelPercent() / 100.0;
        return new TankView(
                tank.getId(),
                tank.getSiteId(),
                tank.getSiteCode(),
                tank.getSiteName(),
                tank.getTag(),
                tank.getName(),
                tank.getProduct(),
                tank.getCapacityBarrels(),
                round(tank.getLevelPercent(), 1),
                Math.round(volume),
                tankLevelState(tank.getLevelPercent()));
    }

    public static EquipmentView equipment(Equipment item) {
        return new EquipmentView(
                item.getId(),
                item.getSiteId(),
                item.getSiteCode(),
                item.getSiteName(),
                item.getTag(),
                item.getName(),
                item.getType(),
                item.getState(),
                item.getMode(),
                item.isFlowControl(),
                item.getLastCommand(),
                item.getLastCommandAt(),
                actions(item));
    }

    public static AlarmView alarm(Alarm alarm, Instant now) {
        long age = Math.max(0, Duration.between(alarm.getRaisedAt(), now).toSeconds());
        return new AlarmView(
                alarm.getId(),
                alarm.getCode(),
                alarm.getSeverity(),
                alarm.getStatus(),
                alarm.getMessage(),
                alarm.getSiteId(),
                alarm.getSiteCode(),
                alarm.getSiteName(),
                alarm.getSource(),
                alarm.getRaisedAt(),
                alarm.getAcknowledgedAt(),
                alarm.getAcknowledgedBy(),
                alarm.getClearedAt(),
                age);
    }

    public static List<String> actions(Equipment item) {
        if (item.getMode() == EquipmentMode.LOCAL) {
            return List.of();
        }
        if (item.getState() == EquipmentState.FAULT) {
            return List.of("RESET");
        }
        List<String> actions = new ArrayList<>(2);
        if (item.getType() == EquipmentType.VALVE) {
            if (item.getState() == EquipmentState.OPEN) {
                actions.add("CLOSE");
            } else if (item.getState() == EquipmentState.CLOSED) {
                actions.add("OPEN");
            }
        } else {
            if (item.getState() == EquipmentState.RUNNING) {
                actions.add("STOP");
            } else if (item.getState() == EquipmentState.STOPPED
                    || item.getState() == EquipmentState.STANDBY) {
                actions.add("START");
            }
        }
        return List.copyOf(actions);
    }

    private static TankLevelState tankLevelState(double level) {
        if (level < 8) {
            return TankLevelState.CRITICAL_LOW;
        }
        if (level < 15) {
            return TankLevelState.LOW;
        }
        if (level > 92) {
            return TankLevelState.HIGH;
        }
        return TankLevelState.NORMAL;
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
