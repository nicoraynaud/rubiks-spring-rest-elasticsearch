package nc.rubiks.core.search.elasticsearch.util;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.AsyncResult;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * Util class responsible for reindexing an entire repository of items
 * It relies on an ElasticsearchRepository and a JpaRepository.
 */
public class ElasticsearchReindexUtil {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchReindexUtil.class);

    private static final int REINDEX_PAGE_SIZE = 50;
    private static final int REINDEX_LOG_INTERVAL = 1000;

    private ElasticsearchReindexUtil() {
    }

    public static <T, ID extends Serializable> Future<Boolean> reIndex(ElasticsearchRepository<T, ID> searchRepository, JpaRepository<T, ID> repository) {
        log.debug("Reindexing all entities...");
        searchRepository.deleteAll();

        Long nb = repository.count();
        log.info("Reindexing {} entities...", nb);
        long itemNb = 1L;

        int pageNumber = 0;
        Page<T> entityPage;

        do {
            PageRequest pageRequest = new PageRequest(pageNumber, REINDEX_PAGE_SIZE);
            entityPage = repository.findAll(pageRequest);

            for (T entity : entityPage) {
                searchRepository.save(entity);
                if (++itemNb % REINDEX_LOG_INTERVAL == 0) {
                    log.info("Reindexed entity {}/{}", itemNb, nb);
                }
            }
            pageNumber++;

        } while (pageNumber < entityPage.getTotalPages());

        log.debug("Done reindexing.");

        return new AsyncResult<>(true);
    }
}
