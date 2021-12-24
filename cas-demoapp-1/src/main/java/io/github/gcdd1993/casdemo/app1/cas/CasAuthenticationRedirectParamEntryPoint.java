package io.github.gcdd1993.casdemo.app1.cas;

import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于拦截需要进行CAS认证的路径，跳转到cas认证url，并将当前的url作为跳转参数拼接到认证url中。
 * 这个跳转参数最终将在CAS认证完成跳转会应用时，由{@link CasAuthenticationSuccessHandler}捕捉并处理。
 * 默认的跳转参数名为redirect。
 */
public class CasAuthenticationRedirectParamEntryPoint extends CasAuthenticationEntryPoint {

    @Setter
    private String redirectParam = "redirect";

    public CasAuthenticationRedirectParamEntryPoint() {
        this.setEncodeServiceUrlWithSessionId(false);
    }

    @Override
    protected String createServiceUrl(HttpServletRequest request, HttpServletResponse response) {
        String serviceUrl = super.createServiceUrl(request, response);
        if (!HttpMethod.GET.matches(request.getMethod())) {
            // 如果不是get请求，就不要附加redirect参数了
            return serviceUrl;
        }
        if (serviceUrl.contains("?") && !serviceUrl.endsWith("&")) {
            serviceUrl += "&";
        } else {
            serviceUrl += "?";
        }
        serviceUrl += redirectParam + "=" + URLUtil.urlEncode(getRequestURIWithQuery(request));
        return serviceUrl;
    }

    private String getRequestURIWithQuery(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (!StringUtils.hasText(queryString)) {
            return request.getRequestURI();
        } else {
            return request.getRequestURI() + "?" + queryString;
        }
    }

}
