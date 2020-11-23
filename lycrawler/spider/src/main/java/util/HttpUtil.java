package util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Liu Ying
 */
public class HttpUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);
    private static final String UTF8 = "utf-8";

    private static final int TIME_OUT_MILLIS = 60000;
    private static String daili = "https://www.kuaidaili.com/getproxy/?orderid=902989670537257&num=100&area=&area_ex=&port=&port_ex=&ipstart=&ipstart_ex=&carrier=0&an_ha=1&an_an=1&sp1=1&protocol=1&method=1&quality=0&sort=0&b_pcchrome=1&b_pcie=1&b_pcff=1&showtype=1";
    private static Map map;
    private static Map<String, String> header = null;
    private static IpProxyUtil ipProxyList = new IpProxyUtil();

    static {
        map = new HashMap();
        map.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
        map.put("Cookie", "channelid=0; sid=1529907978275412; _ga=GA1.2.154333196.1529907741; Qs_lvt_161068=1529907740%2C1529976108; Qs_pv_161068=270419782304435520%2C3874460373527973400%2C3853836041513917000%2C2662933366421502000%2C4229558882744872400; Hm_lvt_7ed65b1cc4b810e9fd37959c9bb51b31=1529907741,1530862627,1531103585; _gid=GA1.2.1732216712.1531298846; sessionid=c4186310381b239c5b2dd53bcf0f4eda");
    }

    static {
        header = new HashMap();
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
    }

    /**
     * GET方式请求目标url, 返回字符串
     *
     * @param url
     * @return
     */
    public static String httpGet(String url, Map<String, String> headers) throws IOException {

        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setSocketTimeout(TIME_OUT_MILLIS).setConnectTimeout(TIME_OUT_MILLIS).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        // 创建HttpGet.
        HttpGet httpGet = new HttpGet(url);
        if (headers != null) {
            for (String header : headers.keySet()) {
                httpGet.setHeader(header, headers.get(header));
            }
        }
        httpGet.setConfig(requestConfig);
        String result = "";
        for (int i = 0; i < 5; i++) {
            // 执行get请求.
            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() == 200) {
                    result = EntityUtils.toString(response.getEntity(), UTF8);
                    break;
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                continue;
            } finally {
                if (null != response) {
                    response.close();
                }
            }
        }
        try {
            httpGet.releaseConnection();
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String httpGetWithProxyNew(String url, Map<String, String> headers, String proxy) throws IOException, HttpException {
        LOGGER.info("httpGetWithProxyNew proxy : " + proxy);
        RequestConfig requestConfig = RequestConfig.custom().setProxy(new HttpHost(proxy.split(":", 2)[0], Integer.parseInt(proxy.split(":", 2)[1]))).setCookieSpec(CookieSpecs.STANDARD).setSocketTimeout(TIME_OUT_MILLIS).setConnectTimeout(TIME_OUT_MILLIS).setConnectionRequestTimeout(TIME_OUT_MILLIS).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        // 创建HttpGet.
        HttpGet httpGet = new HttpGet(url);
        if (headers != null) {
            for (String header : headers.keySet()) {
                httpGet.setHeader(header, headers.get(header));
            }
        }
        httpGet.setConfig(requestConfig);
        String result = "";

        // 执行get请求.
        CloseableHttpResponse response = null;
        try {
            LOGGER.info("执行get请求");
            response = httpClient.execute(httpGet);
//            if (response.getStatusLine().getStatusCode() == 200) {
            LOGGER.info("get toString");
            result = EntityUtils.toString(response.getEntity(), UTF8);
//            }
        } catch (Exception e) {
            LOGGER.error("httpGetWithProxyNew err : " + e.getMessage() + " ; " + e.toString());
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                EntityUtils.consume(response.getEntity());
                response.close();
            }
            try {
                httpGet.releaseConnection();
                httpGet.abort();
                httpClient.close();
            } catch (IOException e) {
                LOGGER.error("IO err : " + e.getMessage());
            }
        }
        return result;
    }

    public static String httpGetWithProxy(String url, Map<String, String> headers, String proxy) throws IOException, HttpException {
        RequestConfig requestConfig = RequestConfig.custom().setProxy(new HttpHost(proxy.split(":", 2)[0], Integer.parseInt(proxy.split(":", 2)[1]))).setCookieSpec(CookieSpecs.STANDARD).setSocketTimeout(TIME_OUT_MILLIS).setConnectTimeout(TIME_OUT_MILLIS).build();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建HttpGet.
        HttpGet httpGet = new HttpGet(url);
        if (headers != null) {
            for (String header : headers.keySet()) {
                httpGet.setHeader(header, headers.get(header));
            }
        }
        httpGet.setConfig(requestConfig);
        String result = "";
        for (int i = 0; i < 2; i++) {
            // 执行get请求.
            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() == 200) {
                    result = EntityUtils.toString(response.getEntity(), UTF8);
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("httpGetWithProxy err : " + e.getMessage() + " ; " + e.toString());
                continue;
            } finally {
                if (response != null) {
                    response.close();
                }
                try {
                    httpGet.releaseConnection();
                    httpGet.abort();
                    httpClient.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
        return result;
    }

    public static String doPost(String url, String jsonstr) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        String result = "";
        try {
            httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            StringEntity se = new StringEntity(jsonstr, "utf-8");
            se.setContentType("text/json");
            se.setChunked(false);
            se.setContentEncoding("utf-8");
            httpPost.setEntity(se);
            response = httpClient.execute(httpPost);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return result;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (httpPost != null) {
                    httpPost.releaseConnection();
                    httpPost.abort();
                }
                httpClient.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return result;
    }

    public static String doPostMultipartFile(String url, MultipartFile file, String cardID, String cardType) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String result = "";
        try {
            String fileName = file.getOriginalFilename();
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.RFC6532);
            builder.addBinaryBody("file", file.getInputStream(), ContentType.DEFAULT_BINARY, fileName);
            // 文件流
            builder.addTextBody("cardID", cardID);
            builder.addTextBody("cardType", cardType);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            // 执行提交
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                // 将响应内容转换为字符串
                result = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("result" + result);
        return result;
    }


    public static Set<String> getProxys() {
        Set<String> proxys = new HashSet<>();
        for (int i = 0; i < i + 1; i++) {
            try {
                String content = httpGet(daili, map);
                Document document = Jsoup.parse(content);
                for (String s : document.getElementById("content").text().split("下载为文本")[1].split(" ")) {
                    if (StringUtils.isNotEmpty(s.replaceAll("\\s", "")) || s.contains(":")) {
                        proxys.add(s);
                    }
                }
                if (proxys.size() > 0) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return proxys;
    }

    public static Set<String> getProxysWithPost(String url, Map<String, String> headers, Map<String, String> postMap) {
        Set<String> proxys = new HashSet<>();
        for (int i = 0; i < i + 1; i++) {
            try {
                String content = postFormStr(url, headers, postMap);
                Document document = Jsoup.parse(content);
//                for (String s : document.getElementById("content").text().split("下载为文本")[1].split(" ")) {
//                    if (StringUtils.isNotEmpty(s.replaceAll("\\s", "")) || s.contains(":")) {
//                        proxys.add(s);
//                    }
//                }
                for (String s : document.select("#proxy_list").text().split(" ")) {
                    if (StringUtils.isNotEmpty(s.replaceAll("\\s", "")) || s.contains(":")) {
                        proxys.add(s);
                    }
                }
                if (proxys.size() > 0) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return proxys;
    }

    public static String postFormStr(String url, Map<String, String> headers, Map<String, String> postMap) {
        String result = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
        HttpPost httpPost = new HttpPost(url);

        if (headers != null && headers.size() > 0) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key));
            }
        }
        try {
            List<NameValuePair> formParams = new ArrayList<NameValuePair>();
            if (postMap != null && postMap.size() > 0) {
                for (String map : postMap.keySet()) {
                    formParams.add(new BasicNameValuePair(map, postMap.get(map)));
                }
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        for (int i = 0; i < 10; i++) {
            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    result = EntityUtils.toString(response.getEntity(), "utf-8");
                    if (StringUtils.isEmpty(result)) {
                        continue;
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            } finally {
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            httpPost.releaseConnection();
            httpPost.abort();
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    public static String httpURLConnectionGet(String url, Map<String, String> headers, String proxy) {
        String msg = null;
        HttpURLConnection connection = null;
        InputStream in = null;
        try {
            URL urlcon = new URL(url);
            InetSocketAddress addr = new InetSocketAddress(proxy.split(":", 2)[0], Integer.parseInt(proxy.split(":", 2)[1]));
            // http 代理
            Proxy proxycon = new Proxy(Proxy.Type.HTTP, addr);
            // 2. 得到网络访问对象java.net.HttpURLConnection
            connection = (HttpURLConnection) urlcon.openConnection(proxycon);
            connection.setDoOutput(false);
            // 设置是否向HttpURLConnection输出
            connection.setDoInput(true);
            // 设置是否从httpUrlConnection读入
            connection.setRequestMethod("GET");
            // 设置请求方式
            connection.setUseCaches(true);
            // 设置是否使用缓存
            connection.setInstanceFollowRedirects(true);
            // 设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
            connection.setConnectTimeout(TIME_OUT_MILLIS);
            // 设置超时时间
            connection.setReadTimeout(TIME_OUT_MILLIS);
            // 设置超时时间
            if (headers != null) {
                for (String header : headers.keySet()) {
                    connection.setRequestProperty(header, headers.get(header));
                    // 设置超时时间
                }
            }
            connection.connect();
            // 连接
            if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                // 5. 如果返回值正常，数据在网络中是以流的形式得到服务端返回的数据
                in = connection.getInputStream();
                msg = IOUtils.toString(in, "utf-8");
                return msg;
            }
//            in = connection.getInputStream();
//            msg = IOUtils.toString(in, "utf-8");
        } catch (Exception e) {
            LOGGER.error("httpURLConnectionGet err : " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (connection != null) {
                    connection.disconnect();
                    // 6. 断开连接，释放资源
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return msg;
    }

    public static String getRequest(String url, int timeOut) {
        String result = null;
        for (int i = 0; i < 10; i++) {
            URL u = null;
            try {
                u = new URL(url);
                if ("https".equalsIgnoreCase(u.getProtocol())) {
                    SslUtils.ignoreSsl();
                }
                URLConnection conn = u.openConnection();
                conn.setConnectTimeout(timeOut);
                conn.setReadTimeout(timeOut);
                result = IOUtils.toString(conn.getInputStream(),"UTF-8");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return result;
    }

    public static String httpGetwithJudgeWord(String url, String judgeWord) {
        try {
            String html = null;
            for (int i = 0; i < 5; i++) {
                if (null != url) {
                    html = HttpUtil.httpGet(url, header);
                }
                if (html != null && html.contains(judgeWord)) {
                    return html;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static Set<String> getProxy() {
        return ProxyDao.getProxyFromRedis();
    }

    public static String httpGetWithProxy(String url, String judgeWord) {
        String ipProxy = null;
        try {
            if (ipProxyList.isEmpty()) {
                LOGGER.info("ipProxyList is empty");
                Set<String> getProxy = getProxy();
                ipProxyList.addProxyIp(getProxy);
            }
            ipProxy = ipProxyList.getProxyIp();
            String html = null;
            for (int i = 0; i < 5; i++) {
                if (url != null && ipProxy != null) {
                    html = HttpUtil.httpGetWithProxy(url, header, ipProxy);
                }
                if (html != null && html.contains(judgeWord)) {
                    return html;
                }
                ipProxyList.removeProxyIpByOne(ipProxy);
                ProxyDao.delectProxyByOne(ipProxy);
                ipProxy = ipProxyList.getProxyIp();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static void main(String[] args) {

        String url = "https://www.traceparts.cn/zh/search/tracepartsfen-lei-ji-jie-ling-bu-jian-luo-xian-quan-dian-ci-tie-dian-ci-tie?CatalogPath=TRACEPARTS%3ATP01012001&PageNumber=1";

        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Origin", "https://www.traceparts.com");
        headers.put("Referer", url);
        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("accept-encoding", "gzip, deflate, br");
        Map<String, String> post = new HashMap<>();
        post.put("Keywords", "WithCAD");
        post.put("FilterOn-Content-Content-0-2-%5B%5D", "");
        post.put("GroupingMode", "1");
        post.put("SortingField", "2");
        String postData = "{\"Keywords\":\"\",\"FilterOn-Content-Content-0-2-%5B%5D\":\"WithCAD\",\"GroupingMode\":\"1\",\"SortingField\":\"2\"}";

        String s = HttpUtil.postFormStr(url, null, post);
        Document soup = Jsoup.parse(s);
        System.out.println(s);
    }

}
