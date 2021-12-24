package io.github.gcdd1993.casdemo.app1.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/server")
public class DefaultPatternController {

    /**
     * 请求此接口，将会判断是否登录，若未登录，将会重定向到CAS登录页
     */
    @GetMapping("/landing")
    protected void landing(
            @RequestParam(name = "to", required = false, defaultValue = "/") String toPath,
            HttpServletResponse response
    ) {
        // 不允许重定向到绝对路径，以防御某些类型的跨站攻击
        if (!UrlUtils.isValidRedirectUrl(toPath) ||
                UrlUtils.isAbsoluteUrl(toPath) ||
                toPath.startsWith("//")
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid redirect path");
        }
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        response.addHeader("Location", toPath);
    }

    /**
     * 判断CAS登录是否过期，但是不会重定向
     */
    @GetMapping("/probe")
    protected void probe() {
        // Do nothing, just for auth checking purpose
    }
}
