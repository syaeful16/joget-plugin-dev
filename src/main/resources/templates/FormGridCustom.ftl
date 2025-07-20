<link rel="stylesheet" type="text/css" href="${request.contextPath}/plugin/com.syan.dev.lib.FormGridCustom/css/formGridStyle.css" />

<div class="form-cell full_width_field" ${elementMetaData!}>
    <#if !(request.getAttribute("com.syan.dev.lib.FormGridCustom")??) >
        <script type="text/javascript" src="${request.contextPath}/plugin/com.syan.dev.lib.FormGridCustom/js/jquery.formGridCustom.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/com.syan.dev.lib.FormGridCustom/js/jquery.gridPagingCustom.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/com.syan.dev.lib.FormGridCustom/js/jquery.formGridCustom.checkboxTools.js"></script>
    </#if>
        <script type="text/javascript">
        $(document).ready(function() {
            var messages = {
                "form.formgrid.duplicateRow": "Duplicate row",
                "form.listgrid.addRows": "@@form.listgrid.addRows@@",
                "form.formgrid.editRow": "@@form.formgrid.editRow@@",
                "form.formgrid.deleteRow": "@@form.formgrid.deleteRow@@",
                "form.formgrid.moveUp": "@@form.formgrid.moveUp@@",
                "form.formgrid.moveDown": "@@form.formgrid.moveDown@@",
                "form.formgrid.deleteMessage.value": "@@form.formgrid.deleteMessage.value@@",
                "form.formgrid.bulkDeleteMessage": "@@form.formgrid.bulkDeleteMessage@@"
            };

            $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").formGridCustom({messages: messages, options : ${optionsJson!}});
            $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").formGridCustom("initPopupDialog", {contextPath:'${request.contextPath}', title:'@@form.formgrid.addEntry@@' <#if requestParams??>, requestParams:${requestParams}</#if>});

            $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").gridPagingCustom({customSize: '${element.properties.pageSize!}' <#if element.properties.enableSorting! == 'true'>, dataSorting : true</#if>});
        })

        function formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}_add(args){
            $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").formGridCustom("addRow", args);
        }

        function formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}_edit(args){
            $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").formGridCustom("editRow", args);
        }
    </script>
    <div style="display: flex; flex-direction: column;">
        <label field-tooltip="${elementParamName!}" class="label">${element.properties.label!} <span class="form-cell-validator">${decoration}${customDecorator!}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
    </div>
    <div class="form-clear"></div>
    <style>
        .td-checkbox .tablesaw-cell-label {
            display: none !important;
        }
    </style>
    <div id="formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}" name="${elementParamName!}" class="grid formgrid form-element <#if element.properties.readonly! == 'true'>readonly</#if> <#if element.properties.enableSorting! == 'true'>enableSorting</#if> <#if element.properties.disabledAdd! == 'true'>disabledAdd</#if> <#if element.properties.disabledDelete! == 'true'>disabledDelete</#if> <#if element.properties.disabledDuplicate! == 'true'>disabledDuplicate</#if>">
        <input type="hidden" disabled="disabled" id="formUrl" value="${request.contextPath}/web/app/${appId}/${appVersion}/form/embed?_submitButtonLabel=${buttonLabel!?html}">
        <input type="hidden" disabled="disabled" id="json" value="${json!}">
        <input type="hidden" disabled="disabled" id="appId" value="${appId!}">
        <input type="hidden" disabled="disabled" id="appVersion" value="${appVersion!}">
        <input type="hidden" disabled="disabled" id="contextPath" value="${request.contextPath}">
        <input type="hidden" disabled="disabled" id="height" value="${element.properties.height!}">
        <input type="hidden" disabled="disabled" id="width" value="${element.properties.width!}">
        <input type="hidden" disabled="disabled" id="uniqueKey" value="${element.properties.uniqueKey!}">
        <input type="hidden" disabled="disabled" id="validateMaxRow" value="${element.properties.validateMaxRow!}">
        <input type="hidden" disabled="disabled" id="deleteMessage" value="${element.properties.deleteMessage!?html}">
        <input type="hidden" disabled="disabled" id="nonce" value="${nonceForm!?html}">
        <input type="hidden" disabled="disabled" id="popupDuplicate" value="${element.properties.showPopupDuplicate!}">
        <#if (element.properties.disabledDelete! != 'true' && element.properties.readonly! != 'true') && element.properties.checkboxDisable! != 'true'>
            <button type="button" class="btn btn-outline-danger btn-sm delete_btn" id="${elementParamName!}_${element.properties.elementUniqueKey!}_delete" style="display:none;width:fit-content;border-color:#dc3545 !important;"><i class="fas fa-trash-alt"></i></button>
        </#if>
        <table cellspacing="0" style="width:100%;"  class="tablesaw tablesaw-stack" data-tablesaw-mode="stack">
            <thead>
                <tr>
                    <#if (element.properties.disabledDelete! != 'true' && element.properties.readonly! != 'true') && element.properties.checkboxDisable! != 'true'>
                        <th class="grid-checkbox-header">
                            <input type="checkbox" id="${elementParamName!}_grid-checkbox-parent" class="grid-checkbox-parent">
                        </th>
                    </#if>
                    <#if element.properties.showRowNumber?? && element.properties.showRowNumber! != "">
                        <th></th>
                    </#if>
                    <#list headers?keys as header>
                        <#assign width = "">
                        <#if headers[header]['width']?? && headers[header]['width'] != "">
                            <#assign width = "width:" + headers[header]['width'] >
                        </#if>
                        <th id="${elementParamName!}_${header?html}" style="${width}">${headers[header]['label']!}</th>
                    </#list>
                    <th class="grid-action-header"></th>
                </tr>
            </thead>
            <tbody>
                <tr id="grid-row-template" class="grid-row-template" style="display:none;">
                    <#if (element.properties.disabledDelete! != 'true' && element.properties.readonly! != 'true') && element.properties.checkboxDisable! != 'true'>
                        <td class="td-checkbox">
                            <input type="checkbox" id="${elementParamName!}_grid-checkbox" class="grid-checkbox-children">
                        </td>
                    </#if>
                    <#if element.properties.showRowNumber?? && element.properties.showRowNumber! != "">
                        <td><span class="grid-cell rowNumber"></span></td>
                    </#if>
                    <#list headers?keys as header>
                        <td><span id="${elementParamName!}_${header?html}"  name="${elementParamName!}_${header?html}" column_key="${header?html}" column_type="${headers[header]['formatType']!?html}" column_format="${headers[header]['format']!?html}" class="grid-cell"></span></td>
                    </#list>
                    <td style="display:none;"><textarea id="${elementParamName!}_jsonrow" name="${elementParamName!}_jsonrow"></textarea></td>
                </tr>
                <#list rows as row>
                    <tr class="grid-row" id="{elementParamName!}_row_${row_index}">
                        <#if element.properties.disabledDelete! != 'true' && element.properties.readonly! != 'true'>
                            <td class="td-checkbox">
                                <input type="checkbox" id="${elementParamName!}_grid-checkbox" class="grid-checkbox-children">
                            </td>
                        </#if>
                        <#if element.properties.showRowNumber?? && element.properties.showRowNumber! != "">
                            <td><span class="grid-cell rowNumber">${row_index + 1}</span></td>
                        </#if>
                        <#list headers?keys as header>
                            <td>
                                <span id="${elementParamName!}_${header?html}" name="${elementParamName!}_${header?html}" column_key="${header?html}" column_type="${headers[header]['formatType']!?html}" column_format="${headers[header]['format']!?html}" class="grid-cell">
                                    <#attempt>
                                        ${element.formatColumn(header, headers[header], row["id"], row[header], appId, appVersion, request.contextPath)}
                                        <#recover>
                                            ${row[header]!?html}
                                    </#attempt>
                                </span>
                            </td>
                        </#list>
                        <td style="display:none;">
                            <textarea id="${elementParamName!}_jsonrow" name="${elementParamName!}_jsonrow_${row_index}">${row['jsonrow']!?html}</textarea>
                        </td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>