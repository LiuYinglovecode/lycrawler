package com.ly.service;

import com.ly.core.config.ConfigClient;
import com.ly.mercury.utilities.common.HttpRequestUtils;
import com.ly.web.api.client.ApiClient;
import com.ly.web.api.client.ApiRequestBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Created by wangchong on 2018/4/19.
 */
@Service
public class BDEService {
    public JSONObject getTracePartList(String keyword, String cateName, int pageNum, int pageSize, int levelNum) throws UnsupportedEncodingException {
        String listUrl = ConfigClient.instance().get("app","part.list.url","http://106.74.18.111:9995/tpsearch/tracepartsByCategory");
        String pictureUrl = ConfigClient.instance().get("app","part.pic.url","http://106.74.18.111:9995/tpsearch/pic");
        JSONObject dataObj = new JSONObject();
        JSONArray resultArray = null;
        long totalAmount = 0;
        // String url = String.format(domain_bde + "/tpsearch/tracepartsByCategory?pageNum=%d&levelnum=%d&pageSize=%d&category=%s&keyword=%s", pageNum + 1, levelNum, pageSize, URLEncoder.encode(StringUtils.isEmpty(cateName) ? "all" : cateName, "utf-8"), URLEncoder.encode(StringUtils.isEmpty(keyword) ? "all" : keyword, "utf-8"));
        String url = String.format(listUrl+"?pageNum=%d&levelnum=%d&pageSize=%d&category=%s&keyword=%s", pageNum + 1, levelNum, pageSize, URLEncoder.encode(StringUtils.isEmpty(cateName) ? "all" : cateName, "utf-8"), URLEncoder.encode(StringUtils.isEmpty(keyword) ? "all" : keyword, "utf-8"));
        String result = HttpRequestUtils.httpGet(url);
        if (StringUtils.isNotEmpty(result)) {
            try {
                JSONObject resultObj = JSONObject.fromObject(result);
                int code = (int) resultObj.getOrDefault("code", -1);
                if (code == 0) {
                    resultArray = resultObj.getJSONArray("result");
                    if (null != resultArray && resultArray.size() > 0) {
                        String picUrl = pictureUrl+"?id=%s";
                        totalAmount = resultObj.getLong("totalAmount");
                        for (int i = 0; i < resultArray.size(); i++) {
                            JSONObject itemObj = resultArray.getJSONObject(i);
                            itemObj.put("picUrl", String.format(picUrl, URLEncoder.encode(itemObj.getString("picId"), "utf-8")));
                            try {
                                itemObj.put("encodeId", URLEncoder.encode(itemObj.getString("id"), "utf-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataObj.put("list", resultArray == null ? new JSONArray() : resultArray);
        dataObj.put("pageIndex", pageNum);
        dataObj.put("pageSize", pageSize);
        dataObj.put("totalAmount", totalAmount);
        return dataObj;
    }

    //获取tracePart列表
    public JSONArray searchTracePartList(String keyword, int pageSize) {
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        JSONArray resultArray = null;
        String url = String.format(domain_bde + "/tpsearch/traceparts?t=%s&num=%d", keyword == null ? "" : keyword, pageSize);
        String result = HttpRequestUtils.httpGet(url);
        if (StringUtils.isNotEmpty(result)) {
            try {
                JSONObject resultObj = JSONObject.fromObject(result);
                int code = (int) resultObj.getOrDefault("code", -1);
                if (code == 0) {
                    resultArray = resultObj.getJSONArray("result");
                    if (null != resultArray && resultArray.size() > 0) {
                        String picUrl = domain_bde + "/tpsearch/pic?id=%s";
                        for (int i = 0; i < resultArray.size(); i++) {
                            JSONObject itemObj = resultArray.getJSONObject(i);
                            itemObj.put("picUrl", String.format(picUrl, URLEncoder.encode(itemObj.getString("picId"), "utf-8")));
                            try {
                                itemObj.put("encodeId", URLEncoder.encode(itemObj.getString("id"), "utf-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultArray == null ? new JSONArray() : resultArray;
    }

    //获取tracePart详情
    public JSONObject searchTracePart(String id) {
        // String domain_bde = ConfigClient.instance().get("web", "domain.bde", "http://106.74.18.111:10002");
        String pictureUrl = ConfigClient.instance().get("app","part.pic.url","http://106.74.18.111:9995/tpsearch/pic");
        String ZIPUrl = ConfigClient.instance().get("app","part.zip.url","http://106.74.18.111:9995/tpsearch/card");
        String infoUrl = ConfigClient.instance().get("app","part.info.url","http://106.74.18.111:9995/tpsearch/info");
        String url = null;
        try {
            // url = String.format(domain_bde + "/tpsearch/info?id=%s", URLEncoder.encode(id,"utf-8"));
            url = String.format(infoUrl+"?id=%s", URLEncoder.encode(id,"utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String picUrl = pictureUrl+"?id=%s";
        String zipUrl = ZIPUrl+"?id=%s";
        String result = null;
        if (url != null && StringUtils.isNotEmpty(url)) {
            result = HttpRequestUtils.httpGet(url);
        }
        JSONObject dataObj = null;
        if (result != null && StringUtils.isNotEmpty(result)) {
            try {
                JSONObject resultObj = JSONObject.fromObject(result);
                int code = (int) resultObj.getOrDefault("code", -1);
                if (code == 0) {
                    dataObj = resultObj.getJSONObject("result");
                    dataObj.put("picUrl", String.format(picUrl, URLEncoder.encode(dataObj.getString("picId"),"utf-8")));
                    dataObj.put("zipUrl", String.format(zipUrl, URLEncoder.encode(dataObj.getString("cadId"), "utf-8")));
                    parseNullValue(dataObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dataObj == null ? new JSONObject() : dataObj;
    }

    //获取专利列表
    public JSONArray searchPatentList(String keyword, int pageSize) {
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        JSONArray resultArray = null;
        String url = String.format(domain_bde + "/psearch/patent?p=%s&num=%d", keyword == null ? "" : keyword, pageSize);
        String result = HttpRequestUtils.httpGet(url);
        if (StringUtils.isNotEmpty(result)) {
            try {
                JSONObject resultObj = JSONObject.fromObject(result);
                int code = (int) resultObj.getOrDefault("code", -1);
                if (code == 0) {
                    resultArray = resultObj.getJSONArray("result");
                    if (null != resultArray && resultArray.size() > 0) {
                        for (int i = 0; i < resultArray.size(); i++) {
                            JSONObject itemObj = resultArray.getJSONObject(i);
                            try {
                                itemObj.put("encodeId", URLEncoder.encode(itemObj.getString("id"), "utf-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultArray == null ? new JSONArray() : resultArray;
    }

    //获取专利详情
    public JSONObject searchPatent(String id) {
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = null;
        try {
            url = String.format(domain_bde + "/psearch/info?id=%s", URLEncoder.encode(id,"utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String result = null;
        if (url != null && StringUtils.isNotEmpty(url))
            result = HttpRequestUtils.httpGet(url);
        JSONObject dataObj = null;
        if (result != null && StringUtils.isNotEmpty(result)) {
            try {
                JSONObject resultObj = JSONObject.fromObject(result);
                int code = (int) resultObj.getOrDefault("code", -1);
                if (code == 0) {
                    dataObj = resultObj.getJSONObject("result");
                    parseNullValue(dataObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dataObj == null ? new JSONObject() : dataObj;
    }

    //获取关联产品需求数据
    public JSONObject searchRelatedProdBuyCates(String keyword, int pageSize) {
        JSONObject dataObj = new JSONObject();
        if (StringUtils.isNotEmpty(keyword)) {
            String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
            String url = null;
            try {
                url = String.format(domain_bde + "/reqsearch/similarNumByProduct?keyword=%s&num=%d", URLEncoder.encode(keyword, "utf-8"), pageSize);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String result = null;
            if (url != null && StringUtils.isNotEmpty(url)) {
                result = HttpRequestUtils.httpGet(url, 60000);
            }
            if (result != null && StringUtils.isNotEmpty(result)) {
                try {
                    JSONObject resultObj = JSONObject.fromObject(result);
                    int code = (int) resultObj.getOrDefault("code", -1);
                    if (code == 0) {
                        dataObj = resultObj.getJSONObject("result");
                        parseNullValue(dataObj);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return dataObj;
    }

    //获取关联产品需求数据
    public JSONObject searchRelatedCapBuyCates(String keyword, int pageSize) {
        JSONObject dataObj = null;
        if (StringUtils.isNotEmpty(keyword)) {
            String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
            String url = null;
            try {
                url = String.format(domain_bde + "/reqsearch/similarNumByCard?keyword=%s&num=%d", URLEncoder.encode(keyword, "utf-8"), pageSize);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String result = null;
            if (url != null && StringUtils.isNotEmpty(url))
                result = HttpRequestUtils.httpGet(url, 60000);
            if (result != null && StringUtils.isNotEmpty(result)) {
                try {
                    JSONObject resultObj = JSONObject.fromObject(result);
                    int code = (int) resultObj.getOrDefault("code", -1);
                    if (code == 0) {
                        dataObj = resultObj.getJSONObject("result");
                        parseNullValue(dataObj);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return dataObj;
    }

    private void parseNullValue(JSONObject itemObj) {
        if (null != itemObj && itemObj.containsValue("\"null\"")) {
            Iterator<Map.Entry> iterator = itemObj.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = iterator.next();
                String value = entry.getValue().toString();
                if ("\"null\"".equals(value)) {
                    entry.setValue("");
                }
            }
        }
    }

    public JSONObject getRelateEnterprise(String patentTitle) {
        // http://106.74.18.111:9995/comsearch/companyByPatentName?patentName=环形支撑式液压径向锻造机
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = domain_bde + "/comsearch/companyByPatentName?patentName="+patentTitle;
//        String url ="http://106.74.18.111:9995/comsearch/companyByPatentName?patentName=中继传输的方法及设备";
        String result = HttpRequestUtils.httpGet(url, 60000);
        JSONObject resultObj = new JSONObject();
        if (result != null)
            resultObj = JSONObject.fromObject(result);
        //JSONArray resArr = new JSONArray();
        JSONObject resObj = new JSONObject();
        if(resultObj.size() != 0 && resultObj.optJSONObject("result") != null && resultObj.optJSONObject("result").size()>0){
            resObj = resultObj.optJSONObject("result");
        }
        return resObj;
    }

    public JSONArray getRelatePatent(String patentTitle) {
        // http://106.74.18.111:9995/psearch/similarPatent?title=航天&num=10
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = domain_bde + "/psearch/similarPatent?title="+patentTitle+"&num=5";
//        String url = "http://106.74.18.111:9995/psearch/similarPatent?title=航天&num=5";
        String result = HttpRequestUtils.httpGet(url, 60000);
        JSONObject resultObj = new JSONObject();
        if (result != null)
            resultObj = JSONObject.fromObject(result);
        JSONArray resArr = new JSONArray();
        if(resultObj.size() != 0 && resultObj.optJSONArray("result") != null && resultObj.optJSONArray("result").size() != 0){
            resArr = resultObj.optJSONArray("result");
        }
        return resArr;
    }

    public JSONArray getRelateBuy(String id) {
        // http://106.74.18.111:9995/reqsearch/requirementByPatentID?patentId=2144AAAAAAADDQI=&num=10
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = null;
        try {
            url = domain_bde + "/reqsearch/requirementByPatentID?patentId="+ URLEncoder.encode(id, "utf-8")+"&num=5";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        String url =  "http://106.74.18.111:9995/reqsearch/requirementByPatentID?patentId=0089AAAAAAAWzcw=&num=5";
        String result = null;
        if (url != null && StringUtils.isNotEmpty(url)) {
            result = HttpRequestUtils.httpGet(url, 60000);
        }
        JSONObject resultObj = new JSONObject();
        if (result != null)
            resultObj = JSONObject.fromObject(result);
        JSONArray resArr = new JSONArray();
        if(resultObj.size() != 0 && resultObj.optJSONObject("result") != null && resultObj.optJSONObject("result").size() > 0){
            JSONObject json = resultObj.optJSONObject("result");
            resArr = jsonObj2JsonArr(json);
        }
        return resArr;
    }

    public JSONArray getRelateModel(String mainKeyword, int i, int i1) {
        // http://106.74.18.111:9995/tpsearch/tracepartsByProductName?pageNum=1&pageSize=10&productName=托利多SBC称重传感器
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = null;
        url = domain_bde + "/tpsearch/similarTraceparts?keyword="+mainKeyword+"&num=5";
        //url = "http://106.74.18.111:9995/tpsearch/similarTraceparts?keyword=螺母&num=5";
        String result = null;
        if (StringUtils.isNotEmpty(url))
            result = HttpRequestUtils.httpGet(url, 60000);
        JSONObject resultObj = new JSONObject();
        if (result != null)
            resultObj = JSONObject.fromObject(result);
        JSONArray resArr = new JSONArray();
        if(resultObj.size() != 0 && resultObj.optJSONArray("result") != null && resultObj.optJSONArray("result").size() != 0){
            JSONArray arr = resultObj.optJSONArray("result");
            for(Object obj:arr){
                JSONObject jsonObj = (JSONObject)obj;
                JSONObject json = new JSONObject();
                json.put("mainKeyword",jsonObj.optString("mainKeyword"));
                json.put("title",jsonObj.optString("partName"));
                String picUrl = domain_bde + "/tpsearch/pic?id=%s";
                try {
                    json.put("image", String.format(picUrl, URLEncoder.encode(jsonObj.optString("picId"),"utf-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                resArr.add(json);
            }
        }
        return resArr;
    }

    public JSONArray getRelateBuyModel(String id, int i) {
        // http://106.74.18.111:9995/reqsearch/requirementByTracepartID?tracepartId=2295AAAAAAAXZjg=&num=10
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = null;
        try {
            url = domain_bde + "/reqsearch/requirementByTracepartID?tracepartId="+URLEncoder.encode(id, "utf-8")+"&num=" + i;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        String url ="http://106.74.18.111:9995/reqsearch/requirementByTracepartID?tracepartId=8693AAAAAAAZQfU=&num=5";
        String result = null;
        if (url != null && StringUtils.isNotEmpty(url))
            result = HttpRequestUtils.httpGet(url, 60000);
        JSONObject resultObj = new JSONObject();
        if (result != null) {
            resultObj = JSONObject.fromObject(result);
        }
        JSONArray resArr = new JSONArray();
        if(resultObj.size() != 0 && resultObj.optJSONObject("result") != null && resultObj.optJSONObject("result").size()>0){
            JSONObject json = resultObj.getJSONObject("result");
            resArr = jsonObj2JsonArr(json);
        }
        return resArr;
    }

    /**
    * @Description: 此方法适用于key值不固定
    * @param
    * @return
    * @throws
    * @author Liankai Li
    * @date 2018/6/12 10:29
    */
    public static JSONArray jsonObj2JsonArr(JSONObject json){
        JSONArray resArr = new JSONArray();
        if(json == null)
            return resArr;
        Set<String > set = json.keySet();
        for(String key : set){
            JSONObject jb = new JSONObject();
            jb.put("title",key);
            jb.put("value",json.optInt(key));
            resArr.add(jb);
        }
        return resArr;
    }

    public JSONArray getRelatePatentModel(String id, int i) {
        // http://106.74.18.111:9995/psearch/patentByTracepartId?tracepartId=7599AAAAAAAAOGY=&num=10
        String domain_bde = ConfigClient.instance().get("bde", "domain.bde.url");
        String url = null;
        try {
            url = domain_bde + "/psearch/patentByTracepartId?tracepartId="+URLEncoder.encode(id, "utf-8")+"&num=" + i;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //String url = "http://106.74.18.111:9995/psearch/patentByTracepartId?tracepartId=8693AAAAAAAZQfU=&num=5";
        String result = null;
        if (url != null && StringUtils.isNotEmpty(url))
            result = HttpRequestUtils.httpGet(url, 60000);
        JSONObject resultObj = new JSONObject();
        if (result != null) {
            resultObj = JSONObject.fromObject(result);
        }
        JSONArray resArr = new JSONArray();
        if(resultObj.size() != 0 && resultObj.optJSONArray("result") != null && resultObj.optJSONArray("result").size() != 0){
            resArr = resultObj.optJSONArray("result");
        }
        return resArr;
    }

    public JSONArray getIndexBDEProduct(int type, int page, int pageSize){
        JSONArray result = new JSONArray();
        String url = ConfigClient.instance().get("bde","bde.search.product");
        String query = "product_type=" + type + "&page=" + page + "&page_size=" + pageSize + "&sort_field=&is_cm_product=2";
        String res = ApiClient.defaultInstance().request(ApiRequestBuilder.create().apiUrl(url).get(query));
        if(StringUtils.isNotEmpty(res)) {
            JSONObject jsonObject = JSONObject.fromObject(res);
            if (jsonObject != null && jsonObject.getInt("code") == 200 && jsonObject.get("data") != null && jsonObject.getString("data") != "null") {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data!= null && data.getString("data") != null && data.getString("data") != "null") {
                    JSONArray list = data.optJSONArray("data");
                    if(list != null && list.size() > 0){
                        for(int i = 0; i < list.size(); i++) {
                            JSONObject map = new JSONObject();
                            JSONObject sin = list.getJSONObject(i);
                            map.put("_id", sin.optLong("_id"));
                            map.put("title", sin.optString("title"));
                            map.put("refprice", sin.optDouble("price"));
                            result.add(map);
                        }
                    }
                }
            }
        }
        return result;
    }
}
