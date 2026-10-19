package org.app.corporateinternetbanking.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditJob {
    private final AuditService auditService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runAudit() {
        log.info("Running balance auditing");
        auditService.auditBalances();
    }
}
