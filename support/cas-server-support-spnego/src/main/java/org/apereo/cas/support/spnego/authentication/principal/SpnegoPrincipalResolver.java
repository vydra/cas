package org.apereo.cas.support.spnego.authentication.principal;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.PersonDirectoryPrincipalResolver;

/**
 * Implementation of a CredentialToPrincipalResolver that takes a
 * SpnegoCredential and returns a SimplePrincipal.
 *
 * @author Arnaud Lesueur
 * @author Marc-Antoine Garrigue
 * @since 3.1
 */
public class SpnegoPrincipalResolver extends PersonDirectoryPrincipalResolver {

    @Override
    protected String extractPrincipalId(final Credential credential) {
        final SpnegoCredential c = (SpnegoCredential) credential;
        final String id = c.getPrincipal().getId();
        return id;
    }

    @Override
    public boolean supports(final Credential credential) {
        return credential != null
                && SpnegoCredential.class.equals(credential.getClass());
    }
}
