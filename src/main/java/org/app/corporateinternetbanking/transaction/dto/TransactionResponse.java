package org.app.corporateinternetbanking.transaction.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.account.dto.AccountNumberDto;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;
import org.app.corporateinternetbanking.transaction.enums.TransactionType;
import org.app.corporateinternetbanking.user.dto.UserIdDto;

@Setter
@Getter
public class TransactionResponse {
    private long id;
    private AccountNumberDto sourceAccount;
    private AccountNumberDto destinationAccount;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String transactionReference;
    private UserIdDto creator;
}
