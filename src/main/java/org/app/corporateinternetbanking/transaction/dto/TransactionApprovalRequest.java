package org.app.corporateinternetbanking.transaction.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.transaction.enums.TransactionStatus;

@Setter
@Getter
public class TransactionApprovalRequest {
    private long transactionId;
    private TransactionStatus status;

}
