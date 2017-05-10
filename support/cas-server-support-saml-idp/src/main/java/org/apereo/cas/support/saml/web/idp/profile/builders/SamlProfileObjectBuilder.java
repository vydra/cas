package org.apereo.cas.support.saml.web.idp.profile.builders;

import org.apereo.cas.support.saml.SamlException;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlRegisteredServiceServiceProviderMetadataFacade;
import org.jasig.cas.client.validation.Assertion;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link SamlProfileObjectBuilder} defines the operations
 * required for building the saml response for an RP.
 *
 * @author Misagh Moayyed
 * @param <T> the type parameter
 * @since 5.0.0
 */
public interface SamlProfileObjectBuilder<T extends SAMLObject> {

    /**
     * Build response.
     *
     * @param authnRequest the authn request
     * @param request      the request
     * @param response     the response
     * @param assertion    the assertion
     * @param service      the service
     * @param adaptor      the adaptor
     * @return the response
     * @throws SamlException the exception
     */
    T build(AuthnRequest authnRequest, HttpServletRequest request,
            HttpServletResponse response, Assertion assertion,
            SamlRegisteredService service, SamlRegisteredServiceServiceProviderMetadataFacade adaptor) throws SamlException;
}
