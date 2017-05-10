package org.apereo.cas.support.saml;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasPersonDirectoryAttributeRepositoryConfiguration;
import org.apereo.cas.config.CoreSamlConfiguration;
import org.apereo.cas.config.SamlConfiguration;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.validation.config.CasCoreValidationConfiguration;
import org.apereo.cas.web.config.CasProtocolViewsConfiguration;
import org.apereo.cas.web.config.CasValidationConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.*;

/**
 * OpenSaml context loading tests.
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {CoreSamlConfiguration.class,
                SamlConfiguration.class,
                RefreshAutoConfiguration.class,
                CasCoreWebConfiguration.class,
                CasPersonDirectoryAttributeRepositoryConfiguration.class,
                CasCoreServicesConfiguration.class,
                CasCoreValidationConfiguration.class,
                CasProtocolViewsConfiguration.class,
                CasValidationConfiguration.class,
                CasCoreAuthenticationConfiguration.class,
                CasCoreTicketsConfiguration.class,
                CasCoreLogoutConfiguration.class,
                CasCoreUtilConfiguration.class,
                CasCoreConfiguration.class})
@ContextConfiguration(locations = "classpath:/opensaml-config.xml")
@WebAppConfiguration
public abstract class AbstractOpenSamlTests {

    protected static final String SAML_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
            + "ID=\"5545454455\" Version=\"2.0\" IssueInstant=\"Value\" "
            + "ProtocolBinding=\"urn:oasis:names.tc:SAML:2.0:bindings:HTTP-Redirect\" "
            + "ProviderName=\"https://localhost:8443/myRutgers\" "
            + "AssertionConsumerServiceURL=\"https://localhost:8443/myRutgers\"/>";

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected OpenSamlConfigBean configBean;

    @Autowired
    protected ParserPool parserPool;

    @Autowired
    @Qualifier("shibboleth.BuilderFactory")
    protected XMLObjectBuilderFactory builderFactory;

    @Autowired
    @Qualifier("shibboleth.MarshallerFactory")
    protected MarshallerFactory marshallerFactory;

    @Autowired
    @Qualifier("shibboleth.UnmarshallerFactory")
    protected UnmarshallerFactory unmarshallerFactory;

    @Test
    public void autowireApplicationContext() {
        assertNotNull(this.applicationContext);
        assertNotNull(this.configBean);
        assertNotNull(this.parserPool);
        assertNotNull(this.builderFactory);
        assertNotNull(this.unmarshallerFactory);
        assertNotNull(this.marshallerFactory);
        assertNotNull(this.configBean.getParserPool());
    }

    @Test
    public void loadStaticContextFactories() {
        assertNotNull(XMLObjectProviderRegistrySupport.getParserPool());
        assertNotNull(XMLObjectProviderRegistrySupport.getBuilderFactory());
        assertNotNull(XMLObjectProviderRegistrySupport.getMarshallerFactory());
        assertNotNull(XMLObjectProviderRegistrySupport.getUnmarshallerFactory());
    }


    @Test
    public void ensureParserIsInitialized() throws Exception {
        assertNotNull(this.parserPool);
        assertNotNull(this.parserPool.getBuilder());
    }
}
