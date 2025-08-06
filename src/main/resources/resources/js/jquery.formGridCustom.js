(function( $ ){
    var contextPath;
    var messages;

    var methods = {

        init: function(args) {
            messages = args.messages;
            console.log(messages)

            return this.each(function(){
                var thisObj = $(this);
                $(this).find("table").data("options", args.options);

                if (jQuery.browser.msie && jQuery.browser.version === '9.0'){
                    var table = $(this).find("table");
                    var expr = new RegExp('>[ \t\r\n\v\f]*<', 'g');
                    var response_html_fixed = table.html().replace(expr, '><'); //A disgusting hack for ie9. Removes space between open and close <td> tags
                    $(table).html(response_html_fixed.trim());
                }

                $(this).find(".grid-row").each(function(rowIndex, row) {
                    console.log(rowIndex, row)
                    var json = $(row).find("textarea").val();
                    methods.decorateRow(row);
                    //methods.fillValue(row, json);
                    methods.updateRowIndex(row, rowIndex);
                });
                methods.disabledMoveAction($(this).find('table'));

                // add new row button
                if ($(this).find(".grid-action-add").length == 0) {
                    var link = $('<a class="grid-action-add" href="#" title="'+ messages['form.listgrid.addRows'] +'"><span>'+ messages['form.listgrid.addRows'] +'</span></a>');
                    link.click(function() {
                        var table = $(this).parent();
                        methods.add.apply(thisObj, arguments);
                        return false;
                    });
                    $(this).append(link);
                }
                methods.showHidePlusIcon(this);

                methods.bindDeleteEvents(this);
            });
        },

        getFrameId: function(id) {
            return "formGridFrame_" + id;
        },

        initPopupDialog: function(args){
            contextPath = args.contextPath;

            var frameId = methods.getFrameId($(this).attr("id"));

            var width = $(this).find('#width').val();
            var height = $(this).find('#height').val();
            $(this).data('requestParams', args.requestParams);

            JPopup.create(frameId, "", width, height);
        },

        add: function() {
            return this.each(function(){
                methods.popupForm.call(this, $(this).attr('id'), $(this).find('#formUrl').val(), $(this).find('#json').val(), $(this).find('#nonce').val(), $(this).attr('id')+"_add", "{}", "", $(this).find('#height').val(), $(this).find('#width').val());
            });
        },

        edit: function(){
            var row = $(this).closest("tr");
            var container = $(row).closest("table").parent();

            methods.popupForm.call(this, $(container).attr('id'), $(container).find('#formUrl').val(), $(container).find('#json').val(), $(container).find('#nonce').val(), $(container).attr('id')+"_edit", "{rowId:'"+$(row).attr('id')+"'}", $(row).find('textarea').val(), $(container).find('#height').val(), $(container).find('#width').val());
        },

        duplicate: function() {
            var row = $(this).closest("tr");
            var container = $(row).closest("table").parent();

            var funcName = $(container).attr('id')+"_duplicate";

            // Cek jika fungsi ada di global scope (window)
            if (typeof window[funcName] === 'function') {
                window[funcName].call(this); // panggil fungsi dengan konteks elemen
            } else {
                console.warn('Function', funcName, 'not found.');
            }
        },

        popupForm: function(id, url, json, nonce, callback, setting, value, height, width){
            if (value != undefined && value != '') {
                var datas = eval("(" + value + ")");
                if (datas.id) {
                    if (url.indexOf("?") != -1) {
                        url += "&";
                    } else {
                        url += "?";
                    }
                    url += "id=" + datas.id;
                }
            }

            if (url.indexOf("?") === -1) {
                url += "?";
            }
            url += UI.userviewThemeParams();
            if ($(this).data('requestParams') != undefined) {
                var params = FormUtil.getFieldsAsUrlQueryString($(this).data('requestParams'), $(this).closest("div.subform-container"));

                if (params !== "") {
                    url += "&" + params;
                }
            }

            var params = {
                _json : json,
                _callback : callback,
                _setting : setting,
                _jsonFormData : value,
                _nonce : nonce
            };

            var frameId = methods.getFrameId(id);
            JPopup.show(frameId, url, params, "", width, height);
        },

        duplicateRow: function(args){
            return $(this).each(function() {
                var row = $(this).closest("tr");
                var container = $(row).closest("table").parent();
                var showPopup = $(container).find('#popupDuplicate').val();

                // Ambil isi JSON dari textarea dalam row
                var json = $(row).find("textarea").val();
                if (!json) {
                    console.warn("Tidak ada data untuk diduplikasi.");
                    return;
                }

                var parsed;
                try {
                    parsed = JSON.parse(json);
                    parsed.id = syanUtils.generateUUID()
                } catch (e) {
                    console.error("JSON tidak valid:", e);
                    return;
                }

                // Hapus dari _tempRequestParamsMap juga
                if (parsed._tempRequestParamsMap && parsed._tempRequestParamsMap.id) {
                    delete parsed._tempRequestParamsMap.id;
                }

                // Contoh: ganti nilai kolom unik agar tidak gagal saat checkDuplicate
                var uniqueKey = $(container).find('#uniqueKey').val();
                if (uniqueKey && parsed[uniqueKey]) {
                    parsed[uniqueKey] += "_copy_" + Date.now(); // agar makin unik
                }

                var resultJson = JSON.stringify(parsed);

                if (showPopup === 'true' || uniqueKey !== '') {
                    // === MODE PAKAI POPUP ===
                    methods.popupForm.call(this,
                        $(container).attr('id'),
                        $(container).find('#formUrl').val(),
                        $(container).find('#json').val(),
                        $(container).find('#nonce').val(),
                        $(container).attr('id') + "_add", // callback
                        "{}",                              // setting kosong
                        resultJson,                        // data awal (prefilled)
                        $(container).find('#height').val(),
                        $(container).find('#width').val()
                    );
                } else {
                    var args = { result: resultJson };

                    if (!methods.checkDuplicate(container, args)) {
                        alert("Data duplikat ditemukan. Tidak bisa menambahkan row yang sama persis.");
                        return;
                    }

                    var table = $(container).find("> table");
                    var template = $(table).find(".grid-row-template");
                    var newRow = $(template).clone();

                    newRow.removeClass("grid-row-template");
                    newRow.css("display", "");
                    newRow.addClass("grid-row");

                    // Uncheck semua checkbox (jika ada)
                    newRow.find(".grid-checkbox-children").prop("checked", false);

                    // Isi value ke dalam row
                    methods.decorateRow(newRow);
                    methods.fillValue(container, newRow, resultJson);

                    // Append ke table
                    table.append(newRow);

                    // Hitung ulang index
                    var rowIndex = $(table).find("tr.grid-row").length - 1;
                    methods.updateRowIndex(newRow, rowIndex);
                    methods.disabledMoveAction($(newRow).closest("table"));

                    // Trigger event & update UI
                    $(container).trigger("change");
                    methods.showHidePlusIcon(container);
                }
            });
        },

        addRow: function(args){
            return $(this).each(function(){
                var frameId = methods.getFrameId($(this).attr('id'));

                if (methods.checkDuplicate(this, args) && !$(this).hasClass("readonly")) {
                    // get table
                    var table = $(this).find("> table");

                    // clone template
                    var template = $(table).find(".grid-row-template");
                    var newRow = $(template).clone();
                    newRow.removeClass("grid-row-template");
                    newRow.css('display','');
                    newRow.addClass("grid-row");

                    //Uncheck new row
                    newRow.find(".grid-checkbox-children").prop("checked", false);

                    methods.decorateRow(newRow);
                    methods.fillValue(this, newRow, args.result);

                    // append row
                    table.append(newRow);

                    // set input names and values
                    var rowIndex = $(table).find("tr.grid-row").length-1;
                    methods.updateRowIndex(newRow, rowIndex);
                    methods.disabledMoveAction($(newRow).closest("table"));

                    JPopup.hide(frameId);

                    // trigger change
                    $(this).trigger("change");
                    methods.showHidePlusIcon(this);
                } else {
                    JPopup.hide(frameId);
                }
            });
        },

        editRow: function(args){
            return $(this).each(function(){
                var frameId = methods.getFrameId($(this).attr('id'));

                if (methods.checkDuplicate(this, args) && !$(this).hasClass("readonly")) {
                    // get table
                    var table = $(this).find("table");
                    var row = $(table).find("#"+args.rowId);

                    methods.fillValue(this, row, args.result);
                    methods.updateAllRowIndex(table);
                    methods.disabledMoveAction(table);
                    JPopup.hide(frameId);

                    // trigger change
                    $(this).trigger("change");
                } else {
                    JPopup.hide(frameId);
                }
            });
        },

        checkDuplicate : function (container, args) {
            var okToInsert = true;
            var uniqueKey = $(container).find('#uniqueKey').val();
            if (uniqueKey && uniqueKey != null) {
                // find existing row
                var obj = eval("[" + args.result + "]");
                var uniqueVal = obj[0][uniqueKey];
                if (uniqueVal) {
                    $(container).find(".grid-cell[column_key=" + uniqueKey + "]").each(function() {
                        if (args.rowId && args.rowId != null) {
                            var row = $(this).closest("tr");
                            if ($(row).attr("id") == args.rowId) {
                                return true;
                            }
                        }
                        if ($.trim($(this).text()) == uniqueVal) {
                            okToInsert = false;
                            return false;
                        }
                    })
                }
            }
            return okToInsert;
        },

        showHidePlusIcon : function (container) {
            var row = $(container).find('#validateMaxRow').val();
            if (row && row != null) {
                var rowcount = $(container).find("tr").length - 2;
                if (rowcount >= parseInt(row)) {
                    $(container).find(".grid-action-add").hide();
                } else {
                    $(container).find(".grid-action-add").show();
                }
            }
        },
        deleteRow: function(args) {
            var row = $(this).closest("tr");
            var table = $(row).closest("table");
            var container = $(table).parent();
            var deleteMessage = $(container).find('#deleteMessage').val();

            if (deleteMessage == "") {
                deleteMessage = messages['form.formgrid.deleteMessage.value'];
            }

            //If args is true, it means the function is fired due to bulk deletion
            if (args === true || confirm(deleteMessage)) {
                row.remove();

                // reset input names
                methods.updateAllRowIndex(table);
                methods.disabledMoveAction(table);

                // trigger change
                var el = table.parent();
                $(el).trigger("change");
                methods.showHidePlusIcon(el);
            }
        },

        moveRowUp: function() {
            var currentRow = $(this).closest("tr");
            var prevRow = $(currentRow).prev();
            if(prevRow.attr("id") != "grid-row-template"){
                $(currentRow).after(prevRow);
                methods.updateAllRowIndex($(currentRow).closest("table"));
                methods.disabledMoveAction($(currentRow).closest("table"));
            }
        },

        moveRowDown: function() {
            var currentRow = $(this).closest("tr");
            var nextRow = $(currentRow).next();
            if(nextRow.length > 0){
                $(nextRow).after(currentRow);
                methods.updateAllRowIndex($(currentRow).closest("table"));
                methods.disabledMoveAction($(currentRow).closest("table"));
            }
        },

        disabledMoveAction: function(table) {
            $(table).find('a.grid-action-moveup').removeClass("disabled");
            $(table).find('a.grid-action-moveup:eq(0)').addClass("disabled");

            $(table).find('a.grid-action-movedown').removeClass("disabled");
            $(table).find('a.grid-action-movedown:last').addClass("disabled");
        },

        decorateRow: function(row) {
            console.log(row)
            var td = $('<td class="grid-action-cell"></td>');
            $(td).append('<a class="grid-action-duplicate far fa-copy" href="#" title="'+ messages['form.formgrid.duplicateRow'] +'"><span>'+ messages['form.formgrid.duplicateRow'] +'</span></a>');
            $(td).append('<a class="grid-action-edit" href="#" title="'+ messages['form.formgrid.editRow'] +'"><span>'+ messages['form.formgrid.editRow'] +'</span></a>');
            $(td).append('<a class="grid-action-delete" href="#" title="'+ messages['form.formgrid.deleteRow'] +'"><span>'+ messages['form.formgrid.deleteRow'] +'</span></a>');
            $(td).append('<a class="grid-action-moveup" href="#" title="'+ messages['form.formgrid.moveUp'] +'"><span>'+ messages['form.formgrid.moveUp'] +'</span></a>');
            $(td).append('<a class="grid-action-movedown" href="#" title="'+ messages['form.formgrid.moveDown'] +'"><span>'+ messages['form.formgrid.moveDown'] +'</span></a>');
            $(td).find('.grid-action-duplicate').click(function() {
                methods.duplicateRow.apply(this, arguments);
                return false;
            });
            $(td).find('.grid-action-edit').click(function() {
                methods.edit.apply(this, arguments);
                return false;
            });
            $(td).find('.grid-action-delete').click(function() {
                methods.deleteRow.apply(this, arguments);
                return false;
            });
            $(td).find('.grid-action-moveup').click(function() {
                methods.moveRowUp.apply(this, arguments);
                return false;
            });
            $(td).find('.grid-action-movedown').click(function() {
                methods.moveRowDown.apply(this, arguments);
                return false;
            });

            $(row).append(td);
        },

        fillValue: function(element, row, json) {
            var obj = eval("["+json+"]");
            $(row).find('span.grid-cell').each(function(){
                var column = $(this).attr("column_key");
                var type = $(this).attr("column_type");
                var format = $(this).attr("column_format");
                if (type != undefined && type == "html") {
                    $(this).html(obj[0][column]);
                } else if (type != undefined && type == "nl2br") {
                    $(this).html(UI.escapeHTML(obj[0][column]).replace(/(?:\r\n|\r|\n)/g, '<br />'));
                } else if (type != undefined && (type == "file" || type == "image") && format != undefined && format != "" && value != "") {
                    try{
                        var formDefId = format;
                        var recordId = obj[0]["id"];
                        var displayValue = "";
                        var values = obj[0][column].split(";");
                        var temp = [];
                        if (obj[0]["_tempFilePathMap"] !== undefined && obj[0]["_tempFilePathMap"] !== null && obj[0]["_tempFilePathMap"][column] !== undefined) {
                            temp = obj[0]["_tempFilePathMap"][column];
                        }
                        for (var v in values) {
                            var tempValue = values[v];
                            var tempFile = false;
                            if (temp.length > 0) {
                                for (var tf in temp) {
                                    if (temp[tf].indexOf("/" + tempValue, this.length - tempValue.length - 1) !== -1 || temp[tf].indexOf("\\" + tempValue, this.length - tempValue.length - 1) !== -1) {
                                        tempFile = true;
                                        break;
                                    }
                                }
                            }

                            if (recordId != undefined && recordId != "" && !tempFile && tempValue !== "") {
                                var appId = $(element).find('#appId').val();
                                var appVersion = $(element).find('#appVersion').val();
                                var contextPath = $(element).find('#contextPath').val();
                                var filePath = contextPath + "/web/client/app/" + appId + "/" + appVersion + "/form/download/" + formDefId + "/" + recordId + "/" + tempValue;
                                if (type == "image") {
                                    var imgfilePath = filePath + ".thumb.jpg.";
                                    filePath += ".";
                                    displayValue += "<a href=\""+filePath+"\" target=\"_blank\" ><img src=\""+imgfilePath+"\"/></a> ";
                                } else {
                                    filePath += ".?attachment=true";
                                    displayValue += "<a href=\""+filePath+"\" target=\"_blank\" >"+tempValue+"</a> ";
                                }
                            } else {
                                displayValue += tempValue + " ";
                            }
                        }
                        $(this).html(displayValue);
                    }catch(e){
                        $(this).text(value);
                    }
                } else {
                    var value = obj[0][column];
                    if (obj[0]["_decryptedDataMap"] !== undefined && obj[0]["_decryptedDataMap"][column] !== undefined) {
                        value = obj[0]["_decryptedDataMap"][column];
                    }
                    if (value === undefined) {
                        value = "";
                    }
                    if (type != undefined && type == "decimal" && format != undefined && format != ""  && (!isNaN(value) || value == "")) {
                        try{
                            if (value == "") {
                                value = "0";
                            }
                            value = parseFloat(value);
                            value = value.toFixed(parseInt(format));
                        }catch(e){}
                    } else if (type != undefined && type == "date" && format != undefined && format != "" && value != "") {
                        try{
                            if (format.indexOf("UTC") === 0) {
                                var dateFormat = format.split('|');
                                if (dateFormat.length === 4) {
                                    var d=new Date(value + " UTC");
                                    if (column === "dateCreated" || column === "dateModified") {
                                        d = new Date(d.getTime() + (d.getTimezoneOffset() * 60000) + (dateFormat[2] * 1) - (dateFormat[3] * 1));
                                    } else {
                                        d = new Date(d.getTime() + (d.getTimezoneOffset() * 60000) + (dateFormat[2] * 1));
                                    }
                                    value = formatDate(d ,methods.getDateFormat(dateFormat[1]));
                                }
                            } else {
                                var dateFormat = format.split('|');
                                if (dateFormat.length === 2) {
                                    var d = new Date(getDateFromFormat(value, methods.getDateFormat(dateFormat[0])));
                                    value = formatDate(d ,methods.getDateFormat(dateFormat[1]));
                                }
                            }
                        }catch(e){}
                    } else if (type != undefined && type == "options" && format != undefined && format != "" && value != "") {
                        try {
                            var options = $(element).find("table").data("options");
                            var values = value.split(";");
                            var temp = [];
                            if (options[column]['OPTIONS_AJAX_BINDER_DATA'] !== undefined) {
                                var co = options[column];
                                var ajaxOptions = co['OPTIONS'];
                                if ($.type(ajaxOptions) === "string") {
                                    ajaxOptions = {};
                                    var tempOpts = eval(co['OPTIONS']);
                                    for (var t in tempOpts) {
                                        ajaxOptions[tempOpts[t].value] = tempOpts[t].label;
                                    }
                                }
                                if (ajaxOptions[values[0]] !== undefined) {
                                    for (var v in values) {
                                        if (ajaxOptions[values[v]] !== undefined) {
                                            temp.push(ajaxOptions[values[v]]);
                                        } else {
                                            temp.push(values[v]);
                                        }
                                    }
                                } else {
                                    var elCell = this;
                                    var contextPath = $(element).find("#contextPath").val();
                                    var cv = obj[0][co['OPTIONS_AJAX_CONTROL_FIELD']];

                                    $.getJSON(contextPath + "/web/json/app/"+co['OPTIONS_AJAX_APP_ID']+"/"+co['OPTIONS_AJAX_APP_VERSION']+"/form/options",
                                        {
                                            _dv: cv,
                                            _n: co['OPTIONS_AJAX_NONCE'],
                                            _bd: co['OPTIONS_AJAX_BINDER_DATA']
                                        },
                                        function(data){
                                            for (var i=0, len=data.length; i < len; i++) {
                                                ajaxOptions[data[i].value] = data[i].label.trim();
                                            }

                                            for (var v in values) {
                                                if (ajaxOptions[values[v]] !== undefined) {
                                                    temp.push(ajaxOptions[values[v]]);
                                                } else {
                                                    temp.push(values[v]);
                                                }
                                            }

                                            $(element).find("table").data("options", options);

                                            value = temp.join(", ");

                                            $(elCell).text(value);
                                        }
                                    );
                                }
                            } else {
                                for (var v in values) {
                                    if (options[column] !== undefined && options[column][values[v]] !== undefined) {
                                        temp.push(options[column][values[v]]);
                                    } else {
                                        temp.push(values[v]);
                                    }
                                }
                            }
                            value = temp.join(", ");
                        } catch (e) {
                        }
                    }

                    //prevent unicode character double escaped
                    value = UI.escapeHTML(value);
                    value = value.replace(/&amp;([#0-9a-zA-Z]+;)/g, "&$1");

                    $(this).html(value);
                }
            });
            $(row).find('textarea').each(function(){
                $(this).val(json);
            });
        },

        getDateFormat: function(format) {
            if (format.indexOf("MMMMM") !== -1) {
                format = format.replace("MMMMM", "MMM");
            } else if (format.indexOf("MMM") !== -1) {
                format = format.replace("MMM", "NNN");
            }
            return format;
        },

        updateRowIndex: function(row, rowIndex){
            var id = $(row).closest("table").parent().attr('id');

            $(row).attr("id", id+"_row_"+rowIndex);
            $(row).find("textarea").each(function(){
                $(this).attr("name", $(this).attr("id")+"_"+rowIndex);
            });

            $(row).find("span.grid-cell.rowNumber").text(rowIndex + 1);

            //update row even/odd css class
            $(row).removeClass("odd");
            $(row).removeClass("even");
            if(rowIndex % 2 === 0){
                $(row).addClass("even");
            }else{
                $(row).addClass("odd");
            }
        },

        updateAllRowIndex: function(table) {
            $(table).find(".grid-row").each(function(rowIndex, row) {
                methods.updateRowIndex(row, rowIndex);
            });
        },

        refreshIndex: function() {
            return this.each(function(){
                var thisObj = $(this);
                var table = thisObj.find("table");

                methods.updateAllRowIndex(table);
                methods.disabledMoveAction(table);
            });
        },

        bindDeleteEvents : function (container) {
            if ($(container).hasClass("readonly") || $(container).hasClass("disabledDelete")) {
                return;
            }

            //initialize the selected rows
            $(container).data("selected_rows", []);

            //handle toggle all rows
            $(container).find(".grid-checkbox-parent").off('change').on('change', function(){
                if ($(this).is(":checked")) {
                    $(container).find(".grid-row .grid-checkbox-children").prop('checked', true).change();
                }else{
                    $(container).find(".grid-row .grid-checkbox-children").prop('checked', false).change();
                }
            });

            //handle bulk delete button clicked
            $(container).find(".delete_btn").off('click').on('click',function(){
                var deleteMessage = messages['form.formgrid.bulkDeleteMessage'];

                if (confirm(deleteMessage)) {
                    var arr = $(container).data("selected_rows");

                    arr.forEach(element => {
                        $(element).formGridCustom("deleteRow", true);
                    });

                    $(container).find(".grid-checkbox-parent").prop('checked', false);
                    //Resets array
                    $(container).data("selected_rows", []);
                }

                //Change delete button display to none
                if($(container).data("selected_rows").length === 0 && $(container).find(".delete_btn").is(':visible')){
                    $(container).find(".delete_btn").hide();
                }
                return false;
            });

            //handle checkboxes of each rows
            $(container).off('change.grid-row', ".grid-row .grid-checkbox-children")
                    .on('change.grid-row', ".grid-row .grid-checkbox-children", function(){
                var td = $(this).closest("td")[0];
                var arr = $(container).data("selected_rows");
                var index = arr.indexOf(td);

                if ($(td).closest("tr").hasClass("grid-row") && index === -1 && $(this).is(":checked")) {
                    arr.push(td);  //element is checked but not exist in selected rows
                } else if (!$(this).is(":checked") && index !== -1){
                    arr.splice(index, 1); //element is not checked but exist in selected rows
                }

                if (arr.length > 0 && !$(container).find(".delete_btn").is(':visible')){
                    $(container).find(".delete_btn").show();
                } else if(arr.length === 0 && $(container).find(".delete_btn").is(':visible')){
                    $(container).find(".delete_btn").hide();
                }
            });
        }
    };

    $.formGridCustom = {
        fillValue: methods.fillValue,
        updateRowIndex: methods.updateRowIndex,
        disabledMoveAction: methods.disabledMoveAction,
        showHidePlusIcon: methods.showHidePlusIcon,
        decorateRow: methods.decorateRow
    };

    $.fn.formGridCustom = function( method ) {
        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.formgrid' );
        }

    };

})( jQuery );

