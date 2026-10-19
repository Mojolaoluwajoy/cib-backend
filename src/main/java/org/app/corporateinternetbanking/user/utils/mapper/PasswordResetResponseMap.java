package org.app.corporateinternetbanking.user.utils.mapper;

import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.dto.PasswordResetResponse;

public class PasswordResetResponseMap {

    public static PasswordResetResponse resetResponseMap(User user) {
        PasswordResetResponse response = new PasswordResetResponse();

        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }
}
