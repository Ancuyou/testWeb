package it.ute.QAUTE.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.ute.QAUTE.entity.Report;
import it.ute.QAUTE.repository.ReportRepository;

@Service
public class ReportService {
    @Autowired
    private ReportRepository reportRepository;

    public void save(Report report) {
        reportRepository.save(report);
    }

}
