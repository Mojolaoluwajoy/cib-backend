package org.app.corporateinternetbanking.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.enums.AccountType;
import org.app.corporateinternetbanking.currency.dto.CurrencyCodeDto;
import org.app.corporateinternetbanking.organization.dto.OrganizationId;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

@Setter
@Getter
public class AccountResponse {
    private String accountNumber;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private AccountType type;
    private OrganizationId organizationId;
    private String organizationName;
    private CurrencyCodeDto currencyCode;
    private UserIdDto createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
