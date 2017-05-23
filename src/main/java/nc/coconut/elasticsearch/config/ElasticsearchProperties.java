package nc.coconut.elasticsearch.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring-Boot properties file
 * Will automatically be populated using coconut.elasticsearch prefix
 * ex: # Elasticsearch configuration
 *     coconut:
 *         elasticsearch:
 *             cluster-nodes: http://localhost:9200
 *             context: my-context
 *             test-mode: false
 *             username: username
 *             password: password
 */
@Component("elasticsearchProperties")
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "coconut.elasticsearch")
public class ElasticsearchProperties {

    private String clusterNodes = "";
    private Boolean testMode = false;
    private String context = "";
    private String username = "";
    private String password = "";

    public List<Pair<String, Integer>> getClusterNodes() {

        if (StringUtils.isEmpty(clusterNodes)) {
            return new ArrayList<>();
        }

        try {
            List<Pair<String, Integer>> result = new ArrayList<>();

            for (String url : clusterNodes.split(",")) {
                int index = url.lastIndexOf(':');
                if (index == -1) {
                    throw new ElasticsearchConfigurationException("An error occured while reading the configuration for elasticsearch cluster-nodes. They should be a comma separated list of urls:port (ex: https://myserver.com:8083,http://mysecondserver.nc:80");
                }
                Integer port = Integer.parseInt(url.substring(index + 1, url.length()));
                result.add(Pair.of(url.substring(0, index), port));
            }

            return result;
        } catch (Exception ex) {
            throw new ElasticsearchConfigurationException("An error occured while reading the configuration for elasticsearch cluster-nodes. They should be a comma separated list of urls:port (ex: https://myserver.com:8083,http://mysecondserver.nc:80", ex);
        }
    }

    public void setClusterNodes(String clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public Boolean getTestMode() {
        return testMode;
    }

    public void setTestMode(Boolean testMode) {
        this.testMode = testMode;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
