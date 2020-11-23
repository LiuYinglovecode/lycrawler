package tracepart.parse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpUtil;
import util.RedisUtil;


import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TracePartsUrlToRedis {
    private final static Logger LOGGER = LoggerFactory.getLogger(TracePartsUrlToRedis.class);
    private static Map<String, String> postData = new HashMap<>();
    private static String url = "https://www.traceparts.cn/zh/search/tracepartsfen-lei?CatalogPath=TRACEPARTS%3ATRACEPARTS";
    private static String tracepartsBaseUrl = "https://www.traceparts.cn";
    private static String toCatchKey = "tracepartsToCatchUrl";
    private static String catchedKey = "tracepartsCatchedUrl";


    static {
        postData.put("Keywords", "");
        postData.put("Content%257C", "WithCAD");
        postData.put("GroupingMode", "1");
        postData.put("SortingField", "2");
    }

    public static void main(String[] args) {
        TracePartsUrlToRedis urlToRedis = new TracePartsUrlToRedis();
        urlToRedis.getUrlToRedis();
    }

    private void SeedUrl() {
        try {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 120, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(15), new ThreadPoolExecutor.CallerRunsPolicy());
            for (int i = 0; i < 10; i++) {
                SeedTask seedTask = new SeedTask(url);
                executor.execute(seedTask);
            }
            executor.shutdown();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private class SeedTask implements Runnable {
        private String url;

        private SeedTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                String content = HttpUtil.httpGet(url, null);
                Document document = Jsoup.parse(content);
                Elements elements = document.select("div.flex-row.color-font-16.result-filter-item.treeview-node-child");
                PrintStream outparse;
                try {
                    outparse = new PrintStream("G://sssss.txt");
                    System.setOut(outparse);
                    System.out.println(document);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                for (Element e : elements) {
                    String href = tracepartsBaseUrl + e.getElementsByTag("a").get(0).attr("href");
                    getListEveryPage(href);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private void getUrlToRedis() {
        try {
            String content = HttpUtil.httpGet(url, null);
            Document document = Jsoup.parse(content);
//          Elements elements = document.select("div.txt-ellipsis > ul#dropdownMenu_CategoryChildren > li > a");
            Elements temp = document.select("div.flex-row.color-font-16.result-filter-item.treeview-node-child");
            Elements temp2 = temp.select("span.treeview-node-title");
            Elements elements = temp2.select("a");
            PrintStream outparse;
            try {
                outparse = new PrintStream("G://span-a.html");
                System.setOut(outparse);
                System.out.println(elements);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            for (Element e : elements) {
                String href = tracepartsBaseUrl + e.attr("href");
                getListEveryPage(href);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * 每页数据
     *
     * @param href
     * @param
     */
    private static void getListEveryPage(String href) {
        for (int i = 1; i < i + 1; i++) {
            Elements result = null;
            for (int x = 0; x < 5; x++) {
                String newContent = HttpUtil.postFormStr(href + "&PageNumber=" + i, null,postData);
                PrintStream outparse3;
                try {
                    outparse3 = new PrintStream("G://newContent.html");
                    System.setOut(outparse3);
                    System.out.println(newContent);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
//                result = Jsoup.parse(newContent).select(".result-block");
                result = Jsoup.parse(newContent).select(".result-block.color-border-bottom-5.flex-row");
                PrintStream outparse4;
                try {
                    outparse4 = new PrintStream("G://crawlerout//result" + i + x + ".html");
                    System.setOut(outparse4);
                    System.out.println(result);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (result.size() > 0) {
                    break;
                }
            }
            if (result.size() == 0) {
                break;
            }
            for (Element res : result) {
                getItemLine(res);
            }
        }
    }

    /**
     * 处理列表页单条数据
     *
     * @param res
     * @param
     */
    private static void getItemLine(Element res) {
        try {
            String detailUrl = tracepartsBaseUrl + res.getElementsByTag("a").get(0).attr("href");
            if (!RedisUtil.isExist(catchedKey, detailUrl)) {
                RedisUtil.insertUrlToSet(toCatchKey, detailUrl);
                System.out.println("success!");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
