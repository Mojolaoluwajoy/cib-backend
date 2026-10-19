package org.app.corporateinternetbanking.security;

import org.app.corporateinternetbanking.organization.domain.entity.Organization;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.app.corporateinternetbanking.user.exceptions.UnauthorizedAccess;

// Tenant isolation rules: SUPER_ADMIN operates the platform and may see every organization;
// every other user is confined to the organization they belong to.
public final class TenantAccess {

    private TenantAccess() {}

    public static boolean isSuperAdmin(User user) {
        return user.getRole() == UserRole.SUPER_ADMIN;
    }

    public static boolean belongsTo(User user, Organization organization) {
        return organization != null
                && user.getOrganization() != null
                && user.getOrganization().getId().equals(organization.getId());
    }

    public static boolean canAccess(User user, Organization organization) {
        return isSuperAdmin(user) || belongsTo(user, organization);
    }

    public static void assertCanAccess(User user, Organization organization) throws UnauthorizedAccess {
        if (!canAccess(user, organization)) {
            throw new UnauthorizedAccess("You do not have access to this organization's resources");
        }
    }

    // For actions that must be done by a member of the organization itself (e.g. moving its money),
    // where even SUPER_ADMIN acting from outside the organization is not allowed.
    public static void assertMemberOf(User user, Organization organization) throws UnauthorizedAccess {
        if (!belongsTo(user, organization)) {
            throw new UnauthorizedAccess("This account does not belong to your organization");
        }
    }
}
