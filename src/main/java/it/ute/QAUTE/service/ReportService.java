package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.repository.AnswerRepository;
import it.ute.QAUTE.repository.MessageRepository;
import it.ute.QAUTE.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import it.ute.QAUTE.entity.Report;
import it.ute.QAUTE.repository.ReportRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ReportService {
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private MessageRepository messageRepository;

    public void save(Report report) {
        reportRepository.save(report);
    }

    public Page<Report> searchReports(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String contentType,
            Report.ReportReason reason,
            Report.ReportStatus status,
            Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("status"), Sort.Order.desc("createdAt"))
        );
        String finalContentType = (contentType != null && contentType.isEmpty()) ? null : contentType;

        Page<Report> reportPage = reportRepository.searchReports(
                startDate,
                endDate,
                finalContentType,
                reason,
                status,
                sortedPageable
        );
        populateContentDetails(reportPage.getContent());

        return reportPage;
    }
    private void populateContentDetails(List<Report> reports) {
        for (Report report : reports) {

            if (report.getContentId() == null || report.getContentType() == null || report.getContentType().isEmpty()) {
                continue;
            }

            switch (report.getContentType().toLowerCase()) {
                case "question":
                    questionRepository.findById(report.getContentId().intValue())
                            .ifPresent(report::setQuestion);
                    break;
                case "answer":
                    answerRepository.findById(report.getContentId().intValue())
                            .ifPresent(report::setAnswer);
                    break;
                case "messages":
                    messageRepository.findById(report.getContentId())
                            .ifPresent(report::setMessage);
                    break;
            }
        }
    }

    public long countByStatus(Report.ReportStatus status) {
        return reportRepository.countByStatus(status);
    }

    @Transactional
    public void approveReport(Long reportId, String contentType, Long contentId) {
        Report report = reportRepository.findById(reportId).orElse(null);

        report.setStatus(Report.ReportStatus.PROCESSED);
        reportRepository.save(report);

        if (contentType != null && contentId != null) {
            switch (contentType.toLowerCase()) {
                case "question":
                    questionRepository.findById(contentId.intValue()).ifPresent(question -> {
                        question.setToxic(true);
                        question.setStatus(Question.QuestionStatus.Rejected);
                        questionRepository.save(question);
                    });
                    break;
                case "answer":
                    answerRepository.findById(contentId.intValue()).ifPresent(answer -> {
                        answerRepository.delete(answer);
                    });
                    break;
                case "messages":
                    messageRepository.findById(contentId).ifPresent(message -> {
                        messageRepository.delete(message);
                    });
                    break;
            }
        }
    }


    @Transactional
    public void rejectReport(Long reportId) {
        log.warn(reportId.toString());
        Report report = reportRepository.findById(reportId).orElse(null);

        report.setStatus(Report.ReportStatus.PROCESSED);
        reportRepository.save(report);
    }
}
