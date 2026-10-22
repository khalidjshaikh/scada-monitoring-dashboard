package com.meridian.scada.repository;

import com.meridian.scada.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findAllByOrderBySortOrderAsc();
}
