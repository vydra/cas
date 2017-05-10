package org.apereo.cas.support.saml.web.flow.mdui;

import org.opensaml.saml.metadata.resolver.filter.MetadataFilterChain;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

/**
 * A {@link StaticMetadataResolverAdapter} that loads metadata from static xml files
 * served by urls or locally.
 *
 * @author Misagh Moayyed
 * @since 4.1.0
 */
public class StaticMetadataResolverAdapter extends AbstractMetadataResolverAdapter {
    /**
     * New ctor - required for serialization and job scheduling.
     */
    public StaticMetadataResolverAdapter() {
        super();
    }

    /**
     * Instantiates a new static metadata resolver adapter.
     *
     * @param metadataResources the metadata resources
     */
    public StaticMetadataResolverAdapter(final Map<Resource, MetadataFilterChain> metadataResources) {
        super(metadataResources);
    }

    @Scheduled(initialDelayString="${cas.saml.mdui.startDelay:30000}",
               fixedDelayString = "${cas.saml.mdui.repeatInterval:900000}")
    @Override
    public void buildMetadataResolverAggregate() {
        super.buildMetadataResolverAggregate();
    }
}
