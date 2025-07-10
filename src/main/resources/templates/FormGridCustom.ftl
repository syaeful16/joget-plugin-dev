<div class="form-cell full_width_field" ${elementMetaData!}>

<#if !(request.getAttribute("com.syan.dev.lib.FormGridCustom")??) >
    <script>
        console.log("FormGrid scripts loaded!");
    </script>
</#if>
<script type="text/javascript">
    $(document).ready(function() {
        var messages = {
            "form.listgrid.addRows": "@@form.listgrid.addRows@@",
            "form.formgrid.editRow": "@@form.formgrid.editRow@@",
            "form.formgrid.deleteRow": "@@form.formgrid.deleteRow@@",
            "form.formgrid.moveUp": "@@form.formgrid.moveUp@@",
            "form.formgrid.moveDown": "@@form.formgrid.moveDown@@",
            "form.formgrid.deleteMessage.value": "@@form.formgrid.deleteMessage.value@@",
            "form.formgrid.bulkDeleteMessage": "@@form.formgrid.bulkDeleteMessage@@"
        };

        $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").enterpriseformgrid({messages: messages, options : ${optionsJson!}});
        $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").enterpriseformgrid("initPopupDialog", {contextPath:'${request.contextPath}', title:'@@form.formgrid.addEntry@@' <#if requestParams??>, requestParams:${requestParams}</#if>});

        $("#formgrid_${elementParamName!}_${element.properties.elementUniqueKey!}").gridPaging({customSize: '${element.properties.pageSize!}' <#if element.properties.enableSorting! == 'true'>, dataSorting : true</#if>});

    })
</script>
</div>