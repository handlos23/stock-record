package com.hxc.stockrecord.utils;

import com.alibaba.fastjson.JSONObject;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;
import java.util.Map;

/**
 * @Description:
 * @Author: huangxingchang
 * @Date: 2025-04-23
 * @Version: V1.0
 */
@Slf4j
public class StockUtils {
    private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_27);

    public static String getUrlParamByJson(JSONObject json) {
        StringBuilder result = new StringBuilder();
        if (json == null) {
            return result.toString();
        } else {
            for(Map.Entry<String, Object> entry : json.entrySet()) {
                if (result.length() == 0) {
                    result.append((String)entry.getKey()).append("=").append(entry.getValue());
                } else {
                    result.append("&").append((String)entry.getKey()).append("=").append(entry.getValue());
                }
            }

            return result.toString();
        }
    }

    public static String getTemplateText(String templateName, String templateContent, Map<String, Object> paramsMap) {
        StringWriter stringWriter = new StringWriter();
        try {
            Template template = new Template(templateName, templateContent, CONFIGURATION);
            template.process(paramsMap, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
