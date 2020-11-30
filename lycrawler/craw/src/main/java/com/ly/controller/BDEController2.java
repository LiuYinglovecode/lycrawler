package com.ly.controller;

import com.ly.service.BDEService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by liuying on 2020/11/30.
 */
@Controller
@RequestMapping("/bde2")
public class BDEController2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(BDEController2.class);
    @Autowired
    private BDEService bdeService;

    private static final int PAGESIZE_TRACEPART = 20;
    private static final int LEVEL_NUM = 3;

    @RequestMapping("/openTracepart")
    public ModelAndView openTracepart() {
        ModelAndView mv = new ModelAndView("views_v2/bde/tracePartListOpen");
        return mv;
    }

    @RequestMapping("/tracepart/detail/{id}.html")
    public ModelAndView toTracePartDetailPage(@PathVariable String id) {
        ModelAndView mv = new ModelAndView("views_v2/bde/tracePartDetail");
        JSONObject obj = new JSONObject();
        try {
            obj = bdeService.searchTracePart(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mv.addObject("data", obj);
        return mv;
    }

    @RequestMapping(value = "/tracepart/list", produces = "application/json;charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public String listTraceParts(@RequestParam(required = false, defaultValue = "all", value = "q") String keyword,
                                 @RequestParam(required = false, defaultValue = "all", value = "cate") String cateName,
                                 @RequestParam(required = false, defaultValue = "0", value = "page") int pageNum) {
        JSONObject resultObj = new JSONObject();
        try {
            JSONObject dataObj = bdeService.getTracePartList(keyword, cateName, pageNum, PAGESIZE_TRACEPART, LEVEL_NUM);
            resultObj.put("msg", "");
            resultObj.put("code", 200);
            resultObj.put("data", dataObj);
        } catch (Exception e) {
            resultObj.put("msg", e.getMessage());
            resultObj.put("code", 500);
            resultObj.put("data", new JSONArray());
            e.printStackTrace();
        }
        return resultObj.toString();
    }
}
