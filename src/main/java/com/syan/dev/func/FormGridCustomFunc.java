package com.syan.dev.func;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.SetupManager;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormGridCustomFunc {
    public void processFormatType(Map<String, String> optMap) {
        String formatType = optMap.get("formatType");
        String format = optMap.get("format");

        if ("date".equalsIgnoreCase(formatType) && "UTC".equalsIgnoreCase(format)) {
            String newFormat = resolveDateFormat();
            int localOffset = ZonedDateTime.now(ZoneId.systemDefault()).getOffset().getTotalSeconds() * 1000;
            int defaultOffset = ZonedDateTime.now().getOffset().getTotalSeconds() * 1000;

            optMap.put("format", "UTC|" + newFormat + "|" + localOffset + "|" + defaultOffset);
        } else if ((formatType.equals("file") || formatType.equals("image")) && format != null && !format.isEmpty()) {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            optMap.put("tableName", appService.getFormTableName(AppUtil.getCurrentAppDefinition(), format));
        }
    }

    protected String resolveDateFormat() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        Locale locale = request.getLocale();

        if (locale != null && locale.toString().startsWith("zh")) {
            request.setAttribute("currentLocale", locale);
            return "yyyy-MM-dd";
        } else {
            SetupManager setupManager = (SetupManager) AppUtil.getApplicationContext().getBean("setupManager");
            if ("true".equalsIgnoreCase(setupManager.getSettingValue("dateFormatFollowLocale"))) {
                DateFormat dateInstance = DateFormat.getDateInstance(3, locale);
                if (dateInstance instanceof SimpleDateFormat) {
                    String pattern = ((SimpleDateFormat) dateInstance).toPattern();
                    return pattern.replaceAll("MM", "M").replaceAll("M", "MM").replaceAll("dd", "d").replaceAll("d", "dd");
                }
            }
            String systemFormat = setupManager.getSettingValue("systemDateFormat");
            return (systemFormat != null && !systemFormat.isEmpty()) ? systemFormat : "MM/dd/yyyy";
        }
    }
}
