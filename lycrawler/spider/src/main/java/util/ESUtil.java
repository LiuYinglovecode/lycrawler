package util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ESUtil.class);
    private static ESClient esClient = new ESClient();

    public boolean writeToES(JSONObject info, String index, String type, String id) {
        try {
//            String date = String.format("%tY", new Date()) + "." + String.format("%tm", new Date()) + "." + String.format("%td", new Date());
            return toES(info, index, type, id);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }


    private boolean toES(JSONObject info, String index, String type, String id) {
        try {
            TransportClient transportClient = esClient.getClient();
            transportClient
                    .prepareIndex(index, type)
                    .setSource(info, XContentType.JSON)
                    .setId(id)
                    .execute()
                    .actionGet();
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }
}
