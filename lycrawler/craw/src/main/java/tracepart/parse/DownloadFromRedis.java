package tracepart.parse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadFromRedis {
    private final static Logger LOGGER = LoggerFactory.getLogger(TracePartsUrlToRedis.class);
    private static Map<String, String> postData = new HashMap<>();
    private static String toCatchKey = "tracepartsToCatchUrl";
    private static String catchedKey = "tracepartsCatchedUrl";
    private static ESUtil esUtil = new ESUtil();
    private static SimpleDateFormat timestamp = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss ZZZ", Locale.US);
    private static SimpleDateFormat timestamp2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

    static {
        postData.put("Keywords", "");
        postData.put("Content%257C", "WithCAD");
        postData.put("GroupingMode", "1");
        postData.put("SortingField", "2");
    }

    public static void main(String[] args) {
        DownloadFromRedis downloadFromRedis = new DownloadFromRedis();
        downloadFromRedis.getUrlFromRedis();
    }

    private void SeedUrl() {
        try {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 120, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(40), new CustomRejectedExecutionHandler());
            String detailUrl = "";
            while (true) {
                if (RedisUtil.getUrlNumber(toCatchKey) > 0 && null != (detailUrl = RedisUtil.getUrlFromeSet(toCatchKey)) && !RedisUtil.isExist(catchedKey, detailUrl)) {
                    DownloadFromRedis.SeedTask seed = new DownloadFromRedis.SeedTask(detailUrl);
                    executor.execute(seed);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private class SeedTask implements Runnable {
        private String detailUrl;

        private SeedTask(String detailUrl) {
            this.detailUrl = detailUrl;
        }

        @Override
        public void run() {
            try {
                JSONObject tracepartInfo = new JSONObject();
                String tracepartDetail = HttpUtil.httpGet(detailUrl, null);
                if (!"".equals(tracepartDetail)) {
                    Document document = Jsoup.parse(tracepartDetail);
                    String product = "";
                    String partNumber = "";
                    for (String item : detailUrl.split("\\?")[1].split("&")) {
                        if (item.startsWith("Product")) {
                            product = item.split("=")[1];
                        } else if (item.startsWith("PartNumber")) {
                            partNumber = item.split("=")[1];
                        }
                    }
                    String id = MD5.getMD5String(product + ";" + partNumber);
                    tracepartInfo.put("id", id);
                    String cadId = MD5.getMD5String(product + ";" + partNumber + "zip");
                    tracepartInfo.put("cadId", cadId);
                    String picId = MD5.getMD5String(product + ";" + partNumber + "jpg");
                    tracepartInfo.put("picId", picId);
                    tracepartInfo.put("product", product);
                    tracepartInfo.put("partNumber", partNumber);
                    tracepartInfo.put("detailUrl", detailUrl);
                    String categoryList = document.select(".result-tb-container.flex-row.color-background-19 .flex-col").text().trim();
                    tracepartInfo.put("categoryList", categoryList);
                    String[] categorys = categoryList.split(">");
                    for (int i = 0; i < categorys.length; i++) {
                        tracepartInfo.put("category" + (i + 1), categorys[i]);
                    }
                    tracepartInfo.put("partName", document.select("#product-items div h1").text().trim());
                    Elements bomFieldsList = document.select(".bomfield-row.color-border-top-20.color-border-bottom-20");
                    JSONObject bomFieldsObj = new JSONObject();
                    for (Element e : bomFieldsList) {
                        bomFieldsObj.put(e.select(".bomfield-label.txt-elip.color-font-16").text().trim(), e.select(".bomfield-value.txt-elip").text().trim());
                    }
                    tracepartInfo.put("containerBOM", String.valueOf(bomFieldsObj));
                    tracepartInfo.put("supplierLogo", document.select("#supplier-container-informations div a img") == null ? "" : document.select("#supplier-container-informations div a img").attr("src"));
                    tracepartInfo.put("supplierName", document.getElementById("supplier-name") == null ? "" : document.getElementById("supplier-name").text());
                    tracepartInfo.put("supplierDesc", document.getElementById("supplier-description") == null ? "" : document.getElementById("supplier-description").text());
                    tracepartInfo.put("supplierWeb", document.getElementsByClass("gmap-supplier-website").size() == 0 ? "" : document.getElementsByClass("gmap-supplier-website").text());
                    tracepartInfo.put("supplierEmail", document.getElementsByClass("gmap-supplier-website").size() == 0 ? "" : document.getElementsByClass("gmap-supplier-email").text());
                    tracepartInfo.put("supplierAddress", document.select(".supplier-addr-hidden-details.flex-col span") == null ? "" : document.select(".supplier-addr-hidden-details.flex-col span").text().trim());
//                            String part_desc = (document.getElementById("description-container")) == null ? "" : document.getElementById("description-container").text();//商品描述
                    String simplePic = document.select("#overview-slider-picture img").attr("src");
                    tracepartInfo.put("simplePic", simplePic);
                    tracepartInfo.put("cadFiles", getCadFile(document));
                    tracepartInfo.put("crawler_time", timestamp.format(new Date()));
                    timestamp2.setTimeZone(TimeZone.getTimeZone("UTC"));
                    tracepartInfo.put("@timestamp", timestamp2.format(new Date()));
                    getCad(detailUrl, cadId);
                    getPic(simplePic, picId);
                    esUtil.writeToES(tracepartInfo, "bde_v3", "traceparts", id);
                    RedisUtil.insertUrlToSet(catchedKey, detailUrl);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
            LOGGER.info(detailUrl + " : 执行完毕");
        }
    }


    private void getUrlFromRedis() {
        try {
            while (true) {
                while (RedisUtil.getUrlNumber(toCatchKey) > 0) {
                    JSONObject tracepartInfo = new JSONObject();
                    String detailUrl = RedisUtil.getUrlFromeSet(toCatchKey);
                    if (null != detailUrl && !RedisUtil.isExist(catchedKey, detailUrl)) {
                        String tracepartDetail = HttpUtil.postFormStr(detailUrl, null, postData);
                        //String tracepartDetail = HttpUtil.httpGet(detailUrl, null);
                        if (!"".equals(tracepartDetail)) {
                            Document document = Jsoup.parse(tracepartDetail);
                            String product = "";
                            String partNumber = "";
                            for (String item : detailUrl.split("\\?")[1].split("&")) {
                                if (item.startsWith("Product")) {
                                    product = item.split("=")[1];
                                } else if (item.startsWith("PartNumber")) {
                                    partNumber = item.split("=")[1];
                                }
                            }
                            String id = MD5.getMD5String(product + ";" + partNumber);
                            tracepartInfo.put("id", id);
                            String cadId = MD5.getMD5String(product + ";" + partNumber + "zip");
                            tracepartInfo.put("cadId", cadId);
                            String picId = MD5.getMD5String(product + ";" + partNumber + "jpg");
                            tracepartInfo.put("picId", picId);
                            tracepartInfo.put("product", product);
                            tracepartInfo.put("partNumber", partNumber);
                            tracepartInfo.put("detailUrl", detailUrl);
                            String categoryList = document.select(".result-tb-container.flex-row.color-background-19 .flex-col").text().trim();
                            tracepartInfo.put("categoryList", categoryList);
                            String[] categorys = categoryList.split(">");
                            for (int i = 0; i < categorys.length; i++) {
                                tracepartInfo.put("category" + (i + 1), categorys[i]);
                            }
                            tracepartInfo.put("partName", document.select("#product-items div h1").text().trim());
                            Elements bomFieldsList = document.select(".bomfield-row.color-border-top-20.color-border-bottom-20");
                            JSONObject bomFieldsObj = new JSONObject();
                            for (Element e : bomFieldsList) {
                                bomFieldsObj.put(e.select(".bomfield-label.txt-elip.color-font-16").text().trim(), e.select(".bomfield-value.txt-elip").text().trim());
                            }
                            tracepartInfo.put("containerBOM", String.valueOf(bomFieldsObj));
                            tracepartInfo.put("supplierLogo", document.select("#supplier-container-informations div a img") == null ? "" : document.select("#supplier-container-informations div a img").attr("src"));
                            tracepartInfo.put("supplierName", document.getElementById("supplier-name") == null ? "" : document.getElementById("supplier-name").text());
                            tracepartInfo.put("supplierDesc", document.getElementById("supplier-description") == null ? "" : document.getElementById("supplier-description").text());
                            tracepartInfo.put("supplierWeb", document.getElementsByClass("gmap-supplier-website").size() == 0 ? "" : document.getElementsByClass("gmap-supplier-website").text());
                            tracepartInfo.put("supplierEmail", document.getElementsByClass("gmap-supplier-website").size() == 0 ? "" : document.getElementsByClass("gmap-supplier-email").text());
                            tracepartInfo.put("supplierAddress", document.select(".supplier-addr-hidden-details.flex-col span") == null ? "" : document.select(".supplier-addr-hidden-details.flex-col span").text().trim());
//                            String part_desc = (document.getElementById("description-container")) == null ? "" : document.getElementById("description-container").text();//商品描述
                            String simplePic = document.select("#overview-slider-picture img").attr("src");
                            tracepartInfo.put("simplePic", simplePic);
                            tracepartInfo.put("cadFiles", getCadFile(document));
                            tracepartInfo.put("crawler_time", timestamp.format(new Date()));
                            timestamp2.setTimeZone(TimeZone.getTimeZone("UTC"));
                            tracepartInfo.put("@timestamp", timestamp2.format(new Date()));
                            getCad(detailUrl, cadId);
                            getPic(simplePic, picId);
                            esUtil.writeToES(tracepartInfo, "bde_v4", "traceparts", id);
                            RedisUtil.insertUrlToSet(catchedKey, detailUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void getPic(String simplePic, String picId) {
        try {
            if (StringUtils.isNotEmpty(simplePic)) {
                DownloadFile.httpDownRealUrl(simplePic, null, picId, "jpg");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void getCad(String detailUrl, String cadId) {
        try {
            String product = "";
            for (String item : detailUrl.split("\\?")[1].split("&")) {
                if (item.startsWith("Product")) {
                    product = item;
                    break;
                }
            }
            String getUrl = "https://www.traceparts.cn/zh/api/viewer/3d/get-scene-file-path?" + product;
            String zipUrl = HttpUtil.httpGet(getUrl, null);
            DownloadFile.httpDownRealUrl(zipUrl, null, cadId, "zip");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private String getCadFile(Document document) {
        Element element = document.selectFirst("#cad-format-select");
        if (element != null) {
            try {
                Map<String, String> map = new HashMap<>();
                Elements elements = element.getElementsByTag("option");
                for (Element option : elements) {
                    if (option.text().contains("选择")) {
                        continue;
                    }
                    map.put(option.text(), option.attr("value") + "#" + option.attr("data-type"));
                }
                return String.valueOf(map);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return "";
    }

    private class CustomRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                // 核心改造点，由blockingqueue的offer改成put阻塞方法
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
