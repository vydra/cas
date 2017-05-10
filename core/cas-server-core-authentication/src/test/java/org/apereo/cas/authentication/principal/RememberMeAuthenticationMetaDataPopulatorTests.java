package org.apereo.cas.authentication.principal;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.BasicCredentialMetaData;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.authentication.TestUtils;
import org.apereo.cas.authentication.AuthenticationBuilder;
import org.apereo.cas.authentication.CredentialMetaData;
import org.apereo.cas.authentication.DefaultHandlerResult;
import org.apereo.cas.authentication.RememberMeCredential;
import org.apereo.cas.authentication.RememberMeUsernamePasswordCredential;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.SimpleTestUsernamePasswordAuthenticationHandler;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Scott Battaglia
 * @since 3.2.1
 *
 */
public class RememberMeAuthenticationMetaDataPopulatorTests {

    private RememberMeAuthenticationMetaDataPopulator p = new RememberMeAuthenticationMetaDataPopulator();

    @Test
    public void verifyWithTrueRememberMeCredentials() {
        final RememberMeUsernamePasswordCredential c = new RememberMeUsernamePasswordCredential();
        c.setRememberMe(true);
        final AuthenticationBuilder builder = newBuilder(c);
        final Authentication auth = builder.build();

        assertEquals(true, auth.getAttributes().get(RememberMeCredential.AUTHENTICATION_ATTRIBUTE_REMEMBER_ME));
    }

    @Test
    public void verifyWithFalseRememberMeCredentials() {
        final RememberMeUsernamePasswordCredential c = new RememberMeUsernamePasswordCredential();
        c.setRememberMe(false);
        final AuthenticationBuilder builder = newBuilder(c);
        final Authentication auth = builder.build();

        assertNull(auth.getAttributes().get(RememberMeCredential.AUTHENTICATION_ATTRIBUTE_REMEMBER_ME));
    }

    @Test
    public void verifyWithoutRememberMeCredentials() {
        final AuthenticationBuilder builder = newBuilder(TestUtils.getCredentialsWithSameUsernameAndPassword());
        final Authentication auth = builder.build();

        assertNull(auth.getAttributes().get(RememberMeCredential.AUTHENTICATION_ATTRIBUTE_REMEMBER_ME));
    }

    private AuthenticationBuilder newBuilder(final Credential credential) {
        final CredentialMetaData meta = new BasicCredentialMetaData(new UsernamePasswordCredential());
        final AuthenticationHandler handler = new SimpleTestUsernamePasswordAuthenticationHandler();
        final AuthenticationBuilder builder = new DefaultAuthenticationBuilder(TestUtils.getPrincipal())
                .addCredential(meta)
                .addSuccess("test", new DefaultHandlerResult(handler, meta));

        if (this.p.supports(credential)) {
            this.p.populateAttributes(builder, credential);
        }
        return builder;
    }

}
