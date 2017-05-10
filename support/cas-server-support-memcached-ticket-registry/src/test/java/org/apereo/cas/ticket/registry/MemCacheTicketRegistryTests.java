package org.apereo.cas.ticket.registry;

import org.apereo.cas.AbstractMemcachedTests;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.TestUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.mock.MockServiceTicket;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.proxy.ProxyGrantingTicket;
import org.apereo.cas.ticket.support.AlwaysExpiresExpirationPolicy;
import org.apereo.cas.ticket.support.NeverExpiresExpirationPolicy;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;


/**
 * Unit test for MemCacheTicketRegistry class.
 *
 * @author Middleware Services
 * @since 3.0.0
 */

@RunWith(Parameterized.class)
public class MemCacheTicketRegistryTests extends AbstractMemcachedTests {

    private MemCacheTicketRegistry registry;

    private final String registryBean;

    public MemCacheTicketRegistryTests(final String beanName) {
        registryBean = beanName;
    }

    @Parameterized.Parameters
    public static Collection<Object> getTestParameters() throws Exception {
        return Arrays.asList(new Object[]{"testCase1"}, new Object[]{"testCase2"});
    }


    @Before
    public void setUp() throws IOException {
        final boolean environmentOk = isMemcachedListening();
        if (!environmentOk) {
            logger.warn("Aborting test since no memcached server is available on localhost.");
        }
        Assume.assumeTrue(environmentOk);
        final ApplicationContext context = new ClassPathXmlApplicationContext("/ticketRegistry-test.xml");
        registry = context.getBean(registryBean, MemCacheTicketRegistry.class);
    }

    @Test
    public void verifyWriteGetDelete() throws Exception {
        final String id = "ST-1234567890ABCDEFGHIJKL123-crud";
        final ServiceTicket ticket = new MockServiceTicket(id, org.apereo.cas.services.TestUtils.getService(),
                new MockTicketGrantingTicket("test"));
        registry.addTicket(ticket);
        final ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(id);
        assertNotNull(ticketFromRegistry);
        assertEquals(id, ticketFromRegistry.getId());
        registry.deleteTicket(id);
        assertNull(registry.getTicket(id));
    }

    @Test
    public void verifyExpiration() throws Exception {
        final String id = "ST-1234567890ABCDEFGHIJKL-exp1";
        final MockServiceTicket ticket = new MockServiceTicket(id, org.apereo.cas.services.TestUtils.getService(), new MockTicketGrantingTicket("test"));
        ticket.setExpiration(new AlwaysExpiresExpirationPolicy());
        registry.addTicket(ticket);
        Thread.sleep(1500);
        assertNull(registry.getTicket(id, ServiceTicket.class));
    }

    @Test
    public void verifyDeleteTicketWithChildren() throws Exception {
        this.registry.addTicket(new TicketGrantingTicketImpl(
                "TGT", TestUtils.getAuthentication(), new NeverExpiresExpirationPolicy()));
        final TicketGrantingTicket tgt = this.registry.getTicket(
                "TGT", TicketGrantingTicket.class);

        final Service service = org.apereo.cas.services.TestUtils.getService("TGT_DELETE_TEST");

        final ServiceTicket st1 = tgt.grantServiceTicket(
                "ST1", service, new NeverExpiresExpirationPolicy(), false, false);
        final ServiceTicket st2 = tgt.grantServiceTicket(
                "ST2", service, new NeverExpiresExpirationPolicy(), false, false);
        final ServiceTicket st3 = tgt.grantServiceTicket(
                "ST3", service, new NeverExpiresExpirationPolicy(), false, false);

        this.registry.addTicket(st1);
        this.registry.addTicket(st2);
        this.registry.addTicket(st3);
        this.registry.updateTicket(tgt);

        assertNotNull(this.registry.getTicket("TGT", TicketGrantingTicket.class));
        assertNotNull(this.registry.getTicket("ST1", ServiceTicket.class));
        assertNotNull(this.registry.getTicket("ST2", ServiceTicket.class));
        assertNotNull(this.registry.getTicket("ST3", ServiceTicket.class));

        this.registry.deleteTicket(tgt.getId());

        assertNull(this.registry.getTicket("TGT", TicketGrantingTicket.class));
        assertNull(this.registry.getTicket("ST1", ServiceTicket.class));
        assertNull(this.registry.getTicket("ST2", ServiceTicket.class));
        assertNull(this.registry.getTicket("ST3", ServiceTicket.class));
    }

    @Test
    public void verifyDeleteTicketWithPGT() {
        final Authentication a = TestUtils.getAuthentication();
        this.registry.addTicket(new TicketGrantingTicketImpl("TGT", a, new NeverExpiresExpirationPolicy()));
        final TicketGrantingTicket tgt = this.registry.getTicket("TGT", TicketGrantingTicket.class);

        final Service service = org.apereo.cas.services.TestUtils.getService("TGT_DELETE_TEST");

        final ServiceTicket st1 = tgt.grantServiceTicket("ST1", service, new NeverExpiresExpirationPolicy(), false, true);
        this.registry.addTicket(st1);
        this.registry.updateTicket(tgt);

        assertNotNull(this.registry.getTicket("TGT", TicketGrantingTicket.class));
        assertNotNull(this.registry.getTicket("ST1", ServiceTicket.class));

        final ProxyGrantingTicket pgt = st1.grantProxyGrantingTicket("PGT-1", a, new NeverExpiresExpirationPolicy());
        this.registry.addTicket(pgt);
        this.registry.updateTicket(tgt);
        this.registry.updateTicket(st1);
        assertEquals(pgt.getGrantingTicket(), tgt);
        assertNotNull(this.registry.getTicket("PGT-1", ProxyGrantingTicket.class));
        assertEquals(a, pgt.getAuthentication());
        assertNotNull(this.registry.getTicket("ST1", ServiceTicket.class));

        assertTrue(this.registry.deleteTicket(tgt.getId()) > 0);

        assertNull(this.registry.getTicket("TGT", TicketGrantingTicket.class));
        assertNull(this.registry.getTicket("ST1", ServiceTicket.class));
        assertNull(this.registry.getTicket("PGT-1", ProxyGrantingTicket.class));
    }
}
