package org.apereo.cas.authentication;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.support.LdapPasswordPolicyConfiguration;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ReturnAttributes;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;

import javax.annotation.PostConstruct;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LDAP authentication handler that uses the ldaptive {@code Authenticator} component underneath.
 * This handler provides simple attribute resolution machinery by reading attributes from the entry
 * corresponding to the DN of the bound user (in the bound security context) upon successful authentication.
 * Principal resolution is controlled by the following properties:
 * <ul>
 * <li>{@link #setPrincipalIdAttribute(String)}</li>
 * <li>{@link #setPrincipalAttributeMap(java.util.Map)}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 * @since 4.0.0
 */
public class LdapAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {

    private static final String LDAP_ATTRIBUTE_ENTRY_DN = LdapAuthenticationHandler.class.getSimpleName().concat(".dn");

    /**
     * Mapping of LDAP attribute name to principal attribute name.
     */

    protected Map<String, String> principalAttributeMap = Collections.emptyMap();

    /**
     * List of additional attributes to be fetched but are not principal attributes.
     */

    protected List<String> additionalAttributes = Collections.emptyList();

    /**
     * Performs LDAP authentication given username/password.
     **/
    private Authenticator authenticator;

    /**
     * Component name.
     */

    private String name = LdapAuthenticationHandler.class.getSimpleName();

    /**
     * Name of attribute to be used for resolved principal.
     */
    private String principalIdAttribute;

    /**
     * Flag indicating whether multiple values are allowed fo principalIdAttribute.
     */
    private boolean allowMultiplePrincipalAttributeValues;

    /**
     * Set of LDAP attributes fetch from an entry as part of the authentication process.
     */
    private String[] authenticatedEntryAttributes = ReturnAttributes.NONE.value();

    /**
     * Default ctor.
     */
    public LdapAuthenticationHandler() {
    }

    /**
     * Creates a new authentication handler that delegates to the given authenticator.
     *
     * @param authenticator Ldaptive authenticator component.
     */
    public LdapAuthenticationHandler(final Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Sets the component name. Defaults to simple class name.
     *
     * @param name Authentication handler name.
     */
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the name of the LDAP principal attribute whose value should be used for the
     * principal ID.
     *
     * @param attributeName LDAP attribute name.
     */
    public void setPrincipalIdAttribute(final String attributeName) {
        this.principalIdAttribute = attributeName;
    }

    /**
     * Sets a flag that determines whether multiple values are allowed for the {@link #principalIdAttribute}.
     * This flag only has an effect if {@link #principalIdAttribute} is configured. If multiple values are detected
     * when the flag is false, the first value is used and a warning is logged. If multiple values are detected
     * when the flag is true, an exception is raised.
     *
     * @param allowed True to allow multiple principal ID attribute values, false otherwise.
     */
    public void setAllowMultiplePrincipalAttributeValues(final boolean allowed) {
        this.allowMultiplePrincipalAttributeValues = allowed;
    }

    /**
     * Sets the mapping of additional principal attributes where the key is the LDAP attribute
     * name and the value is the principal attribute name. The key set defines the set of
     * attributes read from the LDAP entry at authentication time. Note that the principal ID attribute
     * should not be listed among these attributes.
     *
     * @param attributeNameMap Map of LDAP attribute name to principal attribute name.
     */
    public void setPrincipalAttributeMap(final Map<String, String> attributeNameMap) {
        this.principalAttributeMap = attributeNameMap;
    }

    /**
     * Sets the mapping of additional principal attributes where the key and value is the LDAP attribute
     * name. Note that the principal ID attribute
     * should not be listed among these attributes.
     *
     * @param attributeList List of LDAP attribute names
     */
    public void setPrincipalAttributeList(final List<String> attributeList) {
        this.principalAttributeMap = Maps.uniqueIndex(attributeList, Functions.toStringFunction());
    }

    /**
     * Sets the list of additional attributes to be fetched from the user entry during authentication.
     * These attributes are <em>not</em> bound to the principal.
     * <p>
     * A common use case for these attributes is to support password policy machinery.
     *
     * @param additionalAttributes List of operational attributes to fetch when resolving an entry.
     */
    public void setAdditionalAttributes(final List<String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    public void setAuthenticator(final Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential upc)
            throws GeneralSecurityException, PreventedException {
        final AuthenticationResponse response;
        try {
            logger.debug("Attempting LDAP authentication for {}", upc);

            final AuthenticationRequest request = new AuthenticationRequest(upc.getUsername(),
                    new org.ldaptive.Credential(upc.getPassword()),
                    this.authenticatedEntryAttributes);
            response = this.authenticator.authenticate(request);
        } catch (final LdapException e) {
            logger.trace(e.getMessage(), e);
            throw new PreventedException("Unexpected LDAP error", e);
        }
        logger.debug("LDAP response: {}", response);

        final List<MessageDescriptor> messageList;

        final LdapPasswordPolicyConfiguration ldapPasswordPolicyConfiguration =
                (LdapPasswordPolicyConfiguration) super.getPasswordPolicyConfiguration();
        if (ldapPasswordPolicyConfiguration != null) {
            logger.debug("Applying password policy to {}", response);
            messageList = ldapPasswordPolicyConfiguration.getAccountStateHandler().handle(
                    response, ldapPasswordPolicyConfiguration);
        } else {
            logger.debug("No ldap password policy configuration is defined");
            messageList = Collections.emptyList();
        }

        if (response.getResult()) {
            logger.debug("LDAP response returned as result. Creating the final LDAP principal");
            return createHandlerResult(upc, createPrincipal(upc.getUsername(), response.getLdapEntry()), messageList);
        }

        if (AuthenticationResultCode.DN_RESOLUTION_FAILURE == response.getAuthenticationResultCode()) {
            logger.warn("DN resolution failed. {}", response.getMessage());
            throw new AccountNotFoundException(upc.getUsername() + " not found.");
        }
        throw new FailedLoginException("Invalid credentials");
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Creates a CAS principal with attributes if the LDAP entry contains principal attributes.
     *
     * @param username  Username that was successfully authenticated which is used for principal ID when
     *                  {@link #setPrincipalIdAttribute(String)} is not specified.
     * @param ldapEntry LDAP entry that may contain principal attributes.
     * @return Principal if the LDAP entry contains at least a principal ID attribute value, null otherwise.
     * @throws LoginException On security policy errors related to principal creation.
     */
    protected Principal createPrincipal(final String username, final LdapEntry ldapEntry) throws LoginException {
        logger.debug("Creating LDAP principal for {} based on {}", username, ldapEntry.getDn());
        final String id = getLdapPrincipalIdentifier(username, ldapEntry);

        final Map<String, Object> attributeMap = new LinkedHashMap<>(this.principalAttributeMap.size());
        for (final Map.Entry<String, String> ldapAttr : this.principalAttributeMap.entrySet()) {
            final LdapAttribute attr = ldapEntry.getAttribute(ldapAttr.getKey());
            if (attr != null) {
                logger.debug("Found principal attribute: {}", attr);
                final String principalAttrName = ldapAttr.getValue();
                if (attr.size() > 1) {
                    logger.debug("Principal attribute: {} is multivalued", attr);
                    attributeMap.put(principalAttrName, attr.getStringValues());
                } else {
                    attributeMap.put(principalAttrName, attr.getStringValue());
                }
            }
        }

        attributeMap.put(LDAP_ATTRIBUTE_ENTRY_DN, ldapEntry.getDn());

        logger.debug("Created LDAP principal for id {} and {} attributes", id, attributeMap.size());
        return this.principalFactory.createPrincipal(id, attributeMap);
    }

    /**
     * Gets ldap principal identifier. If the principal id attribute is defined, it's retrieved.
     * If no attribute value is found, a warning is generated and the provided username is used instead.
     * If no attribute is defined, username is used instead.
     *
     * @param username  the username
     * @param ldapEntry the ldap entry
     * @return the ldap principal identifier
     * @throws LoginException in case the principal id cannot be determined.
     */
    protected String getLdapPrincipalIdentifier(final String username, final LdapEntry ldapEntry) throws LoginException {
        if (StringUtils.isNotBlank(this.principalIdAttribute)) {
            final LdapAttribute principalAttr = ldapEntry.getAttribute(this.principalIdAttribute);
            if (principalAttr == null || principalAttr.size() == 0) {
                logger.warn("The principal id attribute [{}] is not found. CAS cannot construct the final authenticated principal "
                            + "if it's unable to locate the attribute that is designated as the principal id. "
                            + "Attributes available on the LDAP entry are [{}]. Since principal id attribute is not available, CAS will "
                            + "fallback to construct the principal based on the provided user id: {}",
                        this.principalIdAttribute, ldapEntry.getAttributes(), username);
                return username;
            }

            if (principalAttr.size() > 1) {
                if (!this.allowMultiplePrincipalAttributeValues) {
                    throw new LoginException("Multiple principal values are not allowed: " + principalAttr);
                }
                logger.warn(
                        "Found multiple values for principal id attribute: {}. Using first value={}.",
                        principalAttr,
                        principalAttr.getStringValue());
            }
            logger.debug("Retrieved principal id attribute {}", principalAttr.getStringValue());
            return principalAttr.getStringValue();
        }
        
        logger.debug("Principal id attribute is not defined. Using the default provided user id {}", username);
        return username;
    }

    /**
     * Initialize the handler, setup the authentication entry attributes.
     */
    @PostConstruct
    public void initialize() {
        /**
         * Use a set to ensure we ignore duplicates.
         */
        final Set<String> attributes = new HashSet<>();

        logger.debug("Initializing LDAP attribute configuration...");
        if (StringUtils.isNotBlank(this.principalIdAttribute)) {
            logger.debug("Configured to retrieve principal id attribute {}", this.principalIdAttribute);
            attributes.add(this.principalIdAttribute);
        }
        if (this.principalAttributeMap != null && !this.principalAttributeMap.isEmpty()) {
            final Set<String> attrs = this.principalAttributeMap.keySet();
            attributes.addAll(attrs);
            logger.debug("Configured to retrieve principal attribute collection of {}", attrs);
        }
        if (this.additionalAttributes != null && !this.additionalAttributes.isEmpty()) {
            attributes.addAll(this.additionalAttributes);
            logger.debug("Configured to retrieve additional attributes {}", this.additionalAttributes);
        }
        if (!attributes.isEmpty()) {
            this.authenticatedEntryAttributes = attributes.toArray(new String[attributes.size()]);
        }

        logger.debug("LDAP authentication entry attributes are {}", this.authenticatedEntryAttributes);
    }
}
