package com.syan.dev.services;

import com.syan.dev.func.FormGridCustomFunc;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class FormGridCustomService {
    FormGridCustomFunc formGridCustomFunc = new FormGridCustomFunc();

    public Map<String, Map<String, String>> getHeader(Collection<Map<String, String>> options, FormData formData) {
        Map<String, Map<String, String>> headerMap = new LinkedHashMap<>();

        for (Map<String, String> optMap : options) {
            String value = optMap.get("value");
            String label = optMap.get("label");

            if (value != null) {
                if (label != null) {
                    optMap.put("label", StringUtil.stripHtmlRelaxed(label));
                }
                headerMap.put(value, optMap);
            }

            formGridCustomFunc.processFormatType(optMap);
        }

        return headerMap;
    }




}
