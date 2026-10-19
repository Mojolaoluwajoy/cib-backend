package org.app.corporateinternetbanking.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.dto.AccountNumberDto;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

@Setter
@Getter
public class TransactionApprovalResponse {
    private AccountNumberDto sourceAccount;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private TransactionType type;
    private TransactionStatus status;
    private UserIdDto approver;
    private LocalDateTime approvedAt;
}
