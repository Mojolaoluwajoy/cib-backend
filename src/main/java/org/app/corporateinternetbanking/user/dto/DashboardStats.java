package org.app.corporateinternetbanking.user.dto;

import java.math.BigDecimal;
import lombok.*;

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
