package org.app.corporateinternetbanking.organization.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;

@Setter
@Getter
public class UpdateOrganizationRequest {
    private String name;
    private String organizationEmail;
    private String registrationNumber;
    private OrganizationStatus organizationStatus;
    private String phoneNumber;
}