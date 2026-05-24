package org.app.corporateinternetbanking.user.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
}

