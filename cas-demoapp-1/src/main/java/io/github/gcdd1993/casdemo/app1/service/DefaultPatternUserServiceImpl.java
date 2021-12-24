package io.github.gcdd1993.casdemo.app1.service;

import io.github.gcdd1993.casdemo.app1.config.DefaultPatternUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gcdd1993
 * @since 2021/12/24
 */
@Slf4j
@Service
public class DefaultPatternUserServiceImpl implements DefaultPatternUserService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, String> tokenUserMapping = new HashMap<>(16);

    @Override
    public void handleCasAuthSuccess(HttpServletRequest request, HttpServletResponse response, CasAuthenticationToken authenticationToken, String serviceTicket) {
        log.info("cas auth success, tgt: {}", serviceTicket);
        UserDetails userDetails = authenticationToken.getUserDetails();
        String token = generateToken();

        // 登录成功后，设置Cookie
        Cookie cookie = new Cookie("token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(Math.toIntExact(60 * 60 * 4));
        response.addCookie(cookie);

        // 绑定 token 和 username
        tokenUserMapping.put(token, userDetails.getUsername());
    }

    /**
     * 处理SLO事件，其他客户端登出后，可以通过回调接口通知其他应用退出登录
     */
    @Override
    public void handleCasRemoteSingleLogout(String serviceTicket) {
        // 这里未删除cookie，所以在其他地方必须处理token过期
        log.info("cas remote single out success, tgt: {}", serviceTicket);
    }

    /**
     * 处理登出事件，比如删除Cookie，Session等操作
     */
    @Override
    public void handleLogout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("logout {}", authentication.getPrincipal().toString());
    }

    /**
     * 通过token获取用户信息
     */
    @Override
    public UserDetails getUserByCookieToken(String token) {
        log.info("get user by cookie token {}", token);
        // name = findNameByToken，通过token获取username
        String username = tokenUserMapping.get(token);
        return new User(username, "", Collections.emptyList());
    }

    /**
     * 这里是在CAS系统登录成功后，回调到应用时触发
     */
    @Override
    public UserDetails loadUserDetails(CasAssertionAuthenticationToken token) throws UsernameNotFoundException {
        String name = (String) token.getPrincipal();
        log.info("cas login success, pretend load user details {}", name);
        return new User(name, "", Collections.emptyList());
    }

    private String generateToken() {
        byte[] bytes = new byte[96];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

}
