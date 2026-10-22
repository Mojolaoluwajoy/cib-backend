package org.app.corporateinternetbanking.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.user.dto.DashboardStats;
import org.app.corporateinternetbanking.user.exceptions.UserNotFound;
import org.app.corporateinternetbanking.user.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard API", description = "Summary figures for the dashboard")
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @Operation(summary = "Stats for the caller's organization, or the whole platform for a super admin")
    @GetMapping("/stats")
    public ResponseEntity<GenericResponse> getStats() throws UserNotFound {
        DashboardStats stats = dashboardService.getStats();
        return ResponseEntity.ok(GenericResponse.success(stats, "Stats loaded"));
    }
}
