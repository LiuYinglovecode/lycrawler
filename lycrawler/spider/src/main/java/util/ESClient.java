package util;

import config.Configured;
import config.IConfigManager;
import es.utils.ReflectionUtils;
import es.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESClient extends Configured {
    /**
     * Builder pattern for ESClient
     *
     * @author yangduan
     */
    public static class ESClientBuilder {
        private IConfigManager config;

        public ESClientBuilder setConfig(IConfigManager config) {
            this.config = config;
            return this;
        }

        public ESClient createESClient() {
            return ReflectionUtils.newInstance(ESClient.class, this.config);
        }

    }

    private final static Logger logger = LoggerFactory.getLogger(ESClient.class);

    private static TransportClient client;

    /**
     * 获取ES客户端
     *
     * @return
     */
    public synchronized TransportClient getClient() {
        if (null == client) {
            client = createClient();
        }

        return client;
    }

    /**
     * 创建ES TransportClient
     *
     * @return
     */
    @SuppressWarnings("resource")
    private TransportClient createClient() {
        TransportClient result = null;
        try {
//            Properties config = getConf().config("spiderES").getAll();
            //配置es
            Settings settings = Settings.builder()
                    .put("cluster.name",  "YUNLULOG1")
//                        .put("discovery.type", config.getProperty("discovery.type", "zen"))//发现集群方式
//                        .put("discovery.zen.minimum_master_nodes", Integer.parseInt(config.getProperty("discovery.zen.minimum_master_nodes", "2")))//最少有2个master存在
//                        .put("discovery.zen.ping_timeout", config.getProperty("discovery.zen.ping_timeout", "200ms"))//集群ping时间，太小可能会因为网络通信而导致不能发现集群
//                        .put("discovery.initial_state_timeout",config.getProperty("discovery.zen.initial_state_timeout", "500ms"))
//                        .put("gateway.type", config.getProperty("gateway.type", "local"))//(fs, none, local)
//                        .put("index.number_of_shards", Integer.parseInt(config.getProperty("index.number_of_shards", "1")))
//                        .put("action.auto_create_index", Boolean.parseBoolean(config.getProperty("action.auto_create_index", "false")))//配置是否自动创建索引
//                        .put("cluster.routing.schedule", config.getProperty("cluster.routing.schedule", "50ms"))//发现新节点时间
//                        .put("client.transport.sniff", Boolean.parseBoolean(config.getProperty("client.transport.sniff", "true")))

                    .build();
            // 从属性文件中获取搜索服务器相对域地址
//            String transportAddresses = config.getProperty("transport.addresses");
            //集群s
            String transportAddresses="10.153.40.216:9300,10.153.40.217:9300,10.153.40.218:9300,10.153.51.218:9300,10.153.51.219:9300,10.153.51.63:9300";
//            String transportAddresses="127.0.0.1:9300";


            // 集群地址配置
            List<InetSocketTransportAddress> list = new ArrayList<>();
            if (StringUtils.isNotEmpty(transportAddresses)) {
                String[] strArr = transportAddresses.split(",");
                for (String str : strArr) {
                    String[] addressAndPort = str.split(":");
                    String address = addressAndPort[0];
                    int port = Integer.valueOf(addressAndPort[1]);

                    InetSocketTransportAddress inetSocketTransportAddress = new InetSocketTransportAddress(InetAddress.getByName(address), port);
                    list.add(inetSocketTransportAddress);
                }
            }

            // 这里可以同时连接集群的服务器,可以多个,并且连接服务是可访问的
            InetSocketTransportAddress addressList[] = list.toArray(new InetSocketTransportAddress[list.size()]);

            result = new PreBuiltTransportClient(settings).addTransportAddresses(addressList);
        } catch (Exception e) {
            logger.error("获取ES客户端对象异常：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void closeESClient() {
        if (null != client) {
            client.close();
            client = null;
        }
    }

    /**
     * 删除某个索引
     *
     * @param index 索引名称
     * @param type  索引类型
     * @param id    索引id
     */
    public void deleteDocument(String index, String type, String id) {
        TransportClient client = getClient();
        client.prepareDelete(index, type, id).get();
    }

    /**
     * 从searchResponse读取结果
     *
     * @param searchResponse
     * @return
     */
    public static SearchResult getSearchResult(SearchResponse searchResponse) {
        SearchHits hits = searchResponse.getHits();

        SearchResult result = new SearchResult();
        result.setTotalCount(hits.getTotalHits());

        if (result.getTotalCount() > 0) {
            List<Map<String, Object>> resultList = new ArrayList<>();

            SearchHit[] hitList = hits.getHits();
            for (int i = 0; i < hitList.length; i++) {
                Map<String, Object> fieldValues = new HashMap<>();
                SearchHit hit = hitList[i];

                String id = hit.getId();
                fieldValues.put("_id", id);

                Map<String, SearchHitField> fields = hit.getFields();

                for (Map.Entry<String, SearchHitField> entry : fields.entrySet()) {
                    String name = entry.getKey();

                    if (name.indexOf('.') > 0) {
                        //f.a1  f.a2 ==> f.{a1, a2}
                        String leftName = name.substring(0, name.indexOf('.'));
                        String rightName = name.substring(name.indexOf('.') + 1);

                        if (!fieldValues.containsKey(leftName)) {
                            fieldValues.put(leftName, new ArrayList<Map<String, Object>>());
                        }

                        List<Object> contents = entry.getValue().getValues();
                        @SuppressWarnings("unchecked")
                        ArrayList<Map<String, Object>> list = (ArrayList<Map<String, Object>>) fieldValues.get(leftName);
                        for (int j = 0; j < contents.size(); j++) {
                            if (list.size() < j + 1) {
                                list.add(new HashMap<>());
                            }

                            Object content = contents.get(j);
                            list.get(j).put(rightName, content);
                        }
                    } else {
                        Object content = entry.getValue().getValue();
                        fieldValues.put(name, content);
                    }
                }

                Map<String, HighlightField> highFields = hit.getHighlightFields();

                for (Map.Entry<String, HighlightField> entry : highFields.entrySet()) {
                    Text[] texts = entry.getValue().fragments();
                    String name = entry.getKey();

                    String content = "";
                    for (Text text : texts) {
                        content += text;
                    }

                    fieldValues.put(name, content);

                    String cleanValue = StringTools.escapeHtml(content.replaceAll("\\<[^>]+\\>", ""));
                    fieldValues.put("clean_" + name, cleanValue);
                }

                resultList.add(fieldValues);
            }

            result.setResultList(resultList);
        }

        return result;
    }

    /**
     * @param hit   记录行
     * @param field 字段
     * @return String
     * @Title: getHighlightFields
     * @Description: 获取带有关键字高亮的内容信息
     */
    public static String getHighlightFields(SearchHit hit, String field) {
        String content = "";
        if (hit != null) {
            Map<String, HighlightField> result = hit.highlightFields();
            HighlightField contentField = result.get(field);
            if (contentField != null) {
                Text[] contentTexts = contentField.fragments();
                for (Text text : contentTexts) {
                    content = text.toString();
                }
            } else {
                content = (String) hit.getSource().get(field);
            }
        }
        return content;
    }


}
