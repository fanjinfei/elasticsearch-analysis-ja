package org.codelibs.elasticsearch.ja.analysis;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReloadableKeywordMarkerFilterFactoryTest {

    private ElasticsearchClusterRunner runner;

    private int numOfNode = 3;

    private File[] keywordFiles;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-analysisja-" + System.currentTimeMillis();
        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.put("index.number_of_shards", 3);
                settingsBuilder.put("index.number_of_replicas", 0);
                settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9310");
                settingsBuilder.put("plugin.types", "org.codelibs.elasticsearch.ja.JaPlugin");
                settingsBuilder.put("index.unassigned.node_left.delayed_timeout", "0");
            }
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode));

        keywordFiles = null;
    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
        if (keywordFiles != null) {
            for (File file : keywordFiles) {
                file.deleteOnExit();
            }
        }
    }

    @Test
    public void test_basic() throws Exception {
        keywordFiles = new File[numOfNode];
        for (int i = 0; i < numOfNode; i++) {
            String confPath = runner.getNode(i).settings().get("path.conf");
            keywordFiles[i] = new File(confPath, "keywords.txt");
            updateDictionary(keywordFiles[i], "consisted\nconsists");
        }

        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{" + "\"filter\":{"
                + "\"stem1_filter\":{\"type\":\"flexible_porter_stem\",\"step1\":true,\"step2\":false,\"step3\":false,\"step4\":false,\"step5\":false,\"step6\":false},"
                + "\"marker_filter\":{\"type\":\"reloadable_keyword_marker\",\"keywords_path\":\"keywords.txt\",\"reload_interval\":\"1s\"}"
                + "},"//
                + "\"analyzer\":{" + "\"default_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"stem1_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"marker_filter\",\"stem1_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings).build());
        runner.ensureYellow();

        {
            String text = "consist consisted consistency consistent consistently consisting consists";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "stem1_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(7, tokens.size());
                assertEquals("consist", tokens.get(0).get("token").toString());
                assertEquals("consisted", tokens.get(1).get("token").toString());
                assertEquals("consistency", tokens.get(2).get("token").toString());
                assertEquals("consistent", tokens.get(3).get("token").toString());
                assertEquals("consistently", tokens.get(4).get("token").toString());
                assertEquals("consist", tokens.get(5).get("token").toString());
                assertEquals("consists", tokens.get(6).get("token").toString());
            }
        }

        for (int i = 0; i < numOfNode; i++) {
            String confPath = runner.getNode(i).settings().get("path.conf");
            keywordFiles[i] = new File(confPath, "keywords.txt");
            updateDictionary(keywordFiles[i], "consisting\nconsistent");
        }

        Thread.sleep(1100);

        {
            String text = "consist consisted consistency consistent consistently consisting consists";
            try (CurlResponse response =
                    Curl.post(node, "/" + index + "/_analyze").param("analyzer", "stem1_analyzer").body(text).execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
                assertEquals(7, tokens.size());
                assertEquals("consist", tokens.get(0).get("token").toString());
                assertEquals("consist", tokens.get(1).get("token").toString());
                assertEquals("consistency", tokens.get(2).get("token").toString());
                assertEquals("consistent", tokens.get(3).get("token").toString());
                assertEquals("consistently", tokens.get(4).get("token").toString());
                assertEquals("consisting", tokens.get(5).get("token").toString());
                assertEquals("consist", tokens.get(6).get("token").toString());
            }
        }

    }

    private void updateDictionary(File file, String content) throws IOException, UnsupportedEncodingException, FileNotFoundException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            bw.write(content);
            bw.flush();
        }
    }
}