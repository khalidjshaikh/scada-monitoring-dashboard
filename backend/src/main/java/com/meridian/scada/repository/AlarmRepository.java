package com.meridian.scada.repository;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.domain.AlarmStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findTop60ByOrderByRaisedAtDesc();

    List<Alarm> findByStatusNotOrderByRaisedAtDesc(AlarmStatus status);

    List<Alarm> findByStatusOrderByRaisedAtDesc(AlarmStatus status);

    Optional<Alarm> findFirstByRuleKeyAndStatusNotOrderByRaisedAtDesc(String ruleKey, AlarmStatus status);
}
