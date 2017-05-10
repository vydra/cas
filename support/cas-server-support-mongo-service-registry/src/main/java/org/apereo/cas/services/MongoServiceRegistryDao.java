package org.apereo.cas.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * <p>Implementation of {@code ServiceRegistryDao} that uses a MongoDb repository as the backend
 * persistence mechanism. The repository is configured by the Spring application context. </p>
 * <p>The class will automatically create a default collection to use with services. The name
 * of the collection may be specified through {@link #setCollectionName(String)}.
 * It also presents the ability to drop an existing collection and start afresh
 * through the use of {@link #setDropCollection(boolean)}.</p>
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
public class MongoServiceRegistryDao implements ServiceRegistryDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoServiceRegistryDao.class);

    private String collectionName;

    private boolean dropCollection;

    private MongoOperations mongoTemplate;

    /**
     * Ctor.
     *
     * @param mongoTemplate  mongoTemplate
     * @param collectionName collectionName
     * @param dropCollection dropCollection
     */
    public MongoServiceRegistryDao(final MongoOperations mongoTemplate, final String collectionName, final boolean dropCollection) {
        this.mongoTemplate = mongoTemplate;
        this.collectionName = collectionName;
        this.dropCollection = dropCollection;
    }

    public MongoServiceRegistryDao() {
    }

    /**
     * Initialized registry post construction.
     * Will decide if the configured collection should
     * be dropped and recreated.
     */
    @PostConstruct
    public void init() {
        Assert.notNull(this.mongoTemplate);

        if (this.dropCollection) {
            LOGGER.debug("Dropping database collection: {}", this.collectionName);
            this.mongoTemplate.dropCollection(this.collectionName);
        }

        if (!this.mongoTemplate.collectionExists(this.collectionName)) {
            LOGGER.debug("Creating database collection: {}", this.collectionName);
            this.mongoTemplate.createCollection(this.collectionName);
        }


    }

    @Override
    public boolean delete(final RegisteredService svc) {
        if (this.findServiceById(svc.getId()) != null) {
            this.mongoTemplate.remove(svc, this.collectionName);
            LOGGER.debug("Removed registered service: {}", svc);
            return true;
        }
        return false;
    }

    @Override
    public RegisteredService findServiceById(final long svcId) {
        return this.mongoTemplate.findOne(new Query(Criteria.where("id").is(svcId)),
                RegisteredService.class, this.collectionName);
    }

    @Override
    public List<RegisteredService> load() {
        return this.mongoTemplate.findAll(RegisteredService.class, this.collectionName);
    }

    @Override
    public RegisteredService save(final RegisteredService svc) {
        if (svc.getId() == AbstractRegisteredService.INITIAL_IDENTIFIER_VALUE) {
            ((AbstractRegisteredService) svc).setId(svc.hashCode());
        }
        this.mongoTemplate.save(svc, this.collectionName);
        LOGGER.debug("Saved registered service: {}", svc);
        return this.findServiceById(svc.getId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public long size() {
        return this.mongoTemplate.count(new Query(), RegisteredService.class, this.collectionName);
    }

    public void setCollectionName(final String name) {
        this.collectionName = name;
    }
    
    public void setDropCollection(final boolean dropCollection) {
        this.dropCollection = dropCollection;
    }

    public void setMongoTemplate(final MongoOperations mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
}
