package io.github.gcdd1993.casdemo.app1.cas;

import org.jasig.cas.client.session.SessionMappingStorage;
import org.jasig.cas.client.session.SingleSignOutHandler;
import org.jasig.cas.client.util.AbstractConfigurationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * cas-client提供的SingleSignOutFliter只能基于Servlet Session进行单点登出请求的处理。
 * 这里进行改写，允许用户传入一个handler，自行定义单点登出处理的逻辑。
 */
public final class StatelessSingleSignOutFilter extends AbstractConfigurationFilter {

    private final SingleSignOutHandler handler = new SingleSignOutHandler();

    private final AtomicBoolean handlerInitialized = new AtomicBoolean(false);

    private LogoutTicketHandler logoutTicketHandler = ticket -> {
    };

    public static Builder builder() {
        return new Builder();
    }

    protected StatelessSingleSignOutFilter() {
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (handler.process(request, response)) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    protected void init() {
        handler.setEagerlyCreateSessions(false);
        handler.setSessionMappingStorage(new StatelessSessionMappingStorage());
        handler.init();
        handlerInitialized.set(true);
    }

    private void setArtifactParameterName(final String name) {
        handler.setArtifactParameterName(name);
    }

    private void setLogoutParameterName(final String name) {
        handler.setLogoutParameterName(name);
    }

    private void setRelayStateParameterName(final String name) {
        handler.setRelayStateParameterName(name);
    }

    private void setLogoutCallbackPath(final String logoutCallbackPath) {
        handler.setLogoutCallbackPath(logoutCallbackPath);
    }

    private void setLogoutTicketHandler(LogoutTicketHandler logoutTicketHandler) {
        this.logoutTicketHandler = logoutTicketHandler;
    }

    public interface LogoutTicketHandler {
        void handleLogoutTicket(String serviceTicket);
    }

    public static final class Builder {
        private StatelessSingleSignOutFilter instance = new StatelessSingleSignOutFilter();

        private Builder() {
        }

        public StatelessSingleSignOutFilter build() {
            StatelessSingleSignOutFilter result = this.instance;
            result.init();
            this.instance = null;
            return result;
        }

        public Builder artifactParameterName(final String name) {
            instance.setArtifactParameterName(name);
            return this;
        }

        public Builder logoutParameterName(final String name) {
            instance.setLogoutParameterName(name);
            return this;
        }

        public Builder relayStateParameterName(final String name) {
            instance.setRelayStateParameterName(name);
            return this;
        }

        public Builder logoutCallbackPath(final String logoutCallbackPath) {
            instance.setLogoutCallbackPath(logoutCallbackPath);
            return this;
        }

        public Builder logoutTicketHandler(LogoutTicketHandler logoutTicketHandler) {
            instance.logoutTicketHandler = logoutTicketHandler;
            return this;
        }
    }

    private class StatelessSessionMappingStorage implements SessionMappingStorage {

        @Override
        public HttpSession removeSessionByMappingId(String mappingId) {
            logoutTicketHandler.handleLogoutTicket(mappingId);
            return null;
        }

        @Override
        public void removeBySessionById(String sessionId) {
            // do nothing
        }

        @Override
        public void addSessionById(String mappingId, HttpSession session) {
            // do nothing
        }
    }
}
