package io.github.gcdd1993.casdemo.app1.cookietoken;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {
    @Setter
    private String tokenCookieName = "token";

    private final CookieTokenUserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<String> token = Arrays.stream(cookies).filter(c -> tokenCookieName.equals(c.getName()))
                    .filter(c -> StringUtils.hasText(c.getValue()))
                    .findFirst()
                    .map(Cookie::getValue);
            if (token.isPresent()) {
                UserDetails user = userService.getUserByCookieToken(token.get());
                if (user != null) {
                    CookieAuthenticationToken authenticationToken = new CookieAuthenticationToken(user, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
