package com.meridian.scada.web;

import com.meridian.scada.service.EquipmentService;
import com.meridian.scada.web.dto.CommandRequest;
import com.meridian.scada.web.dto.CommandResponse;
import com.meridian.scada.web.dto.EquipmentView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final com.meridian.scada.service.FleetState fleetState;

    public EquipmentController(EquipmentService equipmentService,
                               com.meridian.scada.service.FleetState fleetState) {
        this.equipmentService = equipmentService;
        this.fleetState = fleetState;
    }

    @GetMapping
    public List<EquipmentView> list() {
        return fleetState.equipment().stream().map(ViewMapper::equipment).toList();
    }

    @PostMapping("/{id}/command")
    public CommandResponse command(@PathVariable Long id, @Valid @RequestBody CommandRequest request) {
        return equipmentService.command(id, request.action());
    }
}
