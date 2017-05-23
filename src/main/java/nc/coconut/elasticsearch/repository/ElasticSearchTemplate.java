package nc.coconut.elasticsearch.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ElasticSearchTemplate is responsible for querying an ES cluster through a given RestClient
 * It contains all root operations needed to administer an indice with a type
 *
 * It is initialiazed with a "debug" mode in order to create specific indice names on the server
 * whenever the debug mode is true. This will ensure that no test can be ran against the same indices
 * as a running application.
 */
@Component
public class ElasticSearchTemplate {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchTemplate.class);

    private final boolean debug;
    private final String context;
    private final String prefix;

    /**
     * Constructor
     * @param context the context the elasticsearch cluster is available at (i.e.: http://es.intranet.world/my-context:9200 => "my-context").
     *                this parameter can be null or empty if no context is used.
     * @param debug the debug mode for this instance.
     *              When set to yes, all indices will be automatically prefixed when used (ex: "test_d4a2e0d7-e968-4ca3-8c11-21d2d1b54e2c_{indice}")
     *              When set to no, no prefix will be used and indices will be as passed in parameter for each method.
     */
    public ElasticSearchTemplate(String context, Boolean debug) {
        this.context = context;
        this.debug = debug;
        if (this.debug) {
            prefix = "test_" + UUID.randomUUID() + "_";
        } else {
            prefix = "";
        }
    }

    /**
     * Computes the rootIndiceName based on the current configuration
     * ex : for "twitter", it will return "/{context}?/prefix_twitter"
     * @param rootIndiceName the indice name as used by the client
     * @return the actual indice name relative url
     */
    public String getRootIndiceName(String rootIndiceName) {
        return '/' + (StringUtils.isNotBlank(context) ?  context + '/' : "") + prefix + rootIndiceName;
    }

    /**
     * Queries ES to check whether a specific indice exists with the given typeName
     * @param client The ES RestClient
     * @param rootIndiceName the Indice
     * @param typeName the type inside the Indice
     * @return true if exists, false otherwise
     */
    public boolean indiceExists(RestClient client, String rootIndiceName, String typeName) {
        try {
            Response existsResponse = client.performRequest(
                "HEAD",
                getRootIndiceName(rootIndiceName),
                Collections.emptyMap());

            log.debug("Verify existence of indice {}/{} resulted with status [{}]", getRootIndiceName(rootIndiceName), typeName, existsResponse.getStatusLine().getStatusCode());

            return existsResponse.getStatusLine().getStatusCode() != 404;
        } catch (Exception ex) {
            log.error("Error occured during query of indice existence {} : {}", typeName, ex);
        }

        return false;
    }

    /**
     * Reads and return the JSON of an Indice
     * @param client the ES RestClient
     * @param rootIndiceName the Indice to read
     * @return the JSON of the Indice configuration
     */
    public String readIndice(RestClient client, String rootIndiceName) {
        Response contentResponse = null;
        try {
            contentResponse = client.performRequest(
                "GET",
                getRootIndiceName(rootIndiceName),
                Collections.emptyMap());

            log.trace("Verify content of indice {} resulted with content [{}]", getRootIndiceName(rootIndiceName), contentResponse);
            log.debug("Verify content of indice {} resulted with status [{}]", getRootIndiceName(rootIndiceName), contentResponse.getStatusLine().getStatusCode());

            return EntityUtils.toString(contentResponse.getEntity());
        } catch (Exception ex) {
            log.error("Error occured during query of indice read {} : {}", getRootIndiceName(rootIndiceName), ex);
        } finally {
            if (contentResponse != null) EntityUtils.consumeQuietly(contentResponse.getEntity());
        }

        return null;
    }

    /**
     * Deletes an existing indice from ES cluster
     * @param client The ES RestClient
     * @param rootIndiceName The Indice to delete
     */
    public void deleteIndice(RestClient client, String rootIndiceName) {
        Response deleteResponse = null;
        try {
            deleteResponse = client.performRequest(
                "DELETE",
                getRootIndiceName(rootIndiceName),
                Collections.emptyMap());

            log.debug("Delete indice {} resulted with status [{}]", getRootIndiceName(rootIndiceName), deleteResponse.getStatusLine().getStatusCode());

        }
        catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == 404) {
                return;
            }
            log.error("Error occured during delete of indice {} : {}", getRootIndiceName(rootIndiceName), ex);
        }
        catch (Exception ex) {
            log.error("Error occured during delete of index {} : {}", getRootIndiceName(rootIndiceName), ex);
        }
    }

    /**
     * Deletes all indices from the ES cluster
     * Careful : This will delete all indices and their stored data from the cluster : there is no turning back !
     * @param client the ES RestClient
     */
    public void deleteAllIndices(RestClient client) {
        Response deleteResponse = null;
        try {
            deleteResponse = client.performRequest(
                "DELETE",
                '/' + prefix + '*',
                Collections.emptyMap());

            log.debug("Delete all indices {} resulted with status [{}]", '/' + prefix + '*', deleteResponse.getStatusLine().getStatusCode());

        }
        catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == 404) {
                return;
            }
            log.error("Error occured during delete of all indices : {}", ex);
        }
        catch (Exception ex) {
            log.error("Error occured during delete of all indices : {}", ex);
        }
    }

    /**
     * Creates an Indice on the ES cluster with a given configuration
     * @param client the ES RestClient
     * @param rootIndiceName the indice name
     * @param typeName the type name inside the indice
     * @param setting the settings in JSON ES compliant format
     */
    public void createIndice(RestClient client, String rootIndiceName, String typeName, String setting) {
        try {
            Response createResponse = client.performRequest(
                "PUT",
                getRootIndiceName(rootIndiceName),
                Collections.emptyMap(),
                new NStringEntity(setting, ContentType.APPLICATION_JSON));

            log.trace("Creationg of indice {}/{} resulted with content : {}", getRootIndiceName(rootIndiceName), typeName, createResponse);
            log.debug("Creationg of indice {}/{} resulted with status [{}]", getRootIndiceName(rootIndiceName), typeName, createResponse.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            log.error("Error occured during creation of indice {}/{} : {}", getRootIndiceName(rootIndiceName), typeName, ex);
        }
    }

    /**
     * Adds mapping configuration to an existing Indice on the ES cluster
     * @param client the ES RestClient
     * @param rootIndiceName the indice name
     * @param typeName the type name inside the indice
     * @param mapping the mapping settings in JSON ES compliant format
     */
    public void putMapping(RestClient client, String rootIndiceName, String typeName, String mapping) {
        try {
            Response putResponse = client.performRequest(
                "PUT",
                getRootIndiceName(rootIndiceName) + "/_mapping/" + typeName,
                Collections.emptyMap(),
                new NStringEntity(mapping, ContentType.APPLICATION_JSON));

            log.trace("Creation of mapping {}/{} resulted with content : {}", getRootIndiceName(rootIndiceName), typeName, putResponse);
            log.debug("Creation of mapping {}/{} resulted with status [{}]", getRootIndiceName(rootIndiceName), typeName, putResponse.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            log.error("Error occured during creation of mapping {}/{} : {}", getRootIndiceName(rootIndiceName), typeName, ex);
        }
    }

    /**
     * Indexes a document in a given indice and type
     * @param client The ES RestClient
     * @param rootIndiceName the indice name
     * @param typeName the type name
     * @param id the id of the document to index
     * @param entity the actual document to index (in JSON ES compliant format)
     */
    public void index(RestClient client, String rootIndiceName, String typeName, String id, String entity) {
        try {
            Response indiceResponse = client.performRequest(
                "PUT",
                getRootIndiceName(rootIndiceName) + '/' + typeName + '/' + id,
                getParams(),
                new NStringEntity(entity, ContentType.APPLICATION_JSON));

            log.trace("Indexing document {}/{} resulted with content : {}", getRootIndiceName(rootIndiceName), typeName, indiceResponse);
            log.debug("Indexing document {}/{} resulted with status [{}]", getRootIndiceName(rootIndiceName), typeName, indiceResponse.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            log.error("Error occured during indexing of document {} : {}", typeName, ex);
        }
    }

    /**
     * Responsible for creating the default parameters for any query made to ES
     * Ex: in debug mode, each query will have a default refresh mode of "wait_for" in order
     * to make sure that the client will only release the connection when the data has been
     * processed by the cluster and not before. (i.e. indexation is not async anymore)
     * @return The parameter map
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        if (debug) {
            params.put("refresh", "wait_for");
        }
        return params;
    }
}
