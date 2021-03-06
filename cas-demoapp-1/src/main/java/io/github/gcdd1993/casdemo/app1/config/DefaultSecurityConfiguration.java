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
                // ??????API????????????????????????????????????CORS??????????????????
                .cors().disable();
        // ????????????????????????????????????spring???session???????????????cookie??????????????????
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // ????????????/logout??????????????????session??????????????????????????????
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/server/logout", "GET"))
                // ?????????????????????service??????????????????CAS???????????????????????????????????????????????????
                .logoutSuccessUrl(casPrefix + "/logout?service=" + URLEncoder.encode(logoutSuccessUrl, StandardCharsets.UTF_8))
                .addLogoutHandler(userService::handleLogout)
                .addLogoutHandler(new CookieClearingLogoutHandler("token"))
        );

        // ??????/login/cas????????????Service Ticket?????????
        http.addFilter(casAuthenticationFilter());
        // ??????CAS???????????????SLO??????????????????
        http.addFilterBefore(singleSignOutFilter(), LogoutFilter.class);
        // ????????????cookie?????????
        http.addFilterBefore(cookieAuthenticationFilter(), LogoutFilter.class);
        // ???????????????????????????????????????optional???
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
        // ?????????????????????????????????????????????????????????
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
     * ??????entry point????????????????????????????????????CAS????????????????????????????????????????????????
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
