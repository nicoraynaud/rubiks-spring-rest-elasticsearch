# spring-rest-elasticsearch

This library brings support of Elasticsearch v5.3 for Java Spring projects based on Spring Boot (and Spring Data). It was initially built because the spring-data-elasticsearch library does not yet support ES 5.x and only uses the transport client.

This work is greatly inspired by the spring-data-elasticsearch library even if it does not meet its advanced feature set.

This library is built on top of official [Elasticsearch Java REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest.html). All communications with an ES cluster is made through the REST API and not the Transport protocole (now deprecated by ES).

> This Library is compatible with ES 5.3 and 5.4. Previous and later 5.x versions should also be compatible : but you should test first :).

This library allows :
* Automatic creation of elasticsearch indices (mapping and settings) at startup by using local config file embedded in classpath : using [Indices API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html)
* Index, update, delete and search of any entity through the use of AbstractElasticsearchRepository class.
* Search of any indexed entity using ES [Search API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html) language. 

## Development

To build this project, you need to have Gradle installed on your computer.

In order to run the unit tests, you will need to setup a local ES cluster node or connect to a remote one.

Here follows a docker-compose setup compatible with the library :

```yaml
    version: '1'
    services:
        spring-rest-elasticsearch-5.x:
            image: docker.elastic.co/elasticsearch/elasticsearch:5.3.2
            mem_limit: 2048m
            # volumes:
            #     - ~/volumes/jhipster/procuration/elasticsearch/:/usr/share/elasticsearch/data/
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

You need to reference both this library and the elasticsearch REST client.

#### Gradle

```groovy
    dependencies {
        
        // Elasticsearch support
        compile group: 'nc.coconut', name: 'spring-rest-elasticsearch', version: spring_rest_elasticsearch_version
        provided group: 'org.elasticsearch.client', name: 'rest', version: elasticsearch_client_version
        
    }
```

#### Maven

```xml
    <dependencies>
        <dependency>
            <groupId>nc.coconut</groupId>
            <artifactId>spring-rest-elasticsearch</artifactId>
            <version>${spring_rest_elasticsearch_version}</version>
        </dependency>
        <dependency>
            <groupId>org.elasticsearch.client</groupId>
            <artifactId>rest</artifactId>
            <version>${elasticsearch_client_version}</version>
        </dependency>
    </dependencies>
```

### Development

#### Prerequisites

To allow the library to work properly, the following points are mandatory :
* Entities meant to be indexed need to have a `getId()` method that will return its unique id throughout the index.
* You need to use Spring and especially Spring-Data because the library use their objects to create queries (Page, Pageable, etc...)

#### Spring Configuration

Let's create a Configuration class for elasticsearch :

```java 
    @Configuration
    public class ElasticsearchConfiguration extends BaseElasticsearchConfiguration {
    
        public ElasticsearchConfiguration(ElasticsearchProperties elasticsearchProperties) {
            super(elasticsearchProperties);
        }
    
        @Bean
        public RestClient buildRestClient(ElasticSearchTemplate elasticSearchTemplate) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
            return super.buildRestClient(elasticSearchTemplate, MyEntity.class, MyEntity2.class, ...);
        }
    
        @Bean
        public EntityMapper buildEntityMapper(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
            return new CustomEntityMapper(jackson2ObjectMapperBuilder.createXmlMapper(false).build());
        }
    
        public class CustomEntityMapper implements EntityMapper {
    
            private ObjectMapper objectMapper;
    
            public CustomEntityMapper(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
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
               public JsonNode readTree(String inputSource) throws IOException {
                   return objectMapper.readTree(inputSource);
               }
           }
       
       }
```

And a configuration in the yml file :

```yaml
    # Elasticsearch configuration
    coconut:
       elasticsearch:
           cluster-nodes: http://localhost:9200
           context: 
           test-mode: false
           username: username
           password: password
```

The above configuration explained :
* cluster-nodes : a comma separated list of cluster nodes to connect to. They need to be specified with the scheme (http, https) and the port.
* context : the potential context where the api of the ES cluster is available at : i.e. : https://mydomain/elasticsearch -> 'elasticsearch' is the context
* test-mode : this is to be used in testing configuration : this enables a `wait_for` parameter to every ES query in order to make the indexing synchronous (which is a must have when writing Unit or Integration tests)
* username and password : when the ES cluster requires authentication, it can be provided here  


#### Custom configuration of ES indices

When building the restClient (`super.buildRestClient(...)`), you have the possibility to specify classes that will be indexed with ES. This configuration is optional (any object can be indexed with ES if they meet the prerequisites) and can be used to specify a custom mapping configuration in ES.

Specifying a class (like `MyEntity.class`) will trigger an automatic search of resources folder to find two files (but you can provide only one of them) :
* `src/main/resources/config/elasticsearch/myentity.mapping.json`

This file contains the mapping of the class as it should be done by ES. See [Elasticsearch Mapping doc](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html)

* `src/main/resources/config/elasticsearch/myentity.setting.json`

This file contains the settings of the index in ES. See [Elasticsearch Index doc](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html)

The library will only look for a file prefixed with the entity classname in lowercase.

#### Using Repositories

When developping you can easily enable CRUD for your entities by creating the following code :

```java
    package nc.coconut.example.repository.search;
     
    import nc.coconut.elasticsearch.repository.ElasticsearchRepository;
    import nc.coconut.example.domain.TheEntity;
     
    /**
     * Elasticsearch repository for the TheEntity entity.
     * 
     * => TheEntity has an Id property of type Long
     */
    public interface TheEntitySearchRepository extends ElasticsearchRepository<TheEntity, Long> {
     
    }
```

And its implementation :

```java
    package nc.coconut.example.repository.search.impl;
     
    import nc.coconut.elasticsearch.mapper.EntityMapper;
    import nc.coconut.elasticsearch.repository.impl.AbstractElasticsearchRepository;
    import nc.coconut.elasticsearch.repository.impl.ElasticSearchTemplate;
    import nc.coconut.example.domain.Procuration;
    import nc.coconut.example.repository.search.ProcurationSearchRepository;
    import org.elasticsearch.client.RestClient;
    import org.springframework.stereotype.Repository;
     
    @Repository
    public class TheEntitySearchRepositoryImpl extends AbstractElasticsearchRepository<TheEntity, Long> implements TheEntitySearchRepository {
     
        protected TheEntitySearchRepositoryImpl(RestClient client, ElasticSearchTemplate elasticSearchTemplate, EntityMapper entityMapper) {
            super(client, elasticSearchTemplate, entityMapper, TheEntity.class);
        }
    }
```

And that's it. You are now enabled to use all the methods declared by the ElasticsearchRepository interface (index, delete, update, query, pagination ...)

#### Creating a more complex query

You can add your custom search queries easily by using the searchComplex() method provided by AbstractElasticsearchRepository that allows you to query ES using its QueryAPI in JSON.

Here follows an example :

```java
    @Repository
    public class TheEntitySearchRepositoryImpl extends AbstractElasticsearchRepository<TheEntity, Long> implements TheEntitySearchRepository {
     
        protected TheEntitySearchRepositoryImpl(RestClient client, ElasticSearchTemplate elasticSearchTemplate, EntityMapper entityMapper) {
            super(client, elasticSearchTemplate, entityMapper, TheEntity.class);
        }
     
        @Override
        public Page<TheEntity> findByNamesAndStatus(String query, Boolean allStatuses, Pageable pageable) {
     
            String actualQueryFormat = "{\"query\": {\"query_string\": { \"analyze_wildcard\": true, \"query\": \"%s\"} } }";
            String filters = "";
     
            // default search is : wildcard to match all
            if (StringUtils.isBlank(query)) {
                filters = "*";
            } else {
                // Or else, we split the query and add it as a term concatenated using AND
                filters = Arrays.stream(query.split(StringUtils.SPACE))
                    .map(s -> String.format("names:%s", s))
                    .collect(Collectors.joining(" AND "));
            }
     
            // Based on the status allStatuses parameter, we add a query term on the filter
            if (!allStatuses) {
                filters += String.format("%sstatut:%s",
                    filters.length() != 0 ? " AND " : StringUtils.EMPTY ,
                    StatutProcuration.ACTIVE.toString());
            }
     
            actualQueryFormat = String.format(actualQueryFormat, filters);
     
            // We then use the parent searchComplex method to query ES for us
            return this.searchComplex(pageable, actualQueryFormat);
        }
    }
```

### Testing

#### Unit or Integration tests

I encourage you to write a base test case for all your ES tests like follows :

```java
    public abstract class BaseESTestCase {
     
        protected static RestClient client;
        protected static ElasticSearchTemplate template;
     
        @BeforeClass
        public static void beforeClass() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
     
            OptElasticsearchProperties properties = new OptElasticsearchProperties();
            properties.setClusterNodes("https://localhost:9200");
            properties.setUsername("username");
            properties.setPassword("password");
     
            // Always set test mode to true
            properties.setTestMode(true);
     
            TestOptElasticsearchConfiguration conf = new TestOptElasticsearchConfiguration(properties);
            template = conf.buildElasticSearchTemplate();
            client = conf.buildRestClient(template);
        }
     
        @AfterClass
        public static void afterClass() throws IOException {
            if (template != null) template.deleteAllIndices(client);
            if (client != null) client.close();
        }
     
        private static class TestOptElasticsearchConfiguration extends BaseOptElasticsearchConfiguration {
            private TestOptElasticsearchConfiguration(OptElasticsearchProperties optElasticsearchProperties) {
                super(optElasticsearchProperties);
            }
        }
    }
```

Using this base class, you will be able to initialize your beans properly and moreover, clean up you ES cluster after the test finish running.

Here is an example of a test class :

```java
    @RunWith(JUnit4.class)
    public class TheEntityElasticsearchRepositoryTest extends BaseESTestCase {
     
        private TheEntityElasticsearchRepository theEntityElasticsearchRepository;
     
        @Before
        public void before() {
            template.deleteIndice(client, "theentity");
            theEntityElasticsearchRepository = new TheEntityElasticsearchRepository(client, template, new TestMapper());
        }
     
        private void indexEntity(String id, String prop) {
            template.index(client, "theentity", "theentity", id,
                "{ \"id\": " + id + ", \"prop\": \"" + prop + "\" }");
        }
     
        @Test
        public void test_save() {
            // Given
            TheEntity e = new TheEntity();
            e.setId(5556l);
     
            // When
            testElasticsearchRepository.save(e);
     
            // Then
            assertThat(testElasticsearchRepository.findOne(5556l).getId()).isEqualTo(5556l);
        }
     
        @Test
        public void searchWithJsonQuery() {
     
            // Given
            indexEntity("1001", "element");
            indexEntity("1002", "element");
            indexEntity("1111", "element");
     
            String jsonQuery = "{\n" +
                "    \"query\" : {\n" +
                "        \"term\" : { \"id\" : \"1002\" }\n" +
                "    }\n" +
                "}";
     
            // When
            Page<TheEntity> result = testElasticsearchRepository.searchComplex(new PageRequest(0,5), jsonQuery);
     
            // Then
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(1002);
        }
    }
```
