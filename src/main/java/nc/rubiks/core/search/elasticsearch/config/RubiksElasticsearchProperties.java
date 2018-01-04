package nc.rubiks.core.search.elasticsearch.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicoraynaud on 03/05/2017.
 */
@Component("rubiksElasticsearchProperties")
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "rubiks.elasticsearch")
public class RubiksElasticsearchProperties {

    private String clusterNodes = StringUtils.EMPTY;
    private Boolean testMode = false;
    private String context = StringUtils.EMPTY;
    private String username = StringUtils.EMPTY;
    private String password = StringUtils.EMPTY;
    private String scanBasePackage = StringUtils.EMPTY;
    private String indexedObjects = StringUtils.EMPTY;

    public List<Pair<String, Integer>> getClusterNodes() {

        if (StringUtils.isEmpty(clusterNodes)) {
            return new ArrayList<>();
        }

        try {
            List<Pair<String, Integer>> result = new ArrayList<>();

            for (String url : clusterNodes.split(",")) {
                int index = url.lastIndexOf(':');
                if (index == -1) {
                    throw new RubiksElasticsearchConfigurationException("An error occured while reading the configuration for rubiks-elasticsearch cluster-nodes. They should be a comma separated list of urls:port (ex: https://myserver.com:8083,http://mysecondserver.nc:80");
                }
                Integer port = Integer.parseInt(url.substring(index + 1, url.length()));
                result.add(Pair.of(url.substring(0, index), port));
            }

            return result;
        } catch (Exception ex) {
            throw new RubiksElasticsearchConfigurationException("An error occured while reading the configuration for rubiks-elasticsearch cluster-nodes. They should be a comma separated list of urls:port (ex: https://myserver.com:8083,http://mysecondserver.nc:80", ex);
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

    public String getScanBasePackage() {
        return scanBasePackage;
    }

    public void setScanBasePackage(String scanBasePackage) {
        this.scanBasePackage = scanBasePackage;
    }

    public List<Class> getIndexedObjects() {

        List<Class> classes = new ArrayList<>();

        if (StringUtils.isBlank(indexedObjects)) {
            return classes;
        }

        try {
            for (String clazzString : indexedObjects.split(",")) {
                classes.add(Class.forName(clazzString));
            }

        } catch (Exception ex) {
            throw new RubiksElasticsearchConfigurationException("An error occured while scanning configuration for indexed classes", ex);
        }

        return classes;
    }

    public void setIndexedObjects(String indexedObjects) {
        this.indexedObjects = indexedObjects;
    }
}
