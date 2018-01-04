# spring-rest-elasticsearch

This library brings support of Elasticsearch v6.0 for Java Spring projects based on Spring Boot (and Spring Data). It was initially built because the spring-data-elasticsearch library does not yet support ES > 5.x and only uses the transport client.

This work is greatly inspired by the spring-data-elasticsearch library even if it does not meet its advanced feature set.

This library is built on top of official [Elasticsearch Java High Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.1/java-rest-high.html). All communications with an ES cluster is made through the REST API and not the Transport protocole (now deprecated by ES).

> This Library is compatible with ES 6.0. Previous and later versions might not work properly : you should test first :).
> Following version will be released to keep up with ES high level client versions.

This library brings the following features :
* Automatic creation of elasticsearch indexes (both mapping and settings) at startup by using local config file embedded in classpath : using [Indices API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html)
* Index, update, delete and search of any entity through the use of AbstractElasticsearchRepository class.
* Search of any indexed entity using ES [Search API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html) language. 
* Optional automatic synchronization between Hibernate persisted entities and ES documents (through the use of Spring Scheduling and Shedlock)

## Development

To build this project, you need to have Gradle installed on your computer.

In order to run the unit tests, you will need to setup a local ES cluster node or connect to a remote one.

Here follows a docker-compose setup compatible with the library :

```yaml
    version: '2'
    services:
        spring-rest-elasticsearch-6.0.0:
            image: docker.elastic.co/elasticsearch/elasticsearch:6.0.0
            ports:
                - 9200:9200
                - 9300:9300
            environment:
                - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
                - "xpack.security.enabled=false"
                - "cluster.name=elasticsearch"
  
```

that you can run using the following command :

    docker-compose -f src/main/docker/elasticsearch.yml -d up

## Build

You can then run `gradle build` to build the library.

## How to use the library

### Dependency

You need to reference this library and set the spring.elasticsearch.version property to the actual version of ES you are using to let Spring know that you are overriding its default ES version.

#### Gradle

```groovy
    dependencies {
        
        // Elasticsearch support
        compile group: 'nc.rubiks', name: 'spring-rest-elasticsearch', version: spring_rest_elasticsearch_version
        
    }
```

#### Maven

```xml
    <dependencies>
        <dependency>
            <groupId>nc.rubiks</groupId>
            <artifactId>spring-rest-elasticsearch</artifactId>
            <version>${spring_rest_elasticsearch_version}</version>
        </dependency>
    </dependencies>
```

### Development

#### Prerequisites

To allow the library to work properly, the following prerequisites are mandatory :
* Entities meant to be indexed need to have a `getId()` method that will return its unique id throughout the index.
* You need to use Spring and especially Spring-Data because the library use their objects to create queries (Page, Pageable, etc...)

#### Spring Configuration

Let's create a Configuration class for setting the lib's document mapper that will convert Java objects into JSON documents.

Here we use Jackson, but you can setup whatever converter you want, by implementing the DocumentMapper Interface properly like follows :

```java
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.util.Map;

@Configuration
public class ElasticsearchConfiguration {

    @Bean
    public DocumentMapper buildDocumentMapper(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        return new CustomDocumentMapper(jackson2ObjectMapperBuilder.createXmlMapper(false).build());
    }

    public class CustomDocumentMapper implements DocumentMapper {

        private ObjectMapper objectMapper;

        public CustomDocumentMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            // Adding Hibernate5Module to ensure full access to lazy loaded properties of entity before saving it in ES
            Hibernate5Module module = new Hibernate5Module().enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
            this.objectMapper.registerModule(module);
        }

        @Override
        public String mapToString(Object object) throws IOException {
            return objectMapper.writeValueAsString(object);
        }

        @Override
        public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
            return objectMapper.readValue(source, clazz);
        }

        @Override
        public <T> T mapToObject(Map source, Class<T> clazz) throws IOException {
            return objectMapper.convertValue(source, clazz);
        }

        @Override
        public JsonNode readTree(String inputSource) throws IOException {
            return objectMapper.readTree(inputSource);
        }
    }
    
    }
```

Now, let's create a configuration section in our application.yml :

```yaml
    rubiks:
        elasticsearch:
            cluster-nodes: http://localhost:9200  # ES cluster 
            context:
            test-mode: false                      # test-mode disabled
            username:                             # no username/password
            password:
            scan-base-package: nc.rubiks          # root package to scan at startup for indexed objects
            indexed-objects:                      # csv list of fullname classes to be synchronized when not annotated
            sync:
                enabled: true                     # entity/document sync enabled
                rate-milliseconds: 1000           # sync refresh rate
                nb-tryouts: 1                     # nb of tryouts before giving up when synchronizing entities (1 is enough for development purposes)
```

The above configuration explained :
* **cluster-nodes** : a comma separated list of cluster nodes to connect to. They need to be specified with the scheme (http, https) and the port.
* **context** : the potential context where the api of the ES cluster is available at : i.e. : https://mydomain/elasticsearch -> 'elasticsearch' is the context
* **test-mode** : this is to be used in testing configuration : this enables a `wait_for` parameter to every ES query in order to make the indexing synchronous (which is a must have when writing Unit or Integration tests). This also prepends all index names with a random UUID in order to isolate all tests runs from each other => this is to be compatible with a continuous integration environement.
* **username** and **password** : when the ES cluster requires authentication, it can be provided here.
* **scan-base-package** : This configuration sets which package is the root one for scanning Objects being annotated with the @ElasticsearchDocument
* **indexed-objects** : : Possibility to manually specify classes not being annotated with @ElasticsearchDocument. (if it's not possible to annotate it)
* **sync** : automatic synchronization between entities and ES documents. If not present, this feature is disabled by default.
  * **enabled** : whether or not to enable to feature
  * **rate-milliseconds** : the refresh rate to use when synchronizing entities. Since the indexation is not done synchronously with the entity life cycle, but in the background, user can choose the rate at which to sync the database and the ES cluster. 
  * **nb-tryouts** : In case of an error occuring during synchronization, how many times the process will retry syncing the same entity before giving up. (usefule in case of network issues or any unavailability of the ES cluster)  

### Use the library

#### Mark an Object as indexed in Elasticsearch

You need to annotate the object with the ``@ElasticsearchDocument`` annotation. It will let you choose the indexName under which you want to store your documents in Elasticsearch. 

This parameter is optional, and if not provided, the lowercase simplename of the class is used.

```java
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;

@Entity
@ElasticsearchDocument(synced = true, indexName = "myIndex")
public class Client implements Serializable {
    
    /**
    * Mandatory getter that will be used to retrieve the ES document ID 
    */
    public Long getId() {
        // ...
    }
}
```

#### Index, Update, Delete the Object in Elasticsearch

First, specify an Interface that inherits ElasticsearchRepository.

```java
package nc.rubiks.project.repository.search;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import nc.rubiks.project.domain.Client;

/**
 * Elasticsearch repository for the Client object.
 */
public interface ClientSearchRepository extends ElasticsearchRepository<Client, Long> {

}
```

Then, implement it using the AbstractElasticsearchRepository provided by the lib: 
```java
package nc.rubiks.project.repository.search.impl;

import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import nc.rubiks.core.search.elasticsearch.repository.impl.AbstractElasticsearchRepository;
import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import nc.rubiks.project.domain.Procuration;
import nc.rubiks.project.repository.search.ClientSearchRepository;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository impl for the Client object.
 */
@Repository
public class ClientSearchRepositoryImpl extends AbstractElasticsearchRepository<Client, Long> implements ClientSearchRepository {

    public ClientSearchRepositoryImpl(RestClient client, RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
        super(client, highLevelClient, documentMapper, elasticSearchTemplate, Client.class);
    }
}
```

You can know inject this repository in any other Spring Bean and access all its implemented methods : [See Interface Java Documentation](https://github.com/nicoraynaud/spring-rest-elasticsearch/blob/master/src/main/nc/rubiks/core/search/elasticsearch/repository/ElasticsearchRepository.java) :
* Page<T> search(Pageable pageable, String query)
* Page<T> search(Pageable pageable, QueryBuilder query)
* SearchResponse search(Pageable pageable, QueryBuilder query, AggregationBuilder aggregation)
* SearchResponse search(Pageable pageable, QueryBuilder query, Collection<AggregationBuilder> aggregations)
* Result<T> searchComplex(Pageable pageable, String jsonQuery)
* T findOne(ID id)
* boolean exists(ID id)
* long count()
* S save(S entity)
* Iterable save(Iterable entities)
* void delete(ID id)
* void delete(T entity)
* void delete(Iterable<? extends T> entities)
* void deleteAll()

#### Add your custom search queries

If the default provided features do not cover your needs and you must design advanced queries, you can extend your class with new search methods and implement your own queries using the ES Rest Client QueryBuilders :

First, define your new interface :

```java
package nc.rubiks.project.repository.search;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;

/**
 * Elasticsearch repository for the Client object.
 */
public interface ClientSearchRepositoryExtra {

    /**
     * Searches for clients items using the "names" property
     * (names is an elasticsearch concat property for all its name related properties)
     * based on allStatuses, the search is filtered with the Statut = ACTIVE
     *
     * @param query the query string
     * @param allStatuses the filter on statuses
     * @param pageable the page information
     * @return A list of procurations matching the query
     */
    Page<Client> findByNamesAndStatus(String query, Boolean allStatuses, Pageable pageable);
}
```

Then, Extends the existing ClientRepository interface with your code :

```java
/**
 * Elasticsearch repository for the Client object.
 */
public interface ClientSearchRepository extends ElasticsearchRepository<Client, Long>, ClientSearchRepositoryExtra {

}
```

And implement your search in the already existing concrete class :
```java
//...
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Elasticsearch repository impl for the Client object.
 */
@Repository
public class ClientSearchRepositoryImpl extends AbstractElasticsearchRepository<Client, Long> implements ClientSearchRepository {

    public ClientSearchRepositoryImpl(RestClient client, RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
        super(client, highLevelClient, documentMapper, elasticSearchTemplate, Client.class);
    }
    
    @Override
    public Page<Client> findByNamesAndStatus(String name, Boolean allStatuses, Pageable pageable) {
        
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.fetchSource(null, new String[]{"excludedProperty"});

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (StringUtils.isNotBlank(name)) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("names", name));
        }

        if (!allStatuses) {
            boolQueryBuilder.must(QueryBuilders.termQuery("status", "ACTIVE"));
        }

        searchSourceBuilder.from(pageable.getOffset());
        searchSourceBuilder.size(pageable.getPageSize());
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort("names");

        SearchResponse response = this.search(searchSourceBuilder);

        Page<PageBlancheSearchDTO> result;
        result = new PageImpl<>(Arrays.stream(response.getHits().getHits()).map(h -> {
            try {
                return documentMapper.mapToObject(h.getSourceAsMap(), getIndexedClass());
            } catch (IOException ex) {
                throw new OptElasticsearchException("Unable to parse result from ES : ", ex);
            }
        }).collect(Collectors.toList()),
            pageable,
            response.getHits().totalHits);

        return result;
            
    }
}
```

#### How to sync an Hibernate Entity with an Elasticsearch document

The @Elasticsearch annotation allows to specify whether the entity must be synchronized. This means that any change done to the entity (create/update/delete) will trigger an ES action (either index the document, reindex it or remove it).
This can be configured using two property of the annotation :
* **synced** : This boolean attribute is false by default but can be set to true to enable synchronization
* **namedQuery** : By default, the lib will use the basic Hibernate Session ``get`` method in order to retrive the object from the database, map it into a JSON document and index it. In many cases, it is useful to tune the query used to fetch an object from the database, especially if we want to index collections or children of the entity (we would then use fetch queries). For all these cases, the lib allows you to define which namedQuery to use when retrieving the object from the database. The namedQuery must be defined as usual using the @NamedQuery JPA annotation.

Rubiks library performs synchronization in the following manner :
* The entity is modified within an Hibernate Session (for example when an HTTP request is made to your app)
* The library detects this modification (CRUD) and creates an ``ElasticsearchSyncAction`` to remind that this entity must be synchronized.
* If the session is not properly commited to the database, the previously created ``ElasticsearchSyncAction`` is then dropped and nothing will be synchronized.
* If the session is successfully commited to the database, the previously created ``ElasticsearchSyncAction`` is persisted as well
* An asynchronous synchronization job will regularly (using the refresh-rate setting) poll the list of ``ElasticsearchSyncAction`` to perform and for each of them will :
  * Fetch the Entity from the databse (using the namedQuery if provided)
  * Convert the Entity into a JSON document using the default DocumentMapper or a custom implementation if provided
  * Call the related ElasticsearchRepository to perform the save() or delete() action.
  * Delete the ``ElasticsearchSyncAction``


#### How to trigger a child Entity synchronization when an parent Entity is modified

In any Entity being synced, if any of its property is marked with the ``@ElasticsearchTriggerSync``, a modification to the entity will trigger the sync of the Entity AND its property.

This will of course work only if the property being annotated is also marked with ``@ElasticsearchDocument``.

**Example :** I have a client with a list of contracts. Both my clients and contracts are indexed and the name of the client is indexed inside the contract ES document. 
With the following code I can decide that whenever my client is modified, it triggers the synchronization of each of his contracts (i.e. any change to the client's name will be reflected in the contracts documents)

```java
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchTriggerSync;

@Entity
@ElasticsearchDocument(synced = true)
public class Client implements Serializable {
    
    /**
    * Mandatory getter that will be used to retrieve the ES document ID 
    */
    public Long getId() {
        // ...
    }
    
    @OneToMany(mappedBy = "client")
    @ElasticsearchTriggerSync         // Now, whenever the client is modified, all its contracts are synced in ES as well 
    private Set<Contracts> contracts;
    
}


@Entity
@ElasticsearchDocument(synced = true)
public class Contract implements Serializable {
    
    /**
    * Mandatory getter that will be used to retrieve the ES document ID 
    */
    public Long getId() {
        // ...
    }
    
    @ManyToOne
    private Client client;
    
}

```

#### Configuring the Sync Job

In order to work, the Sync job relies on several keypoints :
* The Spring JPA configuraiton (application.yml): You need to tell JPA to use the provided Interceptor to catch all Session events :
```yaml
spring:
    jpa:
        properties:
            hibernate.session_factory.interceptor: nc.rubiks.core.search.elasticsearch.interceptor.ElasticsearchEntitySyncInterceptor
```
* The Lib configuration (application.yml) : ``rubiks.elasticsearch.sync.enabled: true`` to enable the feature.
* Spring Scheduling : this is contained within Sprinb Boot, nothing is needed for it to work
* Shedlock : This is a very simple library that enables the job to be compatible with multiple instances of the same application running in parallel. (it is embedded in the lib). *It uses a synchronization table to lock the job and ensure that only one instance of the job is running at a certain time. see [Shedlock](https://github.com/lukas-krecan/ShedLock) for more information.*
* Two tables in the database that need to be created (by you or liquibase):
  * A liquibase script is embedded in the library for each of these tables, you can use them like this :

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd">

    ...

    <!-- adding support for ES sync job -->
    <include file="classpath:config/liquibase/changelog/changeset_added_es_sync_action.xml" relativeToChangelogFile="false"/>
    <include file="classpath:config/liquibase/changelog/changeset_added_shedlock.xml" relativeToChangelogFile="false"/>

</databaseChangeLog>
```

If you need to, you can override the default Shedlock configuration by redefining the config Beans for ``LockProvider`` and ``ScheduledLockConfiguration``.

#### Custom configuration of ES indices

By default, the lib will index you Object as-is, it means that whatever you DocumentMapper implementation produces, it will be indexed.

If you need to customize your Elasticsearch Index mapping or settings (which is highly recommended), you can specify the mapping of the index and type and its settings.

This is done by providing json configuration files in the **resources/config/elasticsearch** folder of your app using the following conventions :
* **Mapping** : provide a file named like ``${simpleClassNameLowerCaseOrIndexName}.mapping.json`` 
  * MyType.java => mytype.mapping.json
  * MyType.java (indexName = "custom") => custom.mapping.json

This file contains the mapping of the class as it should be done by ES. See [Elasticsearch Mapping doc](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html)

* **Setting** : provide a file named like ``${simpleClassNameLowerCaseOrIndexName}.setting.json`` 
  * MyType.java => mytype.setting.json
  * MyType.java (indexName = "custom") => custom.setting.json

This file contains the settings of the index in ES. See [Elasticsearch Index doc](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html)

#### How to Index an Entity through a different DTO object

You might sometimes want to synchronize and index an Entity in a very specific format. In fact, a specific format that is so far from the source entity that it actually is a different object !

Example : I want to store my clients with very few fields and some calculated ones that do not appear in the Entity (for example, the number of contracts it has). This information is not present in the Entity itself and instead of modifying your domain, you can simply define an additional DTO that will have these properties.

First, specify that the entity is indexed as another DTO object :

```java
@Entity
@ElasticsearchDocument(synced = true, documentType = ClientSearchDTO.class)
public class Client implements Serializable {
    
    private Long id;
    
    private String name;
    
    private Set<Contract> contracts;
}
```

Then specify your DTO
```java
public class ClientSearchDTO {
    
    private Long id;
    
    private String name;
    
    private int nbContracts;
}
```

You then need to change your ElasticsearchRepository interface and implementation to the following :

 ```java
 
 /**
  * Elasticsearch repository for the Client object.
  */
 public interface ClientSearchRepository extends ElasticsearchRepository<ClientSearchDTO, Long> {
 
 }
 
 /**
  * Elasticsearch repository impl for the ClientSearchDTO object.
  */
 @Repository
 public class ClientSearchRepositoryImpl extends AbstractElasticsearchRepository<ClientSearchDTO, Long> implements ClientSearchRepository {
 
     public ClientSearchRepositoryImpl(RestClient client, RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
         super(client, highLevelClient, documentMapper, elasticSearchTemplate, ClientSearchDTO.class, Client.class);
     }
 }
 ```

And finally, the synchronization job will need to know how to convert your entity being modified into a DTO object to be indexed. 
You therefore need to implement a new interface that does so. The lib will then automatically pick it up at runtime and use it when synchronizing your entity :

````java
@Component // mark it as a Spring component in order for the lib to detect it automatically at startup
public class ClientToClientSearchDTOConverter implements EntityToElasticsearchDocumentConverter<Client, ClientSearchDTO> {
    
        private final ClientRepository clientRepository;
    
        @Autowired
        public ClientToClientSearchDTOConverter(ClientRepository clientRepository) {
            this.procurationRepository = procurationRepository;
        }
    
        public Class<Client> getEntityType() {
            return Client.class;        
        }
    
        public Class<ClientSearchDTO> getDocumentType() {
            return ClientSearchDTO.class;
        }
    
        /**
         * Methods that returns a Document representation from an entity ID
         * Note that, when called by the ElasticsearchSyncService, the ID is always a string.
         * It is your responsibility to cast the ID into the actual entity ID type.
         * @param id The entity ID in database
         * @return The Document to index in Elasticsearch
         */
        public ClientSearchDTO convert(String id) {
            Client c = this.clientRepository.get(id);
            
            // Create the DTO and initialize it
            // you could use any other bean to help you do that (a mapstruct or dozer converter for example)
            ClientSearchDTO result = new ClientSearchDTO(c);
            result.setNbContracts(c.getContracts().size());
            
            return result;
        }
}
````

### Testing

#### Unit or Integration tests

In order to run Unit or Integration tests, you will need a local version of ES running on your workstation (or a remote one if you happily have a test cluster running).

#### Setting the configuration

Don't forget to set the ``test-mode`` to true so that any interaction with the cluster is synchronous and your documents are available for search as soon as the ES cluster answers your request.
You should also disabled the sync job (no need to sync anything yet...)

```yaml
rubiks:
    elasticsearch:
        cluster-nodes: http://localhost:9200  # ES cluster 
        context:
        test-mode: true                       # test-mode enabled !!!
        username:                             # no username/password
        password:
        scan-base-package: nc.rubiks          # root package to scan at startup for indexed objects
        indexed-objects:                      # csv list of fullname classes to be synchronized when not annotated
        sync:
            enabled: false                    # no need to sync while testing
```

#### Write unit tests with Spring Tests

With the abover configuration in your src/test/resources/config/application.yml, the lib will automatically wire the necessary beans. It then is a simple matter of wiring your beans and play with them :

````java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProjectApp.class)
public class ClientSearchRepositoryImplTest {

    @Autowired
    private ClientSearchRepository clientSearchRepository;

    @Autowired
    private ElasticSearchTemplate elasticSearchTemplate;
        
    @Autowired
    private RestClient restClient;
    
    @Before
    public void setUp() {
        // create documents in your ES test index
    }

    @Test
    public void my_test() {
        // do your thing....
    }

    @After
    public void tearDown() {
        // delete all documents from this index between each test to ensure their independance
        clientSearchRepository.deleteAll();
    }
}
````

#### Write unit tests with JUnit

This way requires a bit more work as the Spring context will not load all the necessary beans for you.

I recommend creating a base test class that can be inherited by each TestSuite and provided some basic configuration and behavior:

````java
package nc.rubiks.core.search.elasticsearch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by nicoraynaud on 03/05/2017.
 */
public abstract class BaseESTestCase {

    protected static RestHighLevelClient highLevelClient;
    protected static ElasticSearchTemplate template;

    @BeforeClass
    public static void beforeClass() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map confYml = mapper.readValue(BaseESTestCase.class.getClassLoader().getResourceAsStream("config/application.yml"), Map.class);

        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(((Map) ((Map)confYml.get("rubiks")).get("elasticsearch")).get("cluster-nodes").toString());
        if (((Map)((Map)confYml.get("rubiks")).get("elasticsearch")).get("username") != null) {
            properties.setUsername(((Map) ((Map) confYml.get("rubiks")).get("elasticsearch")).get("username").toString());
        }
        if (((Map)((Map)confYml.get("rubiks")).get("elasticsearch")).get("password") != null) {
            properties.setPassword(((Map) ((Map) confYml.get("rubiks")).get("elasticsearch")).get("password").toString());
        }

        // Always set test mode to true
        properties.setTestMode(true);

        TestRubiksElasticsearchAutoConfiguration conf = new TestRubiksElasticsearchAutoConfiguration(properties);
        template = conf.buildElasticSearchTemplate();
        highLevelClient = conf.buildRestClient(template);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if (template != null && highLevelClient != null) template.deleteAllIndices(highLevelClient.getLowLevelClient());
        if (highLevelClient != null) highLevelClient.close();
    }

    private static class TestRubiksElasticsearchAutoConfiguration extends RubiksElasticsearchAutoConfiguration {
        private TestRubiksElasticsearchAutoConfiguration(RubiksElasticsearchProperties rubiksElasticsearchProperties) {
            super(rubiksElasticsearchProperties);
        }
    }
}

````

This ``BaseESTestCase`` will do the following for you :
* Read the configuration from the src/test/resources/config/application.yml file
* Instanciate the necessary ElasticsearchTemplate and other low level objects
* After each Test Suite (class), it deletes all indexes and data created for the tests, leaving the ES cluster free from garbage data. (note that it only deletes the test indexes).

You can then use it easily : 

```java
@RunWith(JUnit4.class)
public class AbstractElasticsearchRepositoryTest extends BaseESTestCase {

    private TestElasticsearchRepository testElasticsearchRepository;

    @Before
    public void before() {
        template.deleteIndex(highLevelClient.getLowLevelClient(), "theentity");
        testElasticsearchRepository = new TestElasticsearchRepository(highLevelClient, template, new TestMapper());
    }

    private void indexEntity(Long id, String prop) {
        testElasticsearchRepository.save(new TheEntity().id(id).prop(prop));
    }

    @Test
    public void getIndexName_whenNoDTO_returnSimpleClassName() {

        // Given
        TestElasticsearchRepository testElasticsearchRepository = new TestElasticsearchRepository(highLevelClient, template, new TestMapper());

        // When & Then
        assertThat(testElasticsearchRepository.indexName).isEqualTo("theentity");
    }
}

```
