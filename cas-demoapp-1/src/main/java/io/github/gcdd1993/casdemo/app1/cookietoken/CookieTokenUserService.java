package io.github.gcdd1993.casdemo.app1.cookietoken;

import org.springframework.security.core.userdetails.UserDetails;

public interface CookieTokenUserService {
    UserDetails getUserByCookieToken(String token);
}
