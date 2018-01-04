package nc.rubiks.core.search.elasticsearch.repository.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.support.WriteRequest;
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
 * Created by nicoraynaud on 11/05/2017.
 *
 * ElasticSearchTemplate is responsible for querying an ES cluster through a given RestClient
 * It contains all root operations needed to administer an index with a type
 *
 * It is initialiazed with a "testMode" mode in order to create specific index names on the server
 * whenever the testMode mode is true. This will ensure that no test can be ran against the same indices
 * as a running application.
 */
@Component
public class ElasticSearchTemplate {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchTemplate.class);

    private final boolean testMode;
    private final String context;
    private final String prefix;

    /**
     * Constructor
     * @param context the context the elasticsearch cluster is available at (i.e.: http://es.intranet.opt/my-context:9200 =&gt; "my-context").
     *                this parameter can be null or empty if no context is used.
     * @param testMode the test mode for this instance.
     *              When set to yes, all indices will be automatically prefixed when used (ex: "test_d4a2e0d7-e968-4ca3-8c11-21d2d1b54e2c_{index}")
     *              When set to no, no prefix will be used and indices will be as passed in parameter for each method.
     */
    public ElasticSearchTemplate(String context, Boolean testMode) {
        this.context = context;
        this.testMode = testMode;
        if (this.testMode) {
            prefix = "test_" + UUID.randomUUID() + "_";
        } else {
            prefix = "";
        }
    }

    /**
     * Computes the indexName based on the current configuration
     * ex : for "twitter", it will return "{context}?/prefix_twitter"
     * @param indexName the index name as used by the client
     * @return the actual index name relative url
     */
    public String getRootIndexName(String indexName) {
        return (StringUtils.isNotBlank(context) ?  (context + '/') : "") + prefix + indexName;
    }

    /**
     * Queries ES to check whether a specific index exists with the given typeName
     * @param client The ES RestClient
     * @param indexName the Index
     * @param typeName the type inside the Index
     * @return true if exists, false otherwise
     */
    public boolean indexExists(RestClient client, String indexName, String typeName) {
        try {
            Response existsResponse = client.performRequest(
                HttpHead.METHOD_NAME,
                '/' + getRootIndexName(indexName),
                Collections.emptyMap());

            log.debug("Verify existence of index {}/{} resulted with status [{}]", getRootIndexName(indexName), typeName, existsResponse.getStatusLine().getStatusCode());

            return existsResponse.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
        } catch (Exception ex) {
            log.error("Error occured during query of index existence {} : {}", typeName, ex);
        }

        return false;
    }

    /**
     * Reads and return the JSON of an Index
     * @param client the ES RestClient
     * @param indexName the Index to read
     * @return the JSON of the Index configuration
     */
    public String readIndex(RestClient client, String indexName) {
        Response contentResponse = null;
        try {
            contentResponse = client.performRequest(
                HttpGet.METHOD_NAME,
                '/' + getRootIndexName(indexName),
                Collections.emptyMap());

            log.trace("Verify content of index {} resulted with content [{}]", getRootIndexName(indexName), contentResponse);
            log.debug("Verify content of index {} resulted with status [{}]", getRootIndexName(indexName), contentResponse.getStatusLine().getStatusCode());

            return EntityUtils.toString(contentResponse.getEntity());
        } catch (Exception ex) {
            log.error("Error occured during query of index read {} : {}", getRootIndexName(indexName), ex);
        } finally {
            if (contentResponse != null) EntityUtils.consumeQuietly(contentResponse.getEntity());
        }

        return null;
    }

    /**
     * Deletes an existing index from ES cluster
     * @param client The ES RestClient
     * @param indexName The Index to delete
     */
    public void deleteIndex(RestClient client, String indexName) {
        Response deleteResponse = null;
        try {
            deleteResponse = client.performRequest(
                HttpDelete.METHOD_NAME,
                '/' + getRootIndexName(indexName),
                Collections.emptyMap());

            log.debug("Delete index {} resulted with status [{}]", getRootIndexName(indexName), deleteResponse.getStatusLine().getStatusCode());

        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return;
            }
            log.error("Error occured during delete of index {} : {}", getRootIndexName(indexName), ex);
        } catch (Exception ex) {
            log.error("Error occured during delete of index {} : {}", getRootIndexName(indexName), ex);
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
                HttpDelete.METHOD_NAME,
                '/' + prefix + '*',
                Collections.emptyMap());

            log.debug("Delete all indices /{}* resulted with status [{}]", prefix, deleteResponse.getStatusLine().getStatusCode());

        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return;
            }
            log.error("Error occured during delete of all indices : {}", ex);
        } catch (Exception ex) {
            log.error("Error occured during delete of all indices : {}", ex);
        }
    }

    /**
     * Creates an Index on the ES cluster with a given configuration
     * @param client the ES RestClient
     * @param indexName the index name
     * @param typeName the type name inside the index
     * @param setting the settings in JSON ES compliant format
     */
    public void createIndex(RestClient client, String indexName, String typeName, String setting) {
        try {
            Response createResponse = client.performRequest(
                HttpPut.METHOD_NAME,
                '/' + getRootIndexName(indexName),
                Collections.emptyMap(),
                new NStringEntity(setting, ContentType.APPLICATION_JSON));

            log.trace("Creationg of index {}/{} resulted with content : {}", getRootIndexName(indexName), typeName, createResponse);
            log.debug("Creationg of index {}/{} resulted with status [{}]", getRootIndexName(indexName), typeName, createResponse.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            log.error("Error occured during creation of index {}/{} : {}", getRootIndexName(indexName), typeName, ex);
        }
    }

    /**
     * Adds mapping configuration to an existing Index on the ES cluster
     * @param client the ES RestClient
     * @param indexName the index name
     * @param typeName the type name inside the index
     * @param mapping the mapping settings in JSON ES compliant format
     */
    public void putMapping(RestClient client, String indexName, String typeName, String mapping) {
        try {
            Response putResponse = client.performRequest(
                HttpPut.METHOD_NAME,
                '/' + getRootIndexName(indexName) + "/_mapping/" + typeName,
                Collections.emptyMap(),
                new NStringEntity(mapping, ContentType.APPLICATION_JSON));

            log.trace("Creation of mapping {}/{} resulted with content : {}", getRootIndexName(indexName), typeName, putResponse);
            log.debug("Creation of mapping {}/{} resulted with status [{}]", getRootIndexName(indexName), typeName, putResponse.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            log.error("Error occured during creation of mapping {}/{} : {}", getRootIndexName(indexName), typeName, ex);
        }
    }

    /**
     * Responsible for creating the default parameters for any query made to ES
     * Ex: in testMode mode, each query will have a default refresh mode at "true" in order
     * to make sure that the client will only release the connection when the data has been
     * processed by the cluster and not before. (i.e. indexation is not async anymore)
     * @return The parameter map
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        if (testMode) {
            params.put("refresh", "true");
        }
        return params;
    }

    /**
     * Based on the value of the testMode flag, sets the refresh policy to be immediate or default
     * Ex: in testMode mode, each query will have a default refresh mode of "immediate" in order
     * to make sure that the client will only release the connection when the data has been
     * processed by the cluster and not before. (i.e. indexation is not async anymore)
     * @return The refresh policy
     */
    protected WriteRequest.RefreshPolicy getRefreshPolicy() {
        if (testMode) {
            return WriteRequest.RefreshPolicy.IMMEDIATE;
        } else {
            return WriteRequest.RefreshPolicy.NONE;
        }
    }
}
