package org.app.corporateinternetbanking.organization.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrganizationRequest {

    private String name;
    private String registrationNumber;

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String nin;
    private String organizationEmail;


}
