package com.meridian.scada.web;

import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentMode;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.domain.TankLevelState;
import com.meridian.scada.web.dto.EquipmentView;
import com.meridian.scada.web.dto.TankView;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ViewMapperTest {

    @Test
    void runningPumpOffersStopOnly() {
        Equipment pump = equipment(EquipmentType.PUMP, EquipmentState.RUNNING, EquipmentMode.REMOTE, true);
        assertThat(ViewMapper.actions(pump)).containsExactly("STOP");
    }

    @Test
    void stoppedAndStandbyPumpsOfferStart() {
        Equipment stopped = equipment(EquipmentType.PUMP, EquipmentState.STOPPED, EquipmentMode.REMOTE, true);
        Equipment standby = equipment(EquipmentType.PUMP, EquipmentState.STANDBY, EquipmentMode.REMOTE, true);
        assertThat(ViewMapper.actions(stopped)).containsExactly("START");
        assertThat(ViewMapper.actions(standby)).containsExactly("START");
    }

    @Test
    void valvesUseOpenCloseSemantics() {
        Equipment open = equipment(EquipmentType.VALVE, EquipmentState.OPEN, EquipmentMode.REMOTE, true);
        Equipment closed = equipment(EquipmentType.VALVE, EquipmentState.CLOSED, EquipmentMode.REMOTE, true);
        assertThat(ViewMapper.actions(open)).containsExactly("CLOSE");
        assertThat(ViewMapper.actions(closed)).containsExactly("OPEN");
    }

    @Test
    void faultedAssetOnlyOffersReset() {
        Equipment faulted = equipment(EquipmentType.COMPRESSOR, EquipmentState.FAULT,
                EquipmentMode.REMOTE, true);
        assertThat(ViewMapper.actions(faulted)).containsExactly("RESET");

        EquipmentView view = ViewMapper.equipment(faulted);
        assertThat(view.state()).isEqualTo(EquipmentState.FAULT);
        assertThat(view.actions()).containsExactly("RESET");
    }

    @Test
    void localControlBlocksRemoteActions() {
        Equipment local = equipment(EquipmentType.PUMP, EquipmentState.RUNNING, EquipmentMode.LOCAL, true);
        assertThat(ViewMapper.actions(local)).isEmpty();
    }

    @Test
    void tankLevelsMapToReserveBands() {
        assertThat(ViewMapper.tank(tank(7.9)).levelState()).isEqualTo(TankLevelState.CRITICAL_LOW);
        assertThat(ViewMapper.tank(tank(12.0)).levelState()).isEqualTo(TankLevelState.LOW);
        assertThat(ViewMapper.tank(tank(48.0)).levelState()).isEqualTo(TankLevelState.NORMAL);
        assertThat(ViewMapper.tank(tank(94.0)).levelState()).isEqualTo(TankLevelState.HIGH);
    }

    @Test
    void tankVolumeIsDerivedFromCapacityAndLevel() {
        TankView view = ViewMapper.tank(tank(50.0));
        assertThat(view.volumeBarrels()).isEqualTo(50_000);
        assertThat(view.levelPercent()).isEqualTo(50.0);
    }

    private static Equipment equipment(EquipmentType type, EquipmentState state,
                                       EquipmentMode mode, boolean flowControl) {
        return new Equipment(1L, "WH-01", "North Wellhead", "P-101", "Mainline transfer pump",
                type, state, mode, flowControl, "START", Instant.now());
    }

    private static Tank tank(double levelPercent) {
        return new Tank(1L, "TF-04", "Red Mesa Tank Farm", "T-401", "Crude storage 401",
                "Light sweet crude", 100_000, levelPercent);
    }
}
