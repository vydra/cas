package org.apereo.cas.logging;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apereo.cas.util.serialization.TicketIdSanitizationUtils;
import org.springframework.util.Assert;

/**
 * This is {@link CasAppender}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Plugin(name="CasAppender", category="Core", elementType="appender", printObject=true)
public class CasAppender extends AbstractAppender {
    private static final long serialVersionUID = 3744758323628847477L;
    
    private Configuration config;
    private AppenderRef appenderRef;
    
    /**
     * Instantiates a new Cas appender.
     *
     * @param name        the name
     * @param config      the config
     * @param appenderRef the appender ref
     */
    public CasAppender(final String name, final Configuration config, final AppenderRef appenderRef) {
        super(name, null, PatternLayout.createDefaultLayout());
        Assert.notNull(config, "Log configuration cannot be null");
        Assert.notNull(config, "Appender reference configuration cannot be null");
        
        this.config = config;
        this.appenderRef = appenderRef;
    }
    
    @Override
    public void append(final LogEvent logEvent) {
        final String messageModified = TicketIdSanitizationUtils.sanitize(logEvent.getMessage().getFormattedMessage());
        final Message message = new SimpleMessage(messageModified);
        final LogEvent newLogEvent = Log4jLogEvent.newBuilder()
                .setLevel(logEvent.getLevel())
                .setLoggerName(logEvent.getLoggerName())
                .setLoggerFqcn(logEvent.getLoggerFqcn())
                .setContextMap(logEvent.getContextMap())
                .setContextStack(logEvent.getContextStack())
                .setEndOfBatch(logEvent.isEndOfBatch())
                .setIncludeLocation(logEvent.isIncludeLocation())
                .setMarker(logEvent.getMarker())
                .setMessage(message)
                .setNanoTime(logEvent.getNanoTime())
                .setSource(logEvent.getSource())
                .setThreadName(logEvent.getThreadName())
                .setThrownProxy(logEvent.getThrownProxy())
                .setThrown(logEvent.getThrown())
                .setTimeMillis(logEvent.getTimeMillis()).build();
        
        final String refName = this.appenderRef.getRef();
        if (StringUtils.isNotBlank(refName)) {
            final Appender appender = this.config.getAppender(refName);
            if (appender != null) {
                appender.append(newLogEvent);
            } else {
                throw new IllegalArgumentException("No log appender could be found for " + refName);
            }
        } else {
            throw new IllegalArgumentException("No log appender reference could be located in your logging configuration.");
        }
    }

    /**
     * Create appender cas appender.
     *
     * @param name        the name
     * @param appenderRef the appender ref
     * @param config      the config
     * @return the cas appender
     */
    @PluginFactory
    public static CasAppender build(@PluginAttribute("name") final String name, 
                                    @PluginElement("AppenderRef") final AppenderRef appenderRef,
                                    @PluginConfiguration final Configuration config) {
        return new CasAppender(name, config, appenderRef);
    }
}
