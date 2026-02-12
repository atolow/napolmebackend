package com.dev.napolme.service.logging;

import com.dev.napolme.domain.logging.RequestLog;
import com.dev.napolme.repository.logging.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RequestLogService {

    private static final Logger log = LoggerFactory.getLogger(RequestLogService.class);

    private final RequestLogRepository requestLogRepository;

    public RequestLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    public void save(RequestLog requestLog) {
        requestLogRepository.save(requestLog);
    }

    @Async("requestLogExecutor")
    public void saveAsync(RequestLog requestLog) {
        try {
            requestLogRepository.save(requestLog);
        } catch (Exception ex) {
            log.warn("async request log persist failed id={}", requestLog.getRequestId(), ex);
        }
    }
}
