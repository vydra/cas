package org.apereo.cas.ticket.registry;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.TestUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.mock.MockServiceTicket;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.proxy.ProxyGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.support.NeverExpiresExpirationPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HazelcastTicketRegistry}.
 *
 * @author Dmitriy Kopylenko
 * @since 4.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"classpath:HazelcastTicketRegistryTests-context.xml"})
@SpringBootTest(classes = {RefreshAutoConfiguration.class})
public class HazelcastTicketRegistryTests {
    @Autowired
    @Qualifier("hzTicketRegistry1")
    private TicketRegistry hzTicketRegistry1;

    @Autowired
    @Qualifier("hzTicketRegistry2")
    private TicketRegistry hzTicketRegistry2;
    
    public void setHzTicketRegistry1(final HazelcastTicketRegistry hzTicketRegistry1) {
        this.hzTicketRegistry1 = hzTicketRegistry1;
    }

    public void setHzTicketRegistry2(final HazelcastTicketRegistry hzTicketRegistry2) {
        this.hzTicketRegistry2 = hzTicketRegistry2;
    }

    @Test
    public void retrieveCollectionOfTickets() {
        Collection<Ticket> col = this.hzTicketRegistry1.getTickets();
        for (final Ticket ticket : col) {
            this.hzTicketRegistry1.deleteTicket(ticket.getId());
        }

        col = hzTicketRegistry2.getTickets();
        assertEquals(0, col.size());

        final TicketGrantingTicket tgt = newTestTgt();
        this.hzTicketRegistry1.addTicket(tgt);

        this.hzTicketRegistry1.addTicket(newTestSt(tgt));

        col = hzTicketRegistry2.getTickets();
        assertEquals(2, col.size());
        assertEquals(1, hzTicketRegistry2.serviceTicketCount());
        assertEquals(1, hzTicketRegistry2.sessionCount());
    }

    @Test
    public void basicOperationsAndClustering() throws Exception {
        final TicketGrantingTicket tgt = newTestTgt();
        this.hzTicketRegistry1.addTicket(tgt);

        assertNotNull(this.hzTicketRegistry1.getTicket(tgt.getId()));
        assertNotNull(this.hzTicketRegistry2.getTicket(tgt.getId()));
        assertEquals(1, this.hzTicketRegistry2.deleteTicket(tgt.getId()));
        assertEquals(0, this.hzTicketRegistry1.deleteTicket(tgt.getId()));
        assertNull(this.hzTicketRegistry1.getTicket(tgt.getId()));
        assertNull(this.hzTicketRegistry2.getTicket(tgt.getId()));

        final ServiceTicket st = newTestSt(tgt);
        this.hzTicketRegistry2.addTicket(st);

        assertNotNull(this.hzTicketRegistry1.getTicket("ST-TEST"));
        assertNotNull(this.hzTicketRegistry2.getTicket("ST-TEST"));
        assertEquals(1, this.hzTicketRegistry1.deleteTicket("ST-TEST"));
        assertNull(this.hzTicketRegistry1.getTicket("ST-TEST"));
        assertNull(this.hzTicketRegistry2.getTicket("ST-TEST"));
    }

    @Test
    public void verifyDeleteTicketWithChildren() throws Exception {
        this.hzTicketRegistry1.addTicket(new TicketGrantingTicketImpl(
                "TGT", TestUtils.getAuthentication(), new NeverExpiresExpirationPolicy()));
        final TicketGrantingTicket tgt = this.hzTicketRegistry1.getTicket(
                "TGT", TicketGrantingTicket.class);

        final Service service = org.apereo.cas.services.TestUtils.getService("TGT_DELETE_TEST");

        final ServiceTicket st1 = tgt.grantServiceTicket(
                "ST1", service, new NeverExpiresExpirationPolicy(), false, false);
        final ServiceTicket st2 = tgt.grantServiceTicket(
                "ST2", service, new NeverExpiresExpirationPolicy(), false, false);
        final ServiceTicket st3 = tgt.grantServiceTicket(
                "ST3", service, new NeverExpiresExpirationPolicy(), false, false);

        this.hzTicketRegistry1.addTicket(st1);
        this.hzTicketRegistry1.addTicket(st2);
        this.hzTicketRegistry1.addTicket(st3);
        this.hzTicketRegistry1.updateTicket(tgt);

        assertNotNull(this.hzTicketRegistry1.getTicket(tgt.getId(), TicketGrantingTicket.class));
        assertNotNull(this.hzTicketRegistry1.getTicket("ST1", ServiceTicket.class));
        assertNotNull(this.hzTicketRegistry1.getTicket("ST2", ServiceTicket.class));
        assertNotNull(this.hzTicketRegistry1.getTicket("ST3", ServiceTicket.class));

        assertTrue("TGT and children were deleted", this.hzTicketRegistry1.deleteTicket(tgt.getId()) > 0);

        assertNull(this.hzTicketRegistry1.getTicket(tgt.getId(), TicketGrantingTicket.class));
        assertNull(this.hzTicketRegistry1.getTicket("ST1", ServiceTicket.class));
        assertNull(this.hzTicketRegistry1.getTicket("ST2", ServiceTicket.class));
        assertNull(this.hzTicketRegistry1.getTicket("ST3", ServiceTicket.class));
    }

    @Test
    public void verifyDeleteTicketWithPGT() {
        final Authentication a = TestUtils.getAuthentication();
        this.hzTicketRegistry1.addTicket(new TicketGrantingTicketImpl(
                "TGT", a, new NeverExpiresExpirationPolicy()));
        final TicketGrantingTicket tgt = this.hzTicketRegistry1.getTicket(
                "TGT", TicketGrantingTicket.class);

        final Service service = org.apereo.cas.services.TestUtils.getService("TGT_DELETE_TEST");

        final ServiceTicket st1 = tgt.grantServiceTicket(
                "ST1", service, new NeverExpiresExpirationPolicy(), false, true);

        this.hzTicketRegistry1.addTicket(st1);

        assertNotNull(this.hzTicketRegistry1.getTicket("TGT", TicketGrantingTicket.class));
        assertNotNull(this.hzTicketRegistry1.getTicket("ST1", ServiceTicket.class));

        final ProxyGrantingTicket pgt = st1.grantProxyGrantingTicket("PGT-1", a, new NeverExpiresExpirationPolicy());
        assertEquals(a, pgt.getAuthentication());

        this.hzTicketRegistry1.addTicket(pgt);
        this.hzTicketRegistry1.updateTicket(tgt);
        assertSame(3, this.hzTicketRegistry1.deleteTicket(tgt.getId()));

        assertNull(this.hzTicketRegistry1.getTicket("TGT", TicketGrantingTicket.class));
        assertNull(this.hzTicketRegistry1.getTicket("ST1", ServiceTicket.class));
        assertNull(this.hzTicketRegistry1.getTicket("PGT-1", ProxyGrantingTicket.class));
    }

    private static TicketGrantingTicket newTestTgt() {
        return new MockTicketGrantingTicket("casuser");
    }

    private static ServiceTicket newTestSt(final TicketGrantingTicket tgt) {
        return new MockServiceTicket("ST-TEST", org.apereo.cas.services.TestUtils.getService(), tgt);
    }
}
