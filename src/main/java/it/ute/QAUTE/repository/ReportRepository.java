package it.ute.QAUTE.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.ute.QAUTE.entity.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

}
