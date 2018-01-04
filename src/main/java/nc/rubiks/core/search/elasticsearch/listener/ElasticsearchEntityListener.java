package nc.rubiks.core.search.elasticsearch.listener;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This EntityListener is responsible for converting Entity's modification (create/update/delete) into corresponding
 * modifications in their respecting Elasticsearch index.
 *
 * In order to work, it needs to be plugged in using the @EntityListeners annotation on any entity stored in Elasticsearch.
 * <code>
 *  \@Entity
 *  \@EntityListeners(ElasticsearchEntityListener.class)
 *  public class MyClass {}
 * </code>
 *
 * @deprecated This EntityListener should not be used as it cannot guarantee a perfect synchronisation between the database
 * and Elasticsearch. Sometimes, if a transaction rollbacks for an unexpected reason, then the ES documents might have already been
 * updated and therefore not correspond anymore to their value in database.
 *
 * @author nicoraynaud
 */
@Deprecated
public final class ElasticsearchEntityListener {

    private final Logger log = LoggerFactory.getLogger(ElasticsearchEntityListener.class);

    protected static Map<Class, ElasticsearchRepository> elasticsearchRepositoriesMap = new HashMap<>();

    @Autowired
    public void init(List<ElasticsearchRepository> elasticsearchRepositories) {
        log.info("Initializing ElasticsearchEntityListener with repositories [{}]...", elasticsearchRepositories);

        for (ElasticsearchRepository repo : elasticsearchRepositories) {
            elasticsearchRepositoriesMap.put(repo.getIndexedClass(), repo);
        }
    }

    @PostUpdate
    @PostPersist
    public void postPersist(Object entity) {
        log.debug("postPersist/postUpdate - Event for entity class : [{}]", entity.getClass());
        ElasticsearchRepository esRepo = elasticsearchRepositoriesMap.get(entity.getClass());
        if (esRepo != null) {
            esRepo.save(entity);
            log.debug("postPersist/postUpdate - Called elasticsearchRepository.save({})", entity.getClass());
        } else {
            log.debug("postPersist/postUpdate - Could not find an ElasticsearchRepository for this entity");
        }
    }

    @PostRemove
    @SuppressWarnings("unchecked")
    public void postRemove(Object entity) {
        log.debug("postRemove - Event for entity class : [{}]", entity.getClass());
        ElasticsearchRepository esRepo = elasticsearchRepositoriesMap.get(entity.getClass());
        if (esRepo != null) {
            esRepo.delete(entity);
            log.debug("postRemove - Called elasticsearchRepository.delete({})", entity.getClass());
        } else {
            log.debug("postRemove - Could not find an ElasticsearchRepository for this entity");
        }
    }
}
