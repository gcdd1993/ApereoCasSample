package io.github.gcdd1993.casdemo.app1.cas;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * 用于配置{@link org.springframework.security.cas.web.CasAuthenticationFilter}。
 * 默认情况下，当CAS跳转回应用的/login/cas节点时，应用将以固定的不带参数的/login/cas URL去验证ST (Service Ticket).
 * 当/login/cas附带跳转参数，用于登录成功后跳转时，将出现请求ST和验证ST所用的serviceId不同的情况，从而验证失败。
 * 这个类的作用是动态地获取当前的/login/cas路径后附带的参数，并附加在验证ST的请求中，从而使得整个流程可用。
 */
public class DynamicServiceAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, ServiceAuthenticationDetails> {
    private static final Pattern TICKET_PARAM_PATTERN = Pattern.compile("\\b&?ticket=[^?&]*");
    private final String service;
    private final String separator;

    public DynamicServiceAuthenticationDetailsSource(ServiceProperties serviceProperties) {
        service = serviceProperties.getService();
        separator = service.contains("?") ? "&" : "?";
    }

    @Override
    public ServiceAuthenticationDetails buildDetails(HttpServletRequest context) {
        return () -> {
            String requestURI = context.getRequestURI();
            String queryString = context.getQueryString();
            assert service.endsWith(requestURI);
            return service + separator + TICKET_PARAM_PATTERN.matcher(queryString).replaceFirst("");
        };
    }
}
