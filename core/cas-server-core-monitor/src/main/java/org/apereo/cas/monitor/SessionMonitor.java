package org.apereo.cas.monitor;

import org.apereo.cas.ticket.registry.TicketRegistry;

/**
 * Monitors the status of a {@link TicketRegistry}
 * for exposing internal
 * state information used in status reports.
 *
 * @author Marvin S. Addison
 * @since 3.5.0
 */
public class SessionMonitor implements Monitor<SessionStatus> {

    /** Ticket registry instance that exposes state info. */
    private TicketRegistry registryState;

    /** Threshold above which warnings are issued for session count. */
    private int sessionCountWarnThreshold = -1;

    /** Threshold above which warnings are issued for service ticket count. */
    private int serviceTicketCountWarnThreshold = -1;


    /**
     * Sets the ticket registry that exposes state information that may be queried by this monitor.
     * @param state the ticket registry state instance
     */
    public void setTicketRegistry(final TicketRegistry state) {
        this.registryState = state;
    }


    /**
     * Sets the threshold above which warnings are issued for session counts in excess of value.
     *
     * @param threshold Warn threshold if non-negative value, otherwise warnings are disabled.
     */
    public void setSessionCountWarnThreshold(final int threshold) {
        this.sessionCountWarnThreshold = threshold;
    }


    /**
     * Sets the threshold above which warnings are issued for service ticket counts in excess of value.
     *
     * @param threshold Warn threshold if non-negative value, otherwise warnings are disabled.
     */
    public void setServiceTicketCountWarnThreshold(final int threshold) {
        this.serviceTicketCountWarnThreshold = threshold;
    }



    @Override
    public String getName() {
        return SessionMonitor.class.getSimpleName();
    }

    @Override
    public SessionStatus observe() {
        try {
            final long sessionCount = this.registryState.sessionCount();
            final long ticketCount = this.registryState.serviceTicketCount();

            if (sessionCount == Integer.MIN_VALUE || ticketCount == Integer.MIN_VALUE) {
                return new SessionStatus(StatusCode.UNKNOWN,
                                         String.format("Ticket registry %s reports unknown session and/or ticket counts.",
                                         this.registryState.getClass().getName()),
                                         sessionCount, ticketCount);
            }

            final StringBuilder msg = new StringBuilder();
            StatusCode code = StatusCode.OK;
            if (this.sessionCountWarnThreshold > -1 && sessionCount > this.sessionCountWarnThreshold) {
                code = StatusCode.WARN;
                msg.append(String.format(
                        "Session count (%s) is above threshold %s. ", sessionCount, this.sessionCountWarnThreshold));
            } else {
                msg.append(sessionCount).append(" sessions. ");
            }
            if (this.serviceTicketCountWarnThreshold > -1 && ticketCount > this.serviceTicketCountWarnThreshold) {
                code = StatusCode.WARN;
                msg.append(String.format(
                        "Service ticket count (%s) is above threshold %s.",
                        ticketCount,
                        this.serviceTicketCountWarnThreshold));
            } else {
                msg.append(ticketCount).append(" service tickets.");
            }
            return new SessionStatus(code, msg.toString(), sessionCount, ticketCount);
        } catch (final Exception e) {
            return new SessionStatus(StatusCode.ERROR, e.getMessage());
        }
    }
}
