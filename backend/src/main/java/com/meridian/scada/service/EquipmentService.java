package com.meridian.scada.service;

import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentMode;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;
import com.meridian.scada.web.ApiException;
import com.meridian.scada.web.ViewMapper;
import com.meridian.scada.web.dto.CommandResponse;
import com.meridian.scada.web.dto.EquipmentView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EquipmentService {

    private final FleetState fleetState;
    private final AlarmService alarmService;

    public EquipmentService(FleetState fleetState, AlarmService alarmService) {
        this.fleetState = fleetState;
        this.alarmService = alarmService;
    }

    @Transactional
    public CommandResponse command(Long equipmentId, String requestedAction) {
        String action = requestedAction.toUpperCase();
        Equipment item;
        synchronized (this) {
            item = fleetState.equipment(equipmentId);
            if (item == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Remote asset not found");
            }
            if (item.getMode() == EquipmentMode.LOCAL) {
                throw new ApiException(HttpStatus.CONFLICT,
                        item.getTag() + " is in local control and cannot be operated remotely");
            }
            apply(item, action);
            item.setLastCommand(action);
            item.setLastCommandAt(Instant.now());
            fleetState.saveEquipment(item);
        }
        if ("RESET".equals(action)) {
            alarmService.clearRule("equipment:" + item.getTag() + ":fault");
        }
        return new CommandResponse(true,
                "%s %s command acknowledged · %s".formatted(item.getTag(), action, item.getState()),
                ViewMapper.equipment(item));
    }

    public EquipmentView view(Long equipmentId) {
        Equipment item = fleetState.equipment(equipmentId);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Remote asset not found");
        }
        return ViewMapper.equipment(item);
    }

    private void apply(Equipment item, String action) {
        EquipmentState state = item.getState();
        boolean valve = item.getType() == EquipmentType.VALVE;
        switch (action) {
            case "START" -> {
                if (valve) {
                    throw new ApiException(HttpStatus.CONFLICT, "Valves are opened, not started");
                }
                if (state == EquipmentState.FAULT) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Reset the equipment fault before starting");
                }
                if (state == EquipmentState.RUNNING) {
                    throw new ApiException(HttpStatus.CONFLICT, item.getTag() + " is already running");
                }
                item.setState(EquipmentState.RUNNING);
            }
            case "STOP" -> {
                if (valve) {
                    throw new ApiException(HttpStatus.CONFLICT, "Valves are closed, not stopped");
                }
                if (state == EquipmentState.FAULT) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Reset the equipment fault before issuing a stop");
                }
                if (state == EquipmentState.STOPPED) {
                    throw new ApiException(HttpStatus.CONFLICT, item.getTag() + " is already stopped");
                }
                item.setState(EquipmentState.STOPPED);
            }
            case "OPEN" -> {
                if (!valve) {
                    throw new ApiException(HttpStatus.CONFLICT, "Open is only valid for valves");
                }
                if (state == EquipmentState.FAULT) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Reset the valve fault before operating it");
                }
                if (state == EquipmentState.OPEN) {
                    throw new ApiException(HttpStatus.CONFLICT, item.getTag() + " is already open");
                }
                item.setState(EquipmentState.OPEN);
            }
            case "CLOSE" -> {
                if (!valve) {
                    throw new ApiException(HttpStatus.CONFLICT, "Close is only valid for valves");
                }
                if (state == EquipmentState.FAULT) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Reset the valve fault before operating it");
                }
                if (state == EquipmentState.CLOSED) {
                    throw new ApiException(HttpStatus.CONFLICT, item.getTag() + " is already closed");
                }
                item.setState(EquipmentState.CLOSED);
            }
            case "RESET" -> {
                if (state != EquipmentState.FAULT) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            item.getTag() + " has no active fault to reset");
                }
                item.setState(valve ? EquipmentState.CLOSED : EquipmentState.STOPPED);
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported command");
        }
    }
}
