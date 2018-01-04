package nc.rubiks.core.search.elasticsearch.repository.impl;

import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ElasticSearchTemplateTest {

    private ElasticSearchTemplate template;
    private RestClient restClient;
    private String rootIndiceName;
    private String typeName;

    @Before
    public void setUp() {
        template = new ElasticSearchTemplate("context", false);
        restClient = mock(RestClient.class);
        rootIndiceName = "root";
        typeName = "type";
    }

    @Test
    public void test_indiceExists_error_returnFalse() throws IOException {

        // Given
        when(restClient.performRequest(eq("HEAD"), eq("/context/root"), eq(Collections.emptyMap()))).thenThrow(new IOException());

        // When
        Boolean result = template.indexExists(restClient, rootIndiceName, typeName);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void test_readIndice_error_returnNull() throws IOException {

        // Given
        when(restClient.performRequest(eq("GET"), eq("/context/root"), eq(Collections.emptyMap()))).thenThrow(new IOException());

        // When
        String indice = template.readIndex(restClient, rootIndiceName);

        // Then
        assertThat(indice).isNull();
    }

    @Test
    public void test_deleteAllIndices_IndiceNotFound_doNothing() throws IOException {

        // Given
        Response r = mock(Response.class);
        when(r.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, ""));
        when(r.getRequestLine()).thenReturn(new BasicRequestLine("DELETE", "", new ProtocolVersion("HTTP", 1, 1)));
        ResponseException rExp = new ResponseException(r);
        when(restClient.performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()))).thenThrow(rExp);

        // When
        template.deleteAllIndices(restClient);

        // Then
        verify(restClient, times(1)).performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()));

    }

    @Test
    public void test_deleteAllIndices_OtherHttpStatus_doNothing() throws IOException {

        // Given
        Response r = mock(Response.class);
        when(r.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 500, ""));
        when(r.getRequestLine()).thenReturn(new BasicRequestLine("DELETE", "", new ProtocolVersion("HTTP", 1, 1)));
        ResponseException rExp = new ResponseException(r);
        when(restClient.performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()))).thenThrow(rExp);

        // When
        template.deleteAllIndices(restClient);

        // Then
        verify(restClient, times(1)).performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()));

    }

    @Test
    public void test_deleteAllIndices_exception_doNothing() throws IOException {

        // Given
        when(restClient.performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()))).thenThrow(new IOException());

        // When
        template.deleteAllIndices(restClient);

        // Then
        verify(restClient, times(1)).performRequest(eq("DELETE"), eq("/*"), eq(Collections.emptyMap()));

    }

}
