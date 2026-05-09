package org.app.corporateinternetbanking.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.integration.paystack.PayStackClient;
import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.organization.domain.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackService {
    private final PayStackClient payStackClient;
    private final OrganizationRepository organizationRepository;

    public String createPaystackCustomer(Organization organization) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", organization.getOrganizationEmail());
        body.put("first_name", organization.getName());

        Map<String, Object> response = payStackClient.createCustomer(body);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        return (String) data.get("customer_code");
    }

    public void createVirtualAccount(String customerCode, Organization organization) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerCode);
        body.put("preferred_bank", "wema-bank");

        Map<String, Object> response = payStackClient.createVirtualAccount(body);
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        organization.setVirtualAccountNumber((String) data.get("account_number"));
        organization.setVirtualAccountBank((String) data.get("bank_name"));
        organizationRepository.save(organization);
    }
}
