package io.github.gcdd1993.casdemo.app1.cas;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 用于配置{@link org.springframework.security.cas.web.CasAuthenticationFilter}。
 * 不使用这个类时，默认逻辑是从本地缓存中获得之前被拦截的请求url，创建session并跳转回此url。
 * 当应用集群式部署时，基于本地缓存进行跳转不再有效。使用这个类将读取url中的redirect参数
 * （可配置，需与{@link CasAuthenticationRedirectParamEntryPoint}中的配置对应），并
 * 跳转到指定的url。
 * 同时这个类也提供了一个对于CAS认证成功的回调{@link #setCasAuthSuccessCallback}，应用可在这里加入token创建相关的逻辑。
 */
public class CasAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(CasAuthenticationSuccessHandler.class);

    @Setter
    private CasAuthenticationSuccessCallback casAuthSuccessCallback = (request, response, auth, st) -> {
    };

    public CasAuthenticationSuccessHandler() {
        setRedirectStrategy(new NoSessionRedirectStrategy());
        setTargetUrlParameter("redirect");
    }

    @Override
    protected final void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof CasAuthenticationToken && authentication.isAuthenticated()) {
            CasAuthenticationToken casToken = (CasAuthenticationToken) authentication;
            List<String> ticketParam = URLUtil.parseQuery(request.getQueryString()).get("ticket");
            if (ticketParam == null || ticketParam.size() != 1) {
                throw new IllegalStateException("None or multiple service tickets presented in url");
            }
            String serviceTicket = ticketParam.get(0);
            try {
                casAuthSuccessCallback.handleCasAuthSuccess(request, response, casToken, serviceTicket);
            } catch (Exception e) {
                log.error("Error calling casAuthSuccessCallback", e);
            }
        }
        super.handle(request, response, authentication);
    }

    /**
     * 这个类与其父类的唯一差别是跳过了使用sessionId对跳转url进行encode的步骤。
     */
    private static class NoSessionRedirectStrategy extends DefaultRedirectStrategy {
        @Override
        public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
            String redirectUrl = calculateRedirectUrl(request.getContextPath(), url);

            if (logger.isDebugEnabled()) {
                logger.debug("Redirecting to '" + redirectUrl + "'");
            }

            // 绕过http 1.0兼容的重定向路径转绝对路径逻辑，手动设置重定向相关信息。
            response.setStatus(303);
            response.setHeader("Location", response.encodeRedirectURL(redirectUrl));
        }
    }

    public interface CasAuthenticationSuccessCallback {
        void handleCasAuthSuccess(HttpServletRequest request, HttpServletResponse response, CasAuthenticationToken authenticationToken, String serviceTicket);
    }
}
