package com.syan.dev.lib;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.i18n.LocaleContextHolder;
import javax.servlet.http.HttpServletRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FormGridCustom extends Element implements FormBuilderPaletteElement, FormContainer, GridInnerDataRetriever {
    private final static String MESSAGE_PATH = "messages/FormGridCustom";

    protected Map<String, Map> headerMap;
    protected Map<String, Map> optionsMap;

    protected Form form;
    protected FormData formData;

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        this.formData = formData;

        // Template Form Grid
        String template = "FormGridCustom.ftl";

        String decoration = FormUtil.getElementValidatorDecoration(this, formData);
        dataModel.put("decoration", decoration);

        Map<String, Map> headers = getHeaderMap(formData);
//        dataModel.put("headers", headers);


        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    // Get Header for naming Header table
    protected Map<String, Map> getHeaderMap(FormData formData) {
        this.formData = formData;
        if (this.headerMap == null) {
            this.headerMap = new LinkedHashMap<String, Map>();

            // get property with name "options"
            Object optionProperty = getProperty("options");

            if (optionProperty != null && optionProperty instanceof Collection) {
                for (Map<String, String> optMap : (Collection<Map<String, String>>) optionProperty) {
                    Map<String, String> entry = new LinkedHashMap<>();

                    Object value = optMap.get("value");
                    Object label = optMap.get("label");

                    // Contoh masukkan ke headerMap:
                    if (value != null && label != null) {
                        entry.put("label", StringUtil.stripHtmlRelaxed(label.toString()));
                        this.headerMap.put(value.toString(), entry);
                    }

                    String formatType = optMap.get("formatType");
                    String format =  optMap.get("format");

                    LogUtil.info(this.getClassName(), "formatType: " + formatType + ", format: " + format);

                    if (format != null && formatType != null && "date".equalsIgnoreCase(formatType) && "UTC".equalsIgnoreCase(format)) {
                        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                        Locale locale = request.getLocale();
                        LogUtil.info(this.getClassName(), "Locale: " + locale.toString());
                        if (locale != null && locale.toString().startsWith("zh")) {
//                            WorkflowUtil.getHttpServletRequest().setAttribute("currentLocale", locale);
                            format = "yyyy-MM-dd";
                        } else {
                            SetupManager setupManager = (SetupManager)AppUtil.getApplicationContext().getBean("setupManager");
                            if ("true".equalsIgnoreCase(setupManager.getSettingValue("dateFormatFollowLocale"))) {
                                DateFormat dateInstance = DateFormat.getDateInstance(3, locale);

                                if (dateInstance instanceof SimpleDateFormat) {
                                    format = ((SimpleDateFormat)dateInstance).toPattern();
                                    format = format.replaceAll("MM", "M");
                                    format = format.replaceAll("M", "MM");
                                    format = format.replaceAll("dd", "d");
                                    format = format.replaceAll("d", "dd");
                                }
                            } else {
                                format = setupManager.getSettingValue("systemDateFormat");
                            }
                        }

                        if (format == null || format.isEmpty())
                            format = "MM/dd/yyyy";
                        if (!format.contains(":mm"))
                            format = format + " HH:mm";

                        entry.put("format", "UTC|" + format + "|" + LocaleContextHolder.getTimeZone().getRawOffset() + "|" + TimeZone.getDefault().getRawOffset());
                        continue;
                    }

                    if (formatType != null && (formatType.equals("file") || formatType.equals("image")) && format != null && !format.isEmpty()) {
                        AppService appService = (AppService)AppUtil.getApplicationContext().getBean("appService");
                        LogUtil.info(this.getClassName(), "tableName: " + appService.getFormTableName(AppUtil.getCurrentAppDefinition(), format));
                        entry.put("tableName", appService.getFormTableName(AppUtil.getCurrentAppDefinition(), format));
                    }
                }
            }
        }

        return this.headerMap;
    }

    protected Form getForm() {
        if (this.form == null) {
            String formDefId = getPropertyString("formDefId");
            if (formDefId.isEmpty()) {
                if (getStoreBinder() != null)
                    formDefId = ((FormBinder)getStoreBinder()).getPropertyString("formDefId");
                if (formDefId.isEmpty() && getLoadBinder() != null)
                    formDefId = ((FormBinder)getLoadBinder()).getPropertyString("formDefId");
            }
            LogUtil.info(this.getClassName(), formDefId);

            if (!formDefId.isEmpty()) {
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            }
        }

        return this.form;
    }

    @Override
    public String getFormBuilderCategory() {
        return "Syandev";
    }

    @Override
    public int getFormBuilderPosition() {
        return 1200;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-table\"></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>" + "Form Grid" + "</label><table cellspacing='0'><tr><th>" + "Header" + "</th><th>" + "Header" + "</th></tr><tr><td>" + "Cell" + "</td><td>" + "Cell" + "</td></tr></table>";
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("com.syan.dev.fromgridcustom.element.pluginLabel", getClassName(),  MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Form Grid Element Custom - Syandev";
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("com.syan.dev.fromgridcustom.element.pluginLabel", getClassName(),  MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String formDefField = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        if (appDef != null) {
            String formJsonUrl = "[CONTEXT_PATH]/web/json/console/app/" + appDef.getId() + "/" + appDef.getVersion() + "/forms/options";
            formDefField = "{name:'formDefId',label:'@@form.formgrid.formId@@',type:'selectbox',options_ajax:'" + formJsonUrl + "',required : 'True'}";
        } else {
            formDefField = "{name:'formDefId',label:'@@form.formgrid.formId@@',type:'textfield',required : 'True'}";
        }

        Object[] arguments = { formDefField };
        return AppUtil.readPluginResource(this.getClassName(), "/properties/FormGridCustom.json", arguments, true, MESSAGE_PATH);
    }

    @Override
    public Form getInnerForm() {
        return getForm();
    }

    @Override
    public FormData getFormData(FormRow formRow) {
        return null;
    }
}
