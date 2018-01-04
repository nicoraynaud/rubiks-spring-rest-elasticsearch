package nc.rubiks.core.search.elasticsearch.repository;

import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for querying ElasticsearchSyncAction in order to process them
 *
 * @author nicoraynaud
 */
public interface ElasticsearchSyncActionRepository extends JpaRepository<ElasticsearchSyncAction, UUID> {

    @Query("select esa from ElasticsearchSyncAction esa where nbTryouts < :nbRetry order by esa.createdDate asc")
    List<ElasticsearchSyncAction> findAllOrderByCreatedDateAsc(@Param("nbRetry") int nbRetry);
}
