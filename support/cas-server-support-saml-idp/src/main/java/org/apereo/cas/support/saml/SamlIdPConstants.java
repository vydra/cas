package org.apereo.cas.support.saml;

/**
 * Class that exposes relevant constants and parameters to
 * the SAML IdP.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public interface SamlIdPConstants {

    /** The IdP metadata endpoint. */
    String ENDPOINT_IDP_METADATA = "/idp/metadata";

    /** The RP metadata generation endpoint. */
    String ENDPOINT_GENERATE_RP_METADATA = "/idp/servicemetadatagen";

    /** The SAML2 SSO post profile endpoint. */
    String ENDPOINT_SAML2_SSO_PROFILE_POST = "/idp/profile/SAML2/POST/SSO";

    /** The SAML2 SSO redirect profile endpoint. */
    String ENDPOINT_SAML2_SSO_PROFILE_REDIRECT = "/idp/profile/SAML2/Redirect/SSO";

    /** The SAML2 SLO post endpoint. */
    String ENDPOINT_SAML2_SLO_PROFILE_POST = "/idp/profile/SAML2/POST/SLO";

    /** The SAML2 IDP initiated endpoint. */
    String ENDPOINT_SAML2_IDP_INIT_PROFILE_SSO = "/idp/profile/SAML2/Unsolicited/SSO";
    
    /** The SAML2 SSO post callback profile endpoint. */
    String ENDPOINT_SAML2_SSO_PROFILE_POST_CALLBACK = "/idp/profile/SAML2/POST/SSO/Callback";

    /** The shire constant. */
    String SHIRE = "shire";

    /** The provider id constant. */
    String PROVIDER_ID = "providerId";

    /** The target constant. */
    String TARGET = "target";

    /** The time constant. */
    String TIME = "time";
}

