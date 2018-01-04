package nc.rubiks.core.search.elasticsearch.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by 2617ray on 03/05/2017.
 */
@RunWith(JUnit4.class)
public class InitIndexUtilTest extends BaseESTestCase {

    @Before
    public void before() {
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithoutfiles");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithfiles");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithmapping");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithsetting");
    }

    @After
    public void after() {
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithoutfiles");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithfiles");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithmapping");
        template.deleteIndex(highLevelClient.getLowLevelClient(), "entitywithsetting");
    }

    @Test
    public void testCall_noFiles_doNothing() {

        // Given
        Class c = EntityWithoutFiles.class;

        // When
        InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, c);

        // Then
        assertThat(template.indexExists(highLevelClient.getLowLevelClient(), "entitywithoutfiles", "entitywithoutfiles")).isFalse();
    }

    @Test
    public void testCall_withFiles_createIndex() throws IOException {

        // Given
        Class c = EntityWithFiles.class;

        // When
        InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, c);

        // Then
        assertThat(template.indexExists(highLevelClient.getLowLevelClient(), "entitywithfiles", "entitywithfiles")).isTrue();
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithfiles")).contains("\"id\"");
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithfiles")).contains("my_ascii_folding");
    }

    @Test
    public void testCall_withMappingFile_createIndex() throws IOException {

        // Given
        Class c = EntityWithMapping.class;

        // When
        InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, c);

        // Then
        assertThat(template.indexExists(highLevelClient.getLowLevelClient(), "entitywithmapping", "entitywithmapping")).isTrue();
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithmapping")).contains("\"id\"");
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithmapping")).doesNotContain("my_ascii_folding");
    }

    @Test
    public void testCall_withSettingFile_createIndex() {

        // Given
        Class c = EntityWithSetting.class;

        // When
        InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, c);

        // Then
        assertThat(template.indexExists(highLevelClient.getLowLevelClient(), "entitywithsetting", "entitywithsetting")).isTrue();
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithsetting")).doesNotContain("\"id\"");
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "entitywithsetting")).contains("my_ascii_folding");
    }

    @Test
    public void testCall_withindexNameAndSetting_createIndex() {

        // Given
        Class c = EntityWithSettingAndIndexName.class;

        // When
        InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, c);

        // Then
        assertThat(template.indexExists(highLevelClient.getLowLevelClient(), "theindexname", "theindexname")).isTrue();
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "theindexname")).doesNotContain("\"id\"");
        assertThat(template.readIndex(highLevelClient.getLowLevelClient(), "theindexname")).contains("my_ascii_folding");
    }
}
