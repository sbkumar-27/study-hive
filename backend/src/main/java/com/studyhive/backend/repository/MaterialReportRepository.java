package com.studyhive.backend.repository;

import com.studyhive.backend.entity.MaterialReport;
import com.studyhive.backend.entity.ReportStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialReportRepository extends JpaRepository<MaterialReport, Long> {

    Page<MaterialReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<MaterialReport> findByStatusAndMaterial_Community_CommunityIdIn(
            ReportStatus status, List<Long> communityIds, Pageable pageable);
}
