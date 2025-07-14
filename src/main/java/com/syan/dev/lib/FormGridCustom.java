package com.syan.dev.lib;

import com.syan.dev.services.FormGridCustomService;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.lib.Grid;
import org.joget.apps.form.lib.SelectBox;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class FormGridCustom extends Element implements FormBuilderPaletteElement, FormContainer, GridInnerDataRetriever {
    private final static String MESSAGE_PATH = "messages/FormGridCustom";

    protected Map<FormData, FormRowSet> cachedRowSet = new HashMap<>();
    protected Map<String, FormData> formDatas = new HashMap<>();

    protected Map<String, Map<String, String>> headerMap;
    protected Map<String, Map<String, String>> optionsMap;

    protected Map<String, String> optionFormsJson = new HashMap<>();

    protected Form form;
    protected FormData formData;

    private final FormGridCustomService formGridHeaderService = new FormGridCustomService();

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        this.formData = formData;

        // Template Form Grid
        String template = "FormGridCustom.ftl";

        String decoration = FormUtil.getElementValidatorDecoration(this, formData);
        dataModel.put("decoration", decoration);

        Map<String, Map<String, String>> headers = getHeaderMap(formData);
        dataModel.put("headers", headers);

        String optionsJson = this.getOptionsJson(headers, formData);
        LogUtil.info(this.getClassName(), "Options JSON : " + optionsJson);
        dataModel.put("optionsJson", optionsJson);

        FormRowSet rows = this.getRows(formData);
        if (rows != null && !rows.isEmpty()) {
            for (FormRow row : rows) {
                JSONObject json = new JSONObject(row);
                LogUtil.info(this.getClassName(), "Row JSON: " + json.toString());
            }
        } else {
            LogUtil.info(this.getClassName(), "No rows found.");
        }
        dataModel.put("rows", rows);

        String buttonLabel = "";
        if ("true".equals(getPropertyString("readonly"))) {
            buttonLabel = getPropertyString("submit-label-readonly");
            if (buttonLabel.isEmpty())
                buttonLabel = "Close";
        } else {
            buttonLabel = getPropertyString("submit-label-normal");
            if (buttonLabel.isEmpty())
                buttonLabel = "Submit";
        }
        dataModel.put("buttonLabel", buttonLabel);

        String json = getSelectedFormJson();
        dataModel.put("json", json);

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDef.getAppId());
        dataModel.put("appVersion", appDef.getVersion());

        Object requestParamsProperty = getProperty("requestParams");
        if (requestParamsProperty != null && requestParamsProperty instanceof Object[]) {
            StringBuilder requestJson = new StringBuilder("[");

            for (Object param : (Object[])requestParamsProperty) {
                Map paramMap = (Map)param;

                if (requestJson.length() > 1)
                    requestJson.append(",");

                requestJson.append("{");
                requestJson.append("param:'").append(paramMap.get("param")).append("',");
                requestJson.append("field:'").append(paramMap.get("field")).append("',");
                requestJson.append("defaultValue:'").append(paramMap.get("defaultValue")).append("'");
                requestJson.append("}");
            }

            requestJson.append("]");
            if (requestJson.length() > 2)
                dataModel.put("requestParams", requestJson.toString());
        }

        String nonceForm = SecurityUtil.generateNonce(new String[] { "EmbedForm", appDef.getAppId(), appDef.getVersion().toString(), json }, 1);
        dataModel.put("nonceForm", nonceForm);

        return FormUtil.generateElementHtml(this, formData, template, dataModel);
    }

    protected Map<String, Map<String, String>> getHeaderMap(FormData formData) {
        this.formData = formData;
        if (this.headerMap == null) {
            Object optionProperty = getProperty("options");

            if (optionProperty instanceof Collection) {
                this.headerMap = formGridHeaderService.getHeader((Collection<Map<String, String>>) optionProperty, formData);
            } else {
                this.headerMap = new LinkedHashMap<>();
            }
        }
        return this.headerMap;
    }

    protected String getOptionsJson(Map<String, Map<String, String>> headers, FormData formdata) {
        this.loadOptionsMap(headers, formdata);
        JSONObject jsonObject = new JSONObject();

        try {
            if (this.optionsMap != null && !this.optionsMap.isEmpty()) {
                for(String key : this.optionsMap.keySet()) {
                    Map<String, String> options = this.optionsMap.get(key);
                    key = key.substring(key.indexOf(".") + 1);
                    jsonObject.accumulate(key, options);
                }
            }
        } catch (Exception e) {
        }

        return jsonObject.toString();
    }

    protected void loadOptionsMap(Map<String, Map<String, String>> headers, FormData formData) {
        if (this.optionsMap == null) {
            this.optionsMap = new HashMap<>();

            for(String key : headers.keySet()) {
                Map<String, String> header = headers.get(key);

                String formatType = header.get("formatType");
                String formDefId = header.get("format");

                LogUtil.info(this.getClassName(), "Format Type : " + formatType + ", Form Def ID : " + formDefId);
                if ("options".equals(formatType) && formDefId != null && !formDefId.isEmpty()) {
                    String fieldId = header.get("value");

                    this.getFormOptions(formDefId, fieldId, true, formData);
                }
            }
        }

    }

    protected Map<String, String> getFormOptions(String formDefId, String fieldId, boolean useAjax, FormData gridFormData) {
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao)AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService)AppUtil.getApplicationContext().getBean("formService");

        String optionsKey = formDefId + "." + fieldId;
        LogUtil.info(this.getClassName(), "Options Key : " + optionsKey);
        Map<String, String> options = this.optionsMap.get(optionsKey);

        if (options != null) {
            return options;
        } else {
            LogUtil.info(this.getClassName(), "Options Kosong");
            options = new LinkedHashMap<>();
            String formJson = this.optionFormsJson.get(formDefId);

            if (formJson == null) {
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                if (appDef != null) {
                    FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
                    if (formDef != null) {
                        formJson = formDef.getJson();
                        this.optionFormsJson.put(formDefId, formJson);
                    }
                }
            }

            LogUtil.info(this.getClassName(), "formJson : " + formJson);

            if (formJson != null) {
                FormData formData = new FormData();
                Element element = FormUtil.findAndParseElement(formJson, fieldId);
                LogUtil.info(this.getClassName(), "Element : " + element.toString());
                LogUtil.info(this.getClassName(), "Element Parent : " + element.getParent());
                LogUtil.info(this.getClassName(), "Element Options Binder : " + element.getOptionsBinder());

                if (element != null) {
                    LogUtil.info(this.getClassName(), String.valueOf(FormUtil.isAjaxOptionsSupported(element, formData)));
                    if (useAjax && FormUtil.isAjaxOptionsSupported(element, formData)) {
                        LogUtil.info(this.getClassName(), "in useajax & form util true");
                        FormUtil.setAjaxOptionsElementProperties(element, formData);

                        options.put("OPTIONS_USE_AJAX", "true");
                        options.put("OPTIONS_AJAX_CONTROL_FIELD", element.getPropertyString("controlField"));
                        options.put("OPTIONS_AJAX_APP_ID", element.getPropertyString("appId"));
                        options.put("OPTIONS_AJAX_APP_VERSION", element.getProperty("appVersion").toString());
                        options.put("OPTIONS_AJAX_NONCE", element.getPropertyString("nonce"));
                        options.put("OPTIONS_AJAX_BINDER_DATA", element.getPropertyString("binderData"));


                        if (gridFormData != null) {
                            String[] fields = element.getPropertyString("controlField").split(";");
                            Set<String> values = new HashSet<>();

                            LogUtil.info(this.getClassName(), Arrays.toString(fields));

                            for (FormRow r : this.getRows(gridFormData)) {
                                LogUtil.info(this.getClassName(), r.toString());
                                List<String> v = new ArrayList<>();

                                for (String f : fields) {
                                    String field = f.trim();
                                    if (r.containsKey(field)) {
                                        v.add(r.getProperty(field));
                                    }
                                }

                                values.add(StringUtils.join(v, ";"));
                            }

                            Set<String> uniqueValues = new HashSet<>();

                            try {
                                FormAjaxOptionsBinder ab = (FormAjaxOptionsBinder)element.getOptionsBinder();
                                JSONArray jsonOptions = new JSONArray();

                                for(String v : values) {
                                    for(FormRow o : ab.loadAjaxOptions(v.split(";"))) {
                                        String ov = o.getProperty("value");
                                        if (!uniqueValues.contains(ov)) {
                                            Map<String, String> data = new HashMap<>();

                                            data.put("value", ov);
                                            data.put("label", o.getProperty("label"));

                                            jsonOptions.put(data);
                                            uniqueValues.add(ov);
                                        }
                                    }
                                }

                                options.put("OPTIONS", jsonOptions.toString());
                            } catch (Exception ex) {
                                LogUtil.error(FormService.class.getName(), ex, "Error retrieve options for " + optionsKey);
                            }
                        } else {
                            element.setProperty("controlField", "");

                            try {
                                formData = formService.executeFormOptionsBinders(element, formData);
                                Collection<Map> list = FormUtil.getElementPropertyOptionsMap(element, formData);
                                JSONArray jsonOptions = new JSONArray();

                                for(Map o : list) {
                                    Map<String, String> data = new HashMap<>();

                                    data.put("value", (String)o.get("value"));
                                    data.put("label", (String)o.get("label"));

                                    jsonOptions.put(data);
                                }

                                options.put("OPTIONS", jsonOptions.toString());
                            } catch (Exception ex) {
                                LogUtil.error(FormService.class.getName(), ex, "Error retrieve options for " + optionsKey);
                            }
                        }

                        this.optionsMap.put(optionsKey, options);

                    } else if (element instanceof SelectBox && element.getProperties().containsKey("listId") && !element.getProperties().containsKey("options")) {
                        FormRowSet rows = this.getRows(formData);
                        Collection<String> values = new ArrayList<>();

                        if (rows != null && !rows.isEmpty()) {
                            for(FormRow row : rows) {
                                String value = row.getProperty(fieldId);
                                if (value != null && !value.isEmpty()) {
                                    values.add(value);
                                }
                            }
                        }

                        this.retrieveDatalistOptions(values, element, options, formData);
                        this.optionsMap.put(optionsKey, options);
                    } else {
                        element.setProperty("controlField", "");

                        try {
                            formData = formService.executeFormOptionsBinders(element, formData);

                            for(Map o : FormUtil.getElementPropertyOptionsMap(element, formData)) {
                                options.put((String)o.get("value"), (String)o.get("label"));
                            }

                            this.optionsMap.put(optionsKey, options);
                        } catch (Exception ex) {
                            LogUtil.error(FormService.class.getName(), ex, "Error retrieve options for " + optionsKey);
                        }
                    }
                }
            }

            return this.optionsMap.get(optionsKey);
        }
    }

    protected void retrieveDatalistOptions(Collection<String> values, Element elment, Map<String, String> options, FormData formData) {
        DataListCollection rows = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao)AppUtil.getApplicationContext().getBean("datalistDefinitionDao");
        DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(elment.getPropertyString("listId"), appDef);

        LogUtil.info(this.getClassName(), "datalistDefinition : " + datalistDefinition.getJson());

        if (datalistDefinition != null) {
            String json = datalistDefinition.getJson();
            Object requestParamsProperty = elment.getProperty("requestParams");

            if (formData != null && requestParamsProperty != null && requestParamsProperty instanceof Object[]) {
                Form form = FormUtil.findRootForm(elment);

                for (Object param : (Object[])requestParamsProperty) {
                    Map paramMap = (Map)param;

                    String parameter = (String)paramMap.get("param");
                    String fieldId = (String)paramMap.get("field");
                    String defaultValue = (String)paramMap.get("defaultValue");

                    LogUtil.info(this.getClassName(), "Param : " + parameter + " fieldId : " + fieldId + " defaultValue : " + defaultValue);

                    String[] paramValues = null;
                    String paramValue = "";

                    if (fieldId != null && !fieldId.isEmpty()) {
                        Element field = FormUtil.findElement(fieldId, (Element)form, formData);
                        paramValues = FormUtil.getElementPropertyValues(field, formData);
                    }

                    if (paramValues == null || paramValues.length == 0)
                        paramValues = new String[] { defaultValue };

                    paramValue = FormUtil.generateElementPropertyValues(paramValues);
                    json = json.replaceAll(StringUtil.escapeRegex("#requestParam." + parameter + "#"), StringUtil.escapeRegex(paramValue));
                }
            }

            DataListService dataListService = (DataListService)AppUtil.getApplicationContext().getBean("dataListService");
            DataList dataList = dataListService.fromJson(json);
            DataListBinder binder = dataList.getBinder();

            String idField = elment.getPropertyString("idField");
            if (idField == null || idField.isEmpty()) {
                idField = binder.getPrimaryKeyColumnName();
            }

            if (binder != null) {
                DataListFilterQueryObject[] datalistFilter = dataList.getFilterQueryObjects();
                Collection<DataListFilterQueryObject> queries = new ArrayList<>(Arrays.asList(datalistFilter));

                if (values != null && !values.isEmpty()) {
                    DataListFilterQueryObject queryObject = new DataListFilterQueryObject();

                    StringBuilder query = new StringBuilder(binder.getColumnName(idField) + " in (");
                    for (String v : values) {
                        query.append("?,");
                    }

                    query = new StringBuilder(query.toString().replaceFirst(",$", ")"));
                    queryObject.setOperator("AND");
                    queryObject.setQuery(query.toString());
                    queryObject.setValues(values.toArray(new String[0]));
                    queries.add(queryObject);
                }
                rows = binder.getData(dataList, binder.getProperties(), queries.toArray(new DataListFilterQueryObject[0]), null, null, null, Integer.valueOf(100000));
            }

            if (rows != null && !rows.isEmpty()) {
                String displayField = elment.getPropertyString("displayField");

                if (idField != null && displayField != null) {
                    for (Object r : rows) {
                        options.put((String) DataListService.evaluateColumnValueFromRow(r, idField), (String) DataListService.evaluateColumnValueFromRow(r, displayField));
                    }
                }
            }
        }
    }

    protected FormRowSet getRows(FormData formData) {
        this.formData = formData;

        if (!this.cachedRowSet.containsKey(formData)) {
            String id = this.getPropertyString("id");
            String param = FormUtil.getElementParameterName(this);

            LogUtil.info(this.getClassName(), "id : " + id + ", param : " + param);

            FormRowSet rowSet = new FormRowSet();
            String json = this.getPropertyString("value");

            if (json != null && !json.isEmpty()) {
                try {
                    rowSet = FormUtil.jsonToFormRowSet(json);
                } catch (Exception ex) {
                    LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
                }
            }

            boolean hasSubmittedData = false;
            boolean continueLoop = true;

            for(int i = 0; continueLoop; ++i) {
                FormRow row = new FormRow();

                String paramName = param + "_jsonrow_" + i;
                String paramValue = formData.getRequestParameter(paramName);

                if (paramValue != null) {
                    row.setProperty("jsonrow", paramValue);

                    try {
                        row.putAll(this.convertJsonToFormRow(paramValue));
                    } catch (Exception ex) {
                        LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
                    }
                }

                if (!row.isEmpty()) {
                    if (i == 0) {
                        rowSet = new FormRowSet();
                    }

                    rowSet.add(row);
                    hasSubmittedData = true;
                } else {
                    continueLoop = false;
                }
            }

            if (!hasSubmittedData && formData.getRequestParameter(param + "_jsonrow") != null) {
                hasSubmittedData = true;
            }

            if (!FormUtil.isFormSubmitted(this, formData) || FormUtil.isReadonly(this, formData) || !hasSubmittedData) {
                FormRowSet binderRowSet = formData.getLoadBinderData(this);
                if (binderRowSet != null) {
                    if (!binderRowSet.isMultiRow()) {
                        if (!binderRowSet.isEmpty()) {
                            FormRow row = binderRowSet.get(0);
                            String jsonValue = row.getProperty(id);

                            try {
                                rowSet = FormUtil.jsonToFormRowSet(jsonValue);
                            } catch (Exception ex) {
                                LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
                            }
                        }
                    } else {
                        try {
                            rowSet = this.convertFormRowToJson(binderRowSet);
                        } catch (Exception ex) {
                            LogUtil.error(Grid.class.getName(), ex, "Error parsing grid JSON");
                        }
                    }

                    if (rowSet != null && this.getPropertyString("enableSorting") != null && this.getPropertyString("enableSorting").equals("true") && this.getPropertyString("sortField") != null && !this.getPropertyString("sortField").isEmpty()) {
                        final String sortField = this.getPropertyString("sortField");
                        Collections.sort(rowSet, new Comparator<FormRow>() {
                            public int compare(FormRow row1, FormRow row2) {
                                String number1 = row1.getProperty(sortField);
                                String number2 = row2.getProperty(sortField);
                                if (number1 != null && number2 != null) {
                                    try {
                                        return Integer.parseInt(number1) - Integer.parseInt(number2);
                                    } catch (Exception var6) {
                                    }
                                }

                                return 0;
                            }
                        });
                    }
                }
            }

            this.cachedRowSet.put(formData, rowSet);
        }

        this.prepareUiJsonData(formData, this.cachedRowSet.get(formData));
        return this.cachedRowSet.get(formData);
    }

    protected void prepareUiJsonData(FormData formData, FormRowSet oriRows) {
        if (oriRows != null && !oriRows.isEmpty()) {
            FormRow row = oriRows.get(0);
            if (!row.containsKey("jsonrow")) {
                FormRowSet rowSet = this.convertFormRowToJson(oriRows);
                this.cachedRowSet.put(formData, rowSet);
            }
        }

    }

    protected FormRowSet convertFormRowToJson(FormRowSet oriRowSet) throws JSONException {
        FormRowSet rowSet = new FormRowSet();
        rowSet.setMultiRow(true);

        for(FormRow row : oriRowSet) {
            JSONObject jsonObject = new JSONObject();
            FormRow newRow = new FormRow();


            LogUtil.info(this.getClassName(), row.toString());
            for(Map.Entry entry : row.entrySet()) {
                String key = (String)entry.getKey();
                String value = entry.getValue().toString();
                jsonObject.put(key, value);
                newRow.setProperty(key, value);
            }

            newRow.setProperty("jsonrow", jsonObject.toString());
            if (row.getDeleteFilePathMap() != null && !row.getDeleteFilePathMap().isEmpty()) {
                newRow.setDeleteFilePathMap(row.getDeleteFilePathMap());
            }

            if (row.getTempFilePathMap() != null && !row.getTempFilePathMap().isEmpty()) {
                newRow.setTempFilePathMap(row.getTempFilePathMap());
            }

            rowSet.add(newRow);
        }

        return rowSet;
    }

    protected FormRow convertJsonToFormRow(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        FormRow newRow = new FormRow();
        JSONArray fields = jsonObject.names();
        if (fields != null && fields.length() > 0) {
            for(int k = 0; k < fields.length(); ++k) {
                String fieldName = fields.getString(k);
                if (fieldName.equals("_tempFilePathMap")) {
                    JSONObject tempFilePathMap = jsonObject.getJSONObject("_tempFilePathMap");
                    JSONArray tempFilePaths = tempFilePathMap.names();
                    if (tempFilePaths != null && tempFilePaths.length() > 0) {
                        for(int l = 0; l < tempFilePaths.length(); ++l) {
                            List<String> pathValues = new ArrayList();
                            String tempFilePathFieldId = tempFilePaths.getString(l);
                            JSONArray paths = tempFilePathMap.getJSONArray(tempFilePathFieldId);
                            if (paths != null && paths.length() > 0) {
                                for(int m = 0; m < paths.length(); ++m) {
                                    pathValues.add(paths.getString(m));
                                }
                            }

                            newRow.putTempFilePath(tempFilePathFieldId, (String[])pathValues.toArray(new String[0]));
                        }
                    }
                } else if (fieldName.equals("_deleteFilePathMap")) {
                    JSONObject deleteFilePathMap = jsonObject.getJSONObject("_deleteFilePathMap");
                    JSONArray deleteFilePaths = deleteFilePathMap.names();
                    if (deleteFilePaths != null && deleteFilePaths.length() > 0) {
                        for(int l = 0; l < deleteFilePaths.length(); ++l) {
                            List<String> pathValues = new ArrayList();
                            String deleteFilePathFieldId = deleteFilePaths.getString(l);
                            JSONArray paths = deleteFilePathMap.getJSONArray(deleteFilePathFieldId);
                            if (paths != null && paths.length() > 0) {
                                for(int m = 0; m < paths.length(); ++m) {
                                    pathValues.add(paths.getString(m));
                                }
                            }

                            newRow.putDeleteFilePath(deleteFilePathFieldId, (String[])pathValues.toArray(new String[0]));
                        }
                    }
                } else if (!fieldName.equals("_tempRequestParamsMap")) {
                    String value = jsonObject.get(fieldName).toString();
                    newRow.setProperty(fieldName, value);
                } else {
                    FormData tempFormData = new FormData();
                    JSONObject tempRequestParamMap = jsonObject.getJSONObject("_tempRequestParamsMap");
                    JSONArray tempRequestParams = tempRequestParamMap.names();
                    if (tempRequestParams != null && tempRequestParams.length() > 0) {
                        for(int l = 0; l < tempRequestParams.length(); ++l) {
                            List<String> rpValues = new ArrayList();
                            String rpKey = tempRequestParams.get(l).toString();
                            JSONArray tempValues = tempRequestParamMap.getJSONArray(rpKey);
                            if (tempValues != null && tempValues.length() > 0) {
                                for(int m = 0; m < tempValues.length(); ++m) {
                                    rpValues.add(tempValues.get(m).toString());
                                }
                            }

                            tempFormData.addRequestParameterValues(rpKey, (String[])rpValues.toArray(new String[0]));
                        }
                    }

                    String uniqueKey = UuidGenerator.getInstance().getUuid();
                    newRow.put("__UNIQUEKEY__", uniqueKey);
                    this.formDatas.put(uniqueKey, tempFormData);
                }
            }
        }

        return newRow;
    }



    protected FormRowSet convertJsonToFormRowSet(FormRowSet oriRowSet) throws JSONException {
        FormRowSet rowSet = new FormRowSet();
        rowSet.setMultiRow(true);
        int i = 0;
        for (FormRow row : oriRowSet) {
            FormRow newRow = convertJsonToFormRow(row.get("jsonrow").toString());
            if (getPropertyString("enableSorting") != null && getPropertyString("enableSorting").equals("true") && getPropertyString("sortField") != null && !getPropertyString("sortField").isEmpty())
                newRow.put(getPropertyString("sortField"), Integer.toString(i));
            if (row.getDeleteFilePathMap() != null && !row.getDeleteFilePathMap().isEmpty())
                newRow.setDeleteFilePathMap(row.getDeleteFilePathMap());
            if (row.getTempFilePathMap() != null && !row.getTempFilePathMap().isEmpty())
                newRow.setTempFilePathMap(row.getTempFilePathMap());
            rowSet.add(newRow);
            i++;
        }
        return rowSet;
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
            if (!formDefId.isEmpty()) {
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();

                if (appDef != null) {
                    FormDefinitionDao formDefinitionDao = (FormDefinitionDao)AppUtil.getApplicationContext().getBean("formDefinitionDao");
                    FormService formService = (FormService)AppUtil.getApplicationContext().getBean("formService");
                    FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);

                    if (formDef != null) {
                        String json = formDef.getJson();
                        if (this.formData != null && this.formData.getProcessId() != null && !this.formData.getProcessId().isEmpty()) {
                            WorkflowManager wm = (WorkflowManager)AppUtil.getApplicationContext().getBean("workflowManager");
                            WorkflowAssignment wfAssignment = this.formData.getAssignment();
                            if (wfAssignment == null)
                                wfAssignment = wm.getAssignmentByProcess(this.formData.getProcessId());
                            json = AppUtil.processHashVariable(json, wfAssignment, "json", null);
                        }
                        this.form = (Form)formService.createElementFromJson(json);
                        Boolean readonly = "true".equalsIgnoreCase(getPropertyString("readonly"));
                        Boolean readonlyLabel = "true".equalsIgnoreCase(getPropertyString("readonlyLabel"));
                        if (readonly || readonlyLabel)
                            FormUtil.setReadOnlyProperty(this.form, readonly, readonlyLabel);
                    }
                }
            }
        }
        return this.form;
    }

    protected String getSelectedFormJson() {
        Form form = getForm();

        if (form != null)
            try {
                FormService formService = (FormService)AppUtil.getApplicationContext().getBean("formService");

                String json = formService.generateElementJson(form);
                try {
                    JSONObject temp = new JSONObject(json);
                    JSONObject jsonProps = temp.getJSONObject("properties");
                    JSONObject jsonLoadBinder = new JSONObject();

                    jsonLoadBinder.put("className", "org.joget.plugin.enterprise.JsonFormBinder");
                    jsonLoadBinder.put("properties", new JSONObject());

                    jsonProps.put("loadBinder", jsonLoadBinder);
                    jsonProps.put("storeBinder", jsonLoadBinder);
                    json = temp.toString();
                } catch (Exception exception) {

                }

                return SecurityUtil.encrypt(json);
            } catch (Exception e) {

            }

        setProperty("readonly", "true");
        return "";
    }

    @Override
    public String getFormBuilderCategory() {
        return "Syan Studio";
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
