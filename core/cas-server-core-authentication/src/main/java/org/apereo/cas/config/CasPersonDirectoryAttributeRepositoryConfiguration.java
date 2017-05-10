package org.apereo.cas.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.support.BaseGroovyScriptDaoImpl;
import org.apereo.services.persondir.support.CachingPersonAttributeDaoImpl;
import org.apereo.services.persondir.support.GroovyPersonAttributeDao;
import org.apereo.services.persondir.support.JsonBackedComplexStubPersonAttributeDao;
import org.apereo.services.persondir.support.MergingPersonAttributeDaoImpl;
import org.apereo.services.persondir.support.jdbc.AbstractJdbcPersonAttributeDao;
import org.apereo.services.persondir.support.jdbc.MultiRowJdbcPersonAttributeDao;
import org.apereo.services.persondir.support.jdbc.SingleRowJdbcPersonAttributeDao;
import org.apereo.services.persondir.support.ldap.LdaptivePersonAttributeDao;
import org.apereo.services.persondir.support.merger.MultivaluedAttributeMerger;
import org.apereo.services.persondir.support.merger.NoncollidingAttributeAdder;
import org.apereo.services.persondir.support.merger.ReplacingAttributeAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.naming.directory.SearchControls;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This is {@link CasPersonDirectoryAttributeRepositoryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casPersonDirectoryAttributeRepositoryConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CasPersonDirectoryAttributeRepositoryConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasPersonDirectoryAttributeRepositoryConfiguration.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CasConfigurationProperties casProperties;

    @ConditionalOnMissingBean(name = "attributeRepository")
    @Bean
    public IPersonAttributeDao attributeRepository() {
        final List<IPersonAttributeDao> list = new ArrayList<>();

        addLdapAttributeRepository(list);
        addJdbcAttributeRepository(list);
        addJsonAttributeRepository(list);
        addGroovyAttributeRepository(list);
        addStubAttributeRepositoryIfNothingElse(list);
        return composeMergedAndCachedAttributeRepositories(list);
    }

    private void addJsonAttributeRepository(final List<IPersonAttributeDao> list) {
        final Resource r = casProperties.getAuthn().getAttributeRepository().getJson().getConfig().getLocation();
        if (r != null) {
            try {
                final JsonBackedComplexStubPersonAttributeDao dao = new JsonBackedComplexStubPersonAttributeDao(r);
                dao.init();
                LOGGER.debug("Configured JSON attribute sources from [{}]", r);
                list.add(dao);
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void addGroovyAttributeRepository(final List<IPersonAttributeDao> list) {
        final PrincipalAttributesProperties.Groovy groovy = casProperties.getAuthn().getAttributeRepository().getGroovy();
        if (groovy.getConfig().getLocation() != null) {
            final GroovyPersonAttributeDao dao = new GroovyPersonAttributeDao(new GroovyScriptDao(applicationContext, casProperties));
            dao.setCaseInsensitiveUsername(groovy.isCaseInsensitive());
            LOGGER.debug("Configured Groovy attribute sources from [{}]", groovy.getConfig().getLocation());
            list.add(dao);
        }
    }

    private IPersonAttributeDao composeMergedAndCachedAttributeRepositories(final List<IPersonAttributeDao> list) {
        final MergingPersonAttributeDaoImpl mergingDao = new MergingPersonAttributeDaoImpl();

        final String merger = StringUtils.defaultIfBlank(casProperties.getAuthn().getAttributeRepository().getMerger(), "replace".trim());
        LOGGER.debug("Configured merging strategy for attribute sources is [{}]", merger);
        switch (merger.toLowerCase()) {
            case "merge":
                mergingDao.setMerger(new MultivaluedAttributeMerger());
                break;
            case "add":
                mergingDao.setMerger(new NoncollidingAttributeAdder());
                break;
            case "replace":
            default:
                mergingDao.setMerger(new ReplacingAttributeAdder());
                break;
        }

        final CachingPersonAttributeDaoImpl impl = new CachingPersonAttributeDaoImpl();
        impl.setCacheNullResults(false);

        final Cache graphs = CacheBuilder.newBuilder()
                .concurrencyLevel(2)
                .weakKeys()
                .maximumSize(casProperties.getAuthn().getAttributeRepository().getMaximumCacheSize())
                .expireAfterWrite(casProperties.getAuthn().getAttributeRepository().getExpireInMinutes(), TimeUnit.MINUTES)
                .build();
        impl.setUserInfoCache(graphs.asMap());
        mergingDao.setPersonAttributeDaos(list);
        impl.setCachedPersonAttributesDao(mergingDao);

        if (list.isEmpty()) {
            LOGGER.debug("No attribute repository sources are available to merge together.");
        } else {
            LOGGER.debug("Configured attribute repository sources to merge together: ", list);
            LOGGER.debug("Configured cache expiration policy for merging attribute sources to be {} minute(s)",
                    casProperties.getAuthn().getAttributeRepository().getExpireInMinutes());
        }
        return impl;
    }

    private void addStubAttributeRepositoryIfNothingElse(final List<IPersonAttributeDao> list) {
        if (!casProperties.getAuthn().getAttributeRepository().getAttributes().isEmpty() && list.isEmpty()) {

            final boolean foundAttrs = casProperties.getAuthn().getLdap().stream().filter(p ->
                    (p.getPrincipalAttributeList() != null && !p.getPrincipalAttributeList().isEmpty())
                            || (p.getAdditionalAttributes() != null && !p.getAdditionalAttributes().isEmpty())
            ).findAny().isPresent();

            if (foundAttrs) {
                LOGGER.debug("Found attributes which are resolved from authentication sources. Static attributes are ignored");
            } else {
                LOGGER.warn("Found and added static attributes to the attribute repository");
                list.add(Beans.newStubAttributeRepository(casProperties.getAuthn().getAttributeRepository()));
            }
        } else {
            LOGGER.debug("No attributes are defined for attribute repositories, or other attribute repository sources are defined");
        }
    }

    private void addJdbcAttributeRepository(final List<IPersonAttributeDao> list) {
        final PrincipalAttributesProperties attrs = casProperties.getAuthn().getAttributeRepository();
        final PrincipalAttributesProperties.Jdbc jdbc = attrs.getJdbc();
        if (StringUtils.isNotBlank(jdbc.getSql()) && StringUtils.isNotBlank(jdbc.getUrl())) {
            final AbstractJdbcPersonAttributeDao jdbcDao;

            if (jdbc.isSingleRow()) {
                LOGGER.debug("Configured single-row JDBC attribute repository for {}", jdbc.getUrl());
                jdbcDao = new SingleRowJdbcPersonAttributeDao(
                        Beans.newHickariDataSource(jdbc),
                        jdbc.getSql()
                );
            } else {
                LOGGER.debug("Configured multi-row JDBC attribute repository for {}", jdbc.getUrl());
                jdbcDao = new MultiRowJdbcPersonAttributeDao(
                        Beans.newHickariDataSource(jdbc),
                        jdbc.getSql()
                );
                LOGGER.debug("Configured multi-row JDBC column mappings for {} are {}", jdbc.getUrl(), jdbc.getColumnMappings());
                ((MultiRowJdbcPersonAttributeDao) jdbcDao).setNameValueColumnMappings(jdbc.getColumnMappings());
            }

            jdbcDao.setQueryAttributeMapping(ImmutableMap.of("username", jdbc.getUsername()));
            final Map<String, String> mapping = attrs.getAttributes();
            if (mapping != null && !mapping.isEmpty()) {
                LOGGER.debug("Configured result attribute mapping for {} to be {}", jdbc.getUrl(), attrs.getAttributes());
                jdbcDao.setResultAttributeMapping(mapping);
            }
            jdbcDao.setRequireAllQueryAttributes(jdbc.isRequireAllAttributes());
            jdbcDao.setUsernameCaseCanonicalizationMode(jdbc.getCaseCanonicalization());
            jdbcDao.setQueryType(jdbc.getQueryType());
            list.add(jdbcDao);
        }
    }

    private void addLdapAttributeRepository(final List<IPersonAttributeDao> list) {
        final PrincipalAttributesProperties attrs = casProperties.getAuthn().getAttributeRepository();
        final PrincipalAttributesProperties.Ldap ldap = attrs.getLdap();

        if (StringUtils.isNotBlank(ldap.getBaseDn()) && StringUtils.isNotBlank(ldap.getLdapUrl())) {
            final LdaptivePersonAttributeDao ldapDao = new LdaptivePersonAttributeDao();

            LOGGER.debug("Configured LDAP attribute source for {} and baseDn {}", ldap.getLdapUrl(), ldap.getBaseDn());
            ldapDao.setConnectionFactory(Beans.newPooledConnectionFactory(ldap));
            ldapDao.setBaseDN(ldap.getBaseDn());

            LOGGER.debug("LDAP attributes are fetched from {} via filter {}", ldap.getLdapUrl(), ldap.getUserFilter());
            ldapDao.setSearchFilter(ldap.getUserFilter());

            final SearchControls constraints = new SearchControls();
            if (attrs.getAttributes() != null && !attrs.getAttributes().isEmpty()) {
                LOGGER.debug("Configured result attribute mapping for {} to be {}", ldap.getLdapUrl(), attrs.getAttributes());
                ldapDao.setResultAttributeMapping(attrs.getAttributes());
                final String[] attributes = attrs.getAttributes().keySet().toArray(new String[attrs.getAttributes().keySet().size()]);
                constraints.setReturningAttributes(attributes);
            } else {
                LOGGER.debug("Retrieving all attributes as no explicit attribute mappings are defined for {}", ldap.getLdapUrl());
                constraints.setReturningAttributes(null);
            }

            if (ldap.isSubtreeSearch()) {
                LOGGER.debug("Configured subtree searching for {}", ldap.getLdapUrl());
                constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            }
            constraints.setDerefLinkFlag(true);
            ldapDao.setSearchControls(constraints);

            LOGGER.debug("Initializing LDAP attribute source for {}", ldap.getLdapUrl());
            ldapDao.initialize();
            list.add(ldapDao);
        }
    }

    private static class GroovyScriptDao extends BaseGroovyScriptDaoImpl {
        private ApplicationContext applicationContext;
        private CasConfigurationProperties casProperties;

        GroovyScriptDao(final ApplicationContext applicationContext, final CasConfigurationProperties casProperties) {
            this.applicationContext = applicationContext;
            this.casProperties = casProperties;
        }

        @Override
        public Map<String, List<Object>> getPersonAttributesFromMultivaluedAttributes(final Map<String, List<Object>> attributes) {
            if (attributes.containsKey("username")) {
                final List<Object> a = attributes.get("username");
                if (!a.isEmpty()) {
                    final Map<String, List<Object>> results = new HashMap<>();
                    final Map<String, Object> attrs = getAttributesForUser(a.get(0).toString());
                    LOGGER.debug("Groovy-based attributes found are {}", attrs);
                    attrs.forEach((k, v) -> {
                        final List<Object> values = new ArrayList<>();
                        values.addAll(CollectionUtils.convertValueToCollection(v));
                        LOGGER.debug("Adding Groovy-based attribute {} with value(s) {}", k, values);
                        results.put(k, values);
                    });
                    return results;
                }
            }
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> getAttributesForUser(final String uid) {
            final PrincipalAttributesProperties.Groovy groovy = casProperties.getAuthn().getAttributeRepository().getGroovy();

            final ClassLoader parent = getClass().getClassLoader();
            try (GroovyClassLoader loader = new GroovyClassLoader(parent)) {
                if (groovy.getConfig().getLocation() != null) {
                    final File groovyFile = groovy.getConfig().getLocation().getFile();
                    if (groovyFile.exists()) {
                        final Class<?> groovyClass = loader.parseClass(groovyFile);
                        LOGGER.debug("Loaded groovy class {} from script {}", groovyClass.getSimpleName(), groovyFile.getCanonicalPath());
                        final GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
                        LOGGER.debug("Created groovy object instance from class {}", groovyFile.getCanonicalPath());
                        final Object[] args = {uid, LOGGER, casProperties, applicationContext};
                        LOGGER.debug("Executing groovy script's run method, with parameters {}", args);
                        final Map<String, Object> personAttributesMap = (Map<String, Object>) groovyObject.invokeMethod("run", args);
                        LOGGER.debug("Creating person attributes with the username {} and attributes {}", uid, personAttributesMap);
                        return personAttributesMap;
                    }
                }
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            return new HashMap<>();
        }
    }
}

