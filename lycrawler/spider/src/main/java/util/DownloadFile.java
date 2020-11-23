package util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFile {
    private final static Logger LOGGER = LoggerFactory.getLogger(DownloadFile.class);


    public static String httpDownRealUrl(String realUrl, String cookie, String id, String type) {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(realUrl);
        if (StringUtils.isNotEmpty(cookie)) {
            httpGet.setHeader("Cookie", cookie);
        }
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getEntity() != null) {
                InputStream in = response.getEntity().getContent();
                MultipartFile multipartFile = new MockMultipartFile("multipartFile", "multipartFile", "", in);
                HttpUtil.doPostMultipartFile("http://106.74.18.111:9995/tpsearch/uploadCard", multipartFile, id, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpGet.releaseConnection();
                httpGet.abort();
                httpClient.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return realUrl;
    }

    public static String httpDownRealUrlWithProxy(String realUrl, String filePath, String md5, String cookie, String proxy) {
        String fileName = realUrl.substring(realUrl.lastIndexOf("/")).replace("/", "");
        if (fileName.contains(".")) {
            fileName = md5 + realUrl.substring(realUrl.lastIndexOf("."));
        } else {
            fileName = md5;
        }
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(realUrl);
        httpGet.setConfig(RequestConfig.custom().setProxy(new HttpHost(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))).build());
        httpGet.setHeader("Cookie", cookie);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            try {
                if (!fileName.contains(".")) {
                    fileName = fileName + "." + response.getHeaders("Content-type")[0].toString().split(";")[0].split("/")[1];
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            if (response.getEntity() != null) {
                InputStream in = response.getEntity().getContent();
                if (!filePath.endsWith("/")) {
                    filePath += "/";
                }
                OutputStream os = new FileOutputStream(filePath + fileName);
                int bytesRead = 0;
                byte[] buffer = new byte[81920];
                while ((bytesRead = in.read(buffer, 0, 81920)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();
                in.close();
                return "1," + fileName;
            }
            return "0,no content";
        } catch (Exception e) {
            e.printStackTrace();
            return "0," + e.toString();
        } finally {
            try {
                httpGet.releaseConnection();
                httpGet.abort();
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getRealUrl(String str) {
//        str = "http://www.tracepartsonline.net/PartsDefs/Production/MICHAUD_CHAILLY/10-22102008-081415/documents/DT-LCAT4.htm";
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(100000).setCookieSpec(CookieSpecs.STANDARD_STRICT).setConnectTimeout(100000).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        HttpGet httpGet = new HttpGet(str);
        httpGet.setConfig(requestConfig);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getEntity() != null) {
                String s = EntityUtils.toString(response.getEntity(), "utf-8").replaceAll("\\s", "").replaceAll("\u0000", "");
//                System.out.println(s);
                //    InputStream in=response.getEntity().getContent();
                if (s.contains("location.href")) {
                    String realurl = matchStr(s, "(ht|f)tp(s?)\\:\\/\\/[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(\\/?)([a-zA-Z0-9\\-\\.\\?\\,\\'\\/\\\\\\+%\\$#_]*)?");
                    if (StringUtils.isNotEmpty(realurl)) {
                        return realurl;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return str;
    }

    public static String matchStr(String s, String pattner) {
        Pattern r = Pattern.compile(pattner);
        Matcher m = r.matcher(s);
        if (m.find()) {
            return m.group(0);
        }
        return "";
    }

//    public static void main(String[] args) {
//        httpDownRealUrl("https://cdn.tracepartsonline.net/PartsDefs/Production/EMILE_MAURIN/10-06082008-155011/Documents/catalogue-gamme-inox-2012-pdf-9-1-mo-ESM-LCAT3.htm", "f:/123456/", "dsdsf", null);
//    }
}
