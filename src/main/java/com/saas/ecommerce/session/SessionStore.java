
package com.saas.ecommerce.session;

import java.time.Duration;

public interface SessionStore {
    void setSid(String username, String sid, Duration ttl);
    String getSid(String username);        // null => not found/expired
    void clearSid(String username);
}
