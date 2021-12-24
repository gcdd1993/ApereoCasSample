package io.github.gcdd1993.casdemo.app1.config;

import io.github.gcdd1993.casdemo.app1.cookietoken.CookieTokenUserService;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DefaultPatternUserService
        extends AuthenticationUserDetailsService<CasAssertionAuthenticationToken>, CookieTokenUserService {

    void handleCasAuthSuccess(HttpServletRequest request, HttpServletResponse response, CasAuthenticationToken authenticationToken, String serviceTicket);

    void handleCasRemoteSingleLogout(String serviceTicket);

    void handleLogout(HttpServletRequest request, HttpServletResponse response, Authentication authentication);
}
