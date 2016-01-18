package org.codelibs.elasticsearch.ja.analysis;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AlphaNumWordFilterFactoryTest {

    private ElasticsearchClusterRunner runner;

    private int numOfNode = 1;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-analysisja-" + System.currentTimeMillis();
        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("index.number_of_replicas", 0);
                settingsBuilder.put("index.number_of_shards", 3);
            }
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode));

    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_basic() throws Exception {

        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{" //
                + "\"filter\":{" //
                + "\"alphanum_word_filter\":{\"type\":\"alphanum_word\"}" + "}," //
                + "\"tokenizer\":{" //
                + "\"unigram_analyzer\":{\"type\":\"nGram\",\"min_gram\":\"1\",\"max_gram\":\"1\",\"token_chars\":[\"letter\",\"digit\"]}"
                + "},"//
                + "\"analyzer\":{" //
                + "\"ngram_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"unigram_analyzer\"},"
                + "\"ngram_word_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"unigram_analyzer\",\"filter\":[\"alphanum_word_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, ImmutableSettings.builder().loadFromSource(indexSettings).build());
        runner.ensureYellow();

        {
            String text = "aaa";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ngram_word_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(1, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
            }
        }

        {
            String text = "aaa bbb";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ngram_word_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(2, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
                assertEquals("bbb", tokens.get(1).get("token").toString());
            }
        }

        {
            String text = "aa1 bb2 333";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ngram_word_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(3, tokens.size());
                assertEquals("aa1", tokens.get(0).get("token").toString());
                assertEquals("bb2", tokens.get(1).get("token").toString());
                assertEquals("333", tokens.get(2).get("token").toString());
            }
        }

        {
            String text = "aaa亜aaa";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ngram_word_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(3, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
                assertEquals("亜", tokens.get(1).get("token").toString());
                assertEquals("aaa", tokens.get(2).get("token").toString());
            }
        }

        {
            String text = "嬉しい";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ngram_word_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(3, tokens.size());
                assertEquals("嬉", tokens.get(0).get("token").toString());
                assertEquals("し", tokens.get(1).get("token").toString());
                assertEquals("い", tokens.get(2).get("token").toString());
            }
        }

    }

}
