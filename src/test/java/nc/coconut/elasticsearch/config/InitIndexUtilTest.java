package nc.coconut.elasticsearch.config;

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
        template.deleteIndice(client, "entitywithoutfiles");
        template.deleteIndice(client, "entitywithfiles");
        template.deleteIndice(client, "entitywithmapping");
        template.deleteIndice(client, "entitywithsetting");
    }

    @After
    public void after() {
        template.deleteIndice(client, "entitywithoutfiles");
        template.deleteIndice(client, "entitywithfiles");
        template.deleteIndice(client, "entitywithmapping");
        template.deleteIndice(client, "entitywithsetting");
    }

    @Test
    public void testCall_noFiles_doNothing() {

        // Given
        Class c = EntityWithoutFiles.class;

        // When
        InitIndexUtil.initIndices(client, template, c);

        // Then
        assertThat(template.indiceExists(client, "entitywithoutfiles", "entitywithoutfiles")).isFalse();
    }

    @Test
    public void testCall_withFiles_createIndex() throws IOException {

        // Given
        Class c = EntityWithFiles.class;

        // When
        InitIndexUtil.initIndices(client, template, c);

        // Then
        assertThat(template.indiceExists(client, "entitywithfiles", "entitywithfiles")).isTrue();
        assertThat(template.readIndice(client, "entitywithfiles")).contains("\"id\"");
        assertThat(template.readIndice(client, "entitywithfiles")).contains("my_ascii_folding");
    }

    @Test
    public void testCall_withMappingFile_createIndex() throws IOException {

        // Given
        Class c = EntityWithMapping.class;

        // When
        InitIndexUtil.initIndices(client, template, c);

        // Then
        assertThat(template.indiceExists(client, "entitywithmapping", "entitywithmapping")).isTrue();
        assertThat(template.readIndice(client, "entitywithmapping")).contains("\"id\"");
        assertThat(template.readIndice(client, "entitywithmapping")).doesNotContain("my_ascii_folding");
    }

    @Test
    public void testCall_withSettingFile_createIndex() {

        // Given
        Class c = EntityWithSetting.class;

        // When
        InitIndexUtil.initIndices(client, template, c);

        // Then
        assertThat(template.indiceExists(client, "entitywithsetting", "entitywithsetting")).isTrue();
        assertThat(template.readIndice(client, "entitywithsetting")).doesNotContain("\"id\"");
        assertThat(template.readIndice(client, "entitywithsetting")).contains("my_ascii_folding");
    }
}
