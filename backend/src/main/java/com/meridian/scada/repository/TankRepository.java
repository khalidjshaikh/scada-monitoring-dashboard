package com.meridian.scada.repository;

import com.meridian.scada.domain.Tank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TankRepository extends JpaRepository<Tank, Long> {

    List<Tank> findAllByOrderBySiteIdAscTagAsc();
}
