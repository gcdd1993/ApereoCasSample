package io.github.gcdd1993.casdemo.app1.config;

import io.github.gcdd1993.casdemo.app1.cas.CasAuthenticationRedirectParamEntryPoint;
import io.github.gcdd1993.casdemo.app1.cas.CasAuthenticationSuccessHandler;
import io.github.gcdd1993.casdemo.app1.cas.DynamicServiceAuthenticationDetailsSource;
import io.github.gcdd1993.casdemo.app1.cas.StatelessSingleSignOutFilter;
import io.github.gcdd1993.casdemo.app1.cookietoken.CookieAuthenticationFilter;
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Configuration
public class DefaultSecurityConfiguration extends WebSecurityConfigurerAdapter implements ImportAware {
    private boolean disableAuth = false;
    private boolean disableSslCheck = false;

    @Value("${app.authn.cas.prefix}")
    private String casPrefix;

    @Value("${app.authn.cas.service}")
    private String thisService;

    @Value("${app.authn.cas.logout-success-url}")
    private String logoutSuccessUrl;

    private final DefaultPatternUserService userService;

    private final InsecureHttpsURLConnectionFactory insecureHttpsURLConnectionFactory = new InsecureHttpsURLConnectionFactory();

    public DefaultSecurityConfiguration(DefaultPatternUserService userService) throws GeneralSecurityException {
        this.userService = userService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        final boolean disableAuth = this.disableAuth;
        http.csrf().disable()
                // 由于API通常部署在反向代理后面，CORS检查没有意义
                .cors().disable();
        // 通常为了高可用部署会禁用spring的session生成，采用cookie验证的方案。
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // 用户访问/logout时，登出本地session，同时跳转到统一登出
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/server/logout", "GET"))
                // 这个地方加入的service参数，代表经CAS登出后再跳回到应用本身的指定页面。
                .logoutSuccessUrl(casPrefix + "/logout?service=" + URLEncoder.encode(logoutSuccessUrl, StandardCharsets.UTF_8))
                .addLogoutHandler(userService::handleLogout)
                .addLogoutHandler(new CookieClearingLogoutHandler("token"))
        );

        // 拦截/login/cas，并处理Service Ticket的认证
        http.addFilter(casAuthenticationFilter());
        // 拦截CAS服务发来的SLO统一登出消息
        http.addFilterBefore(singleSignOutFilter(), LogoutFilter.class);
        // 进行基于cookie的认证
        http.addFilterBefore(cookieAuthenticationFilter(), LogoutFilter.class);
        // 允许一些路径完全绕过认证（optional）
        http.authorizeRequests(authorize -> {
            authorize
                    .antMatchers(
                            "/server/login/cas",
                            "/server/back_channel_logout"
                    ).permitAll()
                    .antMatchers("/server/**").authenticated();
            if (disableAuth) {
                authorize.anyRequest().permitAll();
            } else {
                authorize.anyRequest().authenticated();
            }
        });
        // 配置不同的路径在未认证时采用不同的返回
        http.exceptionHandling(configurer ->
                configurer
                        .defaultAuthenticationEntryPointFor(
                                casAuthenticationEntryPoint(),
                                new AntPathRequestMatcher("/server/landing", "GET")
                        )
                        .defaultAuthenticationEntryPointFor(
                                casGatewayAuthenticationEntryPoint(),
                                new OrRequestMatcher(Arrays.asList(
                                        new AntPathRequestMatcher("/api/**", "GET"),
                                        new AntPathRequestMatcher("/server/probe", "GET")
                                ))
                        )
                        .defaultAuthenticationEntryPointFor(
                                new Http403ForbiddenEntryPoint(),
                                AnyRequestMatcher.INSTANCE));
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(casAuthenticationProvider());
    }

    @Bean
    ServiceProperties serviceProperties() {
        ServiceProperties properties = new ServiceProperties();
        properties.setService(thisService);
        return properties;
    }

    @Bean
    CasAuthenticationFilter casAuthenticationFilter() throws Exception {
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        filter.setFilterProcessesUrl("/server/login/cas");
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationDetailsSource(new DynamicServiceAuthenticationDetailsSource(serviceProperties()));
        CasAuthenticationSuccessHandler successHandler = new CasAuthenticationSuccessHandler();
        successHandler.setCasAuthSuccessCallback(userService::handleCasAuthSuccess);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAllowSessionCreation(false);
        return filter;
    }

    @Bean
    StatelessSingleSignOutFilter singleSignOutFilter() {
        return StatelessSingleSignOutFilter.builder()
                .logoutCallbackPath("/server/back_channel_logout")
                .logoutTicketHandler(userService::handleCasRemoteSingleLogout)
                .build();
    }

    @Bean
    CookieAuthenticationFilter cookieAuthenticationFilter() {
        return new CookieAuthenticationFilter(userService);
    }

    @Bean
    CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationRedirectParamEntryPoint();
        entryPoint.setServiceProperties(serviceProperties());
        entryPoint.setLoginUrl(casPrefix + "/login");
        return entryPoint;
    }

    /**
     * 这个entry point用于非界面交互的场景，在CAS会话不存在的情况下直接返回失败。
     */
    @Bean
    CasAuthenticationEntryPoint casGatewayAuthenticationEntryPoint() {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationRedirectParamEntryPoint();
        entryPoint.setServiceProperties(serviceProperties());
        entryPoint.setLoginUrl(casPrefix + "/login?gateway=true");
        return entryPoint;
    }

    @Bean
    CasAuthenticationProvider casAuthenticationProvider() {
        Cas30ServiceTicketValidator ticketValidator = new Cas30ServiceTicketValidator(casPrefix);
        if (this.disableSslCheck) {
            ticketValidator.setURLConnectionFactory(insecureHttpsURLConnectionFactory);
        }
        CasAuthenticationProvider provider = new CasAuthenticationProvider();
        provider.setServiceProperties(serviceProperties());
        provider.setTicketValidator(ticketValidator);
        provider.setAuthenticationUserDetailsService(userService);
        provider.setKey("cas_auth_provider");
        return provider;
    }

    @Bean
    SingleSignOutHttpSessionListener singleSignOutHttpSessionListener() {
        return new SingleSignOutHttpSessionListener();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        MergedAnnotation<EnableDefaultSecurityConfiguration> annotation = importMetadata.getAnnotations().get(EnableDefaultSecurityConfiguration.class);
        if (annotation.isPresent()) {
            disableAuth = annotation.getBoolean("disableAuth");
            disableSslCheck = annotation.getBoolean("disableSslCheck");
        }
    }
}
