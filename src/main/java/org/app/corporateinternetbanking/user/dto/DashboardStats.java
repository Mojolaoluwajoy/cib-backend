package org.app.corporateinternetbanking.user.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStats {
    private int totalAccounts;
    private int pendingOnboarding;
    private int pendingTransactions;
    private BigDecimal transferVolume;
    private BigDecimal payoutVolume;
    private int totalOrganizations;
    private int approvedOrganizations;
    private int rejectedOrganizations;
    private int disabledOrganizations;
}