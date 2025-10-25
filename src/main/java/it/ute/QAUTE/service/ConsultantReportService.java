package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.ConsultantReportDTO;
import it.ute.QAUTE.repository.ConsultantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultantReportService {

    @Autowired
    private ConsultantRepository consultantRepository;

    public long getTotalConsultants() {
        return consultantRepository.countAllConsultants();
    }

    public List<ConsultantReportDTO> getPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        return consultantRepository.getConsultantPerformance(startDate, endDate);
    }
}
