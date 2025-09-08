package com.saas.ecommerce.security;

import com.saas.ecommerce.utils.globalExceptionHandller.CustomBusinessException;
import com.saas.ecommerce.utils.globalExceptionHandller.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.saas.ecommerce.security.AuthContext.*;

@Component
public class TenantGuard {
    /** Allow if SUPER_ADMIN or same tenant, else 403 */
    public void sameTenantOrSuper(Long targetClientId) {
        if (isSuperAdmin()) return;
        Long mine = clientIdOrNull();
        if (mine != null && Objects.equals(mine, targetClientId)) return;
        throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "Cross-tenant access denied");
    }
}
