package org.apereo.cas.support.wsfederation.authentication.principal;

import org.apereo.cas.authentication.principal.PersonDirectoryPrincipalResolver;
import org.apereo.cas.support.wsfederation.WsFederationConfiguration;
import org.apereo.cas.authentication.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class resolves the principal id regarding the WsFederation credentials.
 *
 * @author John Gasper
 * @since 4.2.0
 */
public class WsFederationCredentialsToPrincipalResolver extends PersonDirectoryPrincipalResolver {

    private transient Logger logger = LoggerFactory.getLogger(WsFederationCredentialsToPrincipalResolver.class);

    private WsFederationConfiguration configuration;

    /**
     * Extracts the principalId.
     *
     * @param credentials the credentials
     * @return the principal id
     */
    @Override
    protected String extractPrincipalId(final Credential credentials) {
        final WsFederationCredential wsFedCredentials = (WsFederationCredential) credentials;

        final Map<String, List<Object>> attributes = wsFedCredentials.getAttributes();
        logger.debug("Credential attributes provided are: {}", attributes);

        final String idAttribute = this.configuration.getIdentityAttribute();
        if (attributes.containsKey(idAttribute)) {
            logger.debug("Extracting principal id from attribute {}", this.configuration.getIdentityAttribute());

            final List<Object> idAttributeAsList = attributes.get(this.configuration.getIdentityAttribute());
            if (idAttributeAsList.size() > 1) {
                logger.warn("Found multiple values for id attribute {}.", idAttribute);
            }
            final String principalId = idAttributeAsList.get(0).toString();
            logger.debug("Principal Id extracted from credentials: {}", principalId);
            return principalId;
        }

        logger.warn("Credential attributes do not include an attribute for {}. "
                + "This will prohibit CAS to construct a meaningful authenticated principal. "
                + "Examine the released claims and ensure {} is allowed", idAttribute, idAttribute);
        return null;
    }

    @Override
    protected Map<String, List<Object>> retrievePersonAttributes(final String principalId, final Credential credential) {
        final WsFederationCredential wsFedCredentials = (WsFederationCredential) credential;

        if (this.configuration.getAttributesType() == WsFederationConfiguration.WsFedPrincipalResolutionAttributesType.WSFED) {
            return wsFedCredentials.getAttributes();
        }
        if (this.configuration.getAttributesType() == WsFederationConfiguration.WsFedPrincipalResolutionAttributesType.CAS) {
            return super.retrievePersonAttributes(principalId, credential);
        }
        final Map<String, List<Object>> mergedAttributes = new HashMap<>(wsFedCredentials.getAttributes());
        mergedAttributes.putAll(super.retrievePersonAttributes(principalId, credential));
        return mergedAttributes;
    }

    /**
     * Sets the configuration.
     *
     * @param configuration a configuration
     */
    public void setConfiguration(final WsFederationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean supports(final Credential credential) {
        return credential != null && WsFederationCredential.class.isAssignableFrom(credential.getClass());
    }

}
