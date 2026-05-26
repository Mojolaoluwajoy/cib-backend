package org.app.corporateinternetbanking.organization.dto;

import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.organization.enums.OrganizationStatus;

@Setter
@Getter
public class OrganizationOnlyResponse {
    private Long id;
    private String name;
    private Long adminId;
    private String registrationNumber;
    private OrganizationStatus organizationStatus;


}
