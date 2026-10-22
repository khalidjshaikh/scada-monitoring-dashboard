package com.meridian.scada.service;

import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.repository.EquipmentRepository;
import com.meridian.scada.repository.SiteRepository;
import com.meridian.scada.repository.TankRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory working set for rapidly changing operational state. H2 remains the
 * system of record; this component batches writes so a 1 Hz simulation does not
 * hammer the database.
 */
@Component
public class FleetState {

    private final SiteRepository siteRepository;
    private final TankRepository tankRepository;
    private final EquipmentRepository equipmentRepository;

    private volatile List<Site> sites = List.of();
    private final Map<Long, Tank> tanks = new LinkedHashMap<>();
    private final Map<Long, Equipment> equipment = new LinkedHashMap<>();
    private volatile boolean loaded;

    public FleetState(SiteRepository siteRepository, TankRepository tankRepository,
                      EquipmentRepository equipmentRepository) {
        this.siteRepository = siteRepository;
        this.tankRepository = tankRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public synchronized void load() {
        sites = List.copyOf(siteRepository.findAllByOrderBySortOrderAsc());
        tanks.clear();
        for (Tank tank : tankRepository.findAllByOrderBySiteIdAscTagAsc()) {
            tanks.put(tank.getId(), tank);
        }
        equipment.clear();
        for (Equipment item : equipmentRepository.findAllByOrderBySiteIdAscTagAsc()) {
            equipment.put(item.getId(), item);
        }
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<Site> sites() {
        return sites;
    }

    public List<Tank> tanks() {
        return tanks.values().stream()
                .sorted(Comparator.comparing(Tank::getSiteCode).thenComparing(Tank::getTag))
                .toList();
    }

    public List<Equipment> equipment() {
        return equipment.values().stream()
                .sorted(Comparator.comparing(Equipment::getSiteId).thenComparing(Equipment::getTag))
                .toList();
    }

    public Equipment equipment(Long id) {
        return equipment.get(id);
    }

    public synchronized void flushAssets() {
        tankRepository.saveAll(tanks.values());
        equipmentRepository.saveAll(equipment.values());
    }

    public void saveEquipment(Equipment item) {
        equipmentRepository.save(item);
    }
}
