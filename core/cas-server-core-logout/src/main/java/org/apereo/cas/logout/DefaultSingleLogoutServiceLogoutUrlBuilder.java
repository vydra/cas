package org.apereo.cas.logout;

import org.apereo.cas.services.RegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * This is {@link DefaultSingleLogoutServiceLogoutUrlBuilder} which acts on a registered
 * service to determine how the logout url endpoint should be decided.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class DefaultSingleLogoutServiceLogoutUrlBuilder implements SingleLogoutServiceLogoutUrlBuilder {
    protected transient Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public URL determineLogoutUrl(final RegisteredService registeredService, final SingleLogoutService singleLogoutService) {
        try {
            URL logoutUrl = new URL(singleLogoutService.getOriginalUrl());
            final URL serviceLogoutUrl = registeredService.getLogoutUrl();

            if (serviceLogoutUrl != null) {
                logger.debug("Logout request will be sent to [{}] for service [{}]",
                        serviceLogoutUrl, singleLogoutService);
                logoutUrl = serviceLogoutUrl;
            }
            return logoutUrl;
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
