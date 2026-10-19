package org.app.corporateinternetbanking.ledger.utils.mapper;

import org.app.corporateinternetbanking.ledger.domain.entity.LedgerEntry;
import org.app.corporateinternetbanking.ledger.dto.LedgerCreationRequest;
import org.app.corporateinternetbanking.ledger.dto.LedgerCreationResponse;

public class LedgerMap {

    public static LedgerEntry mapCreationRequest(LedgerCreationRequest request) {
        LedgerEntry ledgerEntry = new LedgerEntry();

        ledgerEntry.setAccount(request.getAccount());
        ledgerEntry.setTransaction(request.getTransaction());
        ledgerEntry.setAmount(request.getAmount());
        ledgerEntry.setType(request.getEntryType());
        ledgerEntry.setBalanceAfter(request.getBalanceAfter());
        return ledgerEntry;
    }

    public static LedgerCreationResponse mapCreationResponse(LedgerEntry ledgerEntry) {
        LedgerCreationResponse response = new LedgerCreationResponse();
        response.setId(ledgerEntry.getId());
        response.setAccount(ledgerEntry.getAccount());
        response.setTransaction(ledgerEntry.getTransaction());
        response.setAmount(ledgerEntry.getAmount());
        response.setCurrency(ledgerEntry.getCurrency());
        response.setType(ledgerEntry.getType());
        response.setBalanceAfter(ledgerEntry.getBalanceAfter());
        response.setCreatedAt(ledgerEntry.getCreatedAt());
        return response;
    }
}
