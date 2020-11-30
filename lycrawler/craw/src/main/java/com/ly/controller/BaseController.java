package com.ly.controller;

import com.ly.core.config.ConfigClient;
import com.ly.mercury.server.portal.common.MemoryCacheHelper;
import com.ly.mercury.server.portal.service.DataOpService;
import com.ly.mercury.utilities.common.*;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuying on 2020/11/30.
 */
public class BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);
    @Autowired
    protected HttpSession session;
    @Autowired
    protected HttpServletRequest request;
    @Autowired
    protected HttpServletResponse response;
    @Autowired
    protected DataOpService dataOpService;

    protected static Object _lockobj = new Object();
    protected static Date _lastUpdate;
    protected static long days = 30 * 60;
    protected static long modified = System.currentTimeMillis();


    /**
     * 基于@ExceptionHandler异常处理
     */
    @ExceptionHandler
    public String exp(HttpServletRequest request, Exception ex) {
        request.setAttribute("exception", ex);
        // 根据不同错误转向不同页面
        return "/common/500";
    }

    @ModelAttribute
    public void populateModel(Model model) {
        try {
            //静态资源配置
            String resourceUrl = ConfigClient.instance().get("app", "resource.urlprefix");
            String resourceVersion = ConfigClient.instance().get("web", "resource.version");

            String pageTitle = ConfigClient.instance().get("web", "title.casic", "航天云网，国家工业互联网平台");
            String homepage = ConfigClient.instance().get("web", "homepage", "/");

            String domain_buy = ConfigClient.instance().get("web", "domain.buy");
            model.addAttribute("domain_biz", ConfigClient.instance().get("app", "cas.clientUrl"));
            model.addAttribute("domain_buy", domain_buy);
            String domain_uc = ConfigClient.instance().get("web", "domain.uc", "http://uc.casicloud.com");
            String domain_cb = ConfigClient.instance().get("web", "domain.cb");

            //String domain_industry = ConfigClient.instance().get("web","domain.industry","http://industry.casicloud.com");
            model.addAttribute("Global_PageTitle", pageTitle);
            model.addAttribute("Global_Homepage", homepage);
            if ("true".equalsIgnoreCase(ConfigClient.instance().get("app", "resource.urlprefix.disable"))) {
                model.addAttribute("Global_ResourceDomain", "");
            } else {
                model.addAttribute("Global_ResourceDomain", resourceUrl);
            }
            model.addAttribute("resourceVersion", resourceVersion);
            boolean isMobile = WebTools.isMobile(request.getHeader("user-agent"));
            model.addAttribute("isMobile", isMobile);

            /**
             * uc 工作室 域名
             * 替换为 domain_uc domain_etpss
             */
            model.addAttribute("domain_uc", domain_uc);
            model.addAttribute("domain_cb", domain_cb);

            /**
             * ixinnuo 域名
             */
            model.addAttribute("domain_ixinnuo", ConfigClient.instance().get("biz", "domain.ixinnuo.url", "www.i-xinnuo.com"));

            /**
             * 项目域名变量控制：
             *             企业大脑    ${domain_edss}
             *             企业驾驶舱    ${domain_eoss}
             *             云端业务工作室   ${domain_etpss}
             *             云端应用工作室   ${domain_appstudio}
             *             云端营销   ${domain_cmapp}
             *             企业上云服务站（前台）${domain_enass}
             *             企业上云服务站（后台）${domain_enass_admin}
             *             中小企业服务站（原为小微企业服务站） ${domain_smbss}
             *             航天云网OA    ${casicoa_url}
             *             新业务门户    ${domain_biz}
             *
             */
            model.addAttribute("casicoa_url", ConfigClient.instance().get("biz", "domain.casicoa.url"));
            model.addAttribute("domain_etpss", ConfigClient.instance().get("biz", "domain.etpss.url"));
            model.addAttribute("domain_cmapp", ConfigClient.instance().get("biz", "domain.cmapp.url"));
            model.addAttribute("domain_eoss", ConfigClient.instance().get("biz", "domain.eoss.url"));
            model.addAttribute("domain_enass", ConfigClient.instance().get("biz", "domain.enass.url"));
            model.addAttribute("domain_enass_admin", ConfigClient.instance().get("biz", "domain.enass.admin.url"));
            model.addAttribute("domain_edss", ConfigClient.instance().get("biz", "domain.edss.url"));
            model.addAttribute("domain_smbss", ConfigClient.instance().get("web", "domain.smbss.url"));
            model.addAttribute("domain_appstudio", ConfigClient.instance().get("biz", "domain.appstudio.url"));
            model.addAttribute("domain_supply", ConfigClient.instance().get("biz", "domain.supply.url"));

            model.addAttribute("appid", ConfigClient.instance().get("biz", "appid"));
            model.addAttribute("yunluui_resource_domain", ConfigClient.instance().get("biz", "yunluui_resource_domain"));
            model.addAttribute("yunluui_resource_framework_version", ConfigClient.instance().get("biz", "yunluui_resource_framework_version"));
            model.addAttribute("yunluui_resource_version", ConfigClient.instance().get("biz", "yunluui_resource_version"));
            //消息推送微服务地址
            model.addAttribute("casicloudComMsgUrl", ConfigClient.instance().get("coin", "casicloud.com.msmsg"));
            //资讯推送微服务地址
            model.addAttribute("casicloudComMessagesUrl", ConfigClient.instance().get("coin", "casicloud.com.messages"));
            //列表页的热点,详情页的相关推荐
            model.addAttribute("recommendDocNewsUrl", ConfigClient.instance().get("bde", "recommend.docnews.hot"));
            //资讯推荐
            model.addAttribute("newsRecommend", ConfigClient.instance().get("bde", "bde.news.recommend"));
            //友情链接
            if (MemoryCacheHelper.getCache("friend") != null) {
                JSONArray friend = JSONArray.fromObject(MemoryCacheHelper.getCache("friend"));
                model.addAttribute("friendly", friend);
            }

            JSONArray headerTypical = (JSONArray) MemoryCacheHelper.getCache("headertypical");
            if (headerTypical != null && !headerTypical.isEmpty() && headerTypical.size() > 0) {
                model.addAttribute("headerTypical", headerTypical);
            }
            //新增热词
            if (MemoryCacheHelper.getCache("admin03_search_hotword") != null) {
                model.addAttribute("admin03_search_hotword", MemoryCacheHelper.getCache("admin03_search_hotword"));
            }
            model.addAttribute("relativeWord",ConfigClient.instance().get("bde","relative.word"));

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        //判断是否为内蒙古域名
        String domain = ConfigClient.instance().get("web", "domain.nmggyy", "nmggyy.com");
        if (request.getHeader("host").contains(domain)) {
            String nmgheader = dataOpService.getNMGHeader();
            String nmgfooter = dataOpService.getNMGFooter();
            if (StringUtils.isNotEmpty(nmgheader)) {
                model.addAttribute("header_nmggyy", nmgheader);
            }
            if (StringUtils.isNotEmpty(nmgfooter)) {
                model.addAttribute("footer_nmggyy", nmgfooter);
            }
        }
        model.addAttribute("Global_ViewRoot", "/WEB-INF/views");
    }

    public static boolean setResponseHeaderCache(HttpServletResponse response, long days, long modified) {
        /*String maxAgeDirective = "max-age=" + days;
        response.setHeader("Cache-Control", maxAgeDirective);
        response.setStatus(HttpServletResponse.SC_OK);
        response.addDateHeader("Last-Modified", modified);
        response.addDateHeader("Expires", System.currentTimeMillis() + days * 1000);*/
        return true;
    }

    @Deprecated
    public boolean checkPersonalUser() {
        long orgId = UserHelper.getCurrentOrgId(session);
        if (orgId > 0 && UserHelper.isPersonalUser(orgId)) {
            WebTools.redirect(response, "/workbench/personal.html");
            return true;
        }
        return false;
    }

    @Deprecated
    public boolean checkLogin() {
        if (!UserHelper.isLogin(session)) {
            try {
                String path = request.getRequestURI();
                String query = request.getQueryString();
                if (StringUtils.isNotEmpty(query))
                    path += "?" + query;
                response.sendRedirect("/login?ret=" + URLEncoder.encode(path, "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private static final String LOG_CLOUDMARKETING_DATASOURCE = ConfigClient.instance().get("cloudmarketing", "log.datasourceId", "200");

    /**
     * 日志上传简单封装
     *
     * @param
     * @param name   对象名称
     * @param type   对象类型
     * @param action 采用的操作
     * @param status 返回状态
     * @param msg    返回结果
     */
    @Deprecated
    public void cmLogUpload(String name, String type, int action, String status, String msg) {

        try {
            String userName = UserHelper.getCurrentUserName(session);
            long tenantId = UserHelper.getCurrentOrgId(session);
            long userId = UserHelper.getCurrentUserId(session);

            Map<String, String> data = new HashMap<>();
            data.put(LogParams.USER_NAME, userName);
            data.put(LogParams.USER_ID, String.valueOf(userId));
            data.put(LogParams.TARGET_ID, String.valueOf(tenantId));
            data.put(LogParams.TARGET_TYPE, type);
            data.put(LogParams.TARGET_NAME, name);
            data.put(LogParams.REAULT_CODE, status);
            data.put(LogParams.RESULT_MSG, msg);

            LogStat.uploadLog(userId, tenantId, "", action, data, LOG_CLOUDMARKETING_DATASOURCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}