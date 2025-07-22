(function($) {
    // Tambahkan method langsung ke jQuery
    $.fn.getTotalChecked = function() {
        return this.find('.grid-checkbox-children:checked').length;
    };

    // Ambil array JSON dari semua row yang dicentang
    $.fn.getCheckedJsonData = function(...keysToInclude) {
        let result = [];

        this.each(function() {
            $(this).find('.grid-checkbox-children:checked').each(function() {
                const row = $(this).closest('tr.grid-row');
                const json = row.find('textarea').val();

                try {
                    const parsed = JSON.parse(json);

                    let cleaned;

                    if (keysToInclude.length > 0) {
                        // Ambil hanya key yang diminta
                        cleaned = {};
                        keysToInclude.forEach(k => {
                            if (parsed.hasOwnProperty(k)) {
                                cleaned[k] = parsed[k];
                            }
                        });
                    } else {
                        // Ambil semua key KECUALI "" dan yang diawali _temp
                        cleaned = Object.fromEntries(
                            Object.entries(parsed).filter(([key]) =>
                                key.trim() !== "" && !key.startsWith("_temp")
                            )
                        );
                    }

                    result.push(cleaned);
                } catch (e) {
                    console.warn('Data JSON tidak valid:', e);
                }
            });
        });

        return result;
    };

    $.fn.getAllJsonData = function(...keysToInclude) {
        let result = [];

        this.each(function() {
            $(this).find('tr.grid-row').each(function() {
                const json = $(this).find('textarea').val();

                try {
                    const parsed = JSON.parse(json);
                    let cleaned;

                    if (keysToInclude.length > 0) {
                        // Hanya ambil key yang disebutkan
                        cleaned = {};
                        keysToInclude.forEach(k => {
                            if (parsed.hasOwnProperty(k)) {
                                cleaned[k] = parsed[k];
                            }
                        });
                    } else {
                        // Ambil semua key KECUALI yang kosong atau diawali _temp
                        cleaned = Object.fromEntries(
                            Object.entries(parsed).filter(([key]) =>
                                key.trim() !== "" && !key.startsWith("_temp")
                            )
                        );
                    }

                    result.push(cleaned);
                } catch (e) {
                    console.warn('Data JSON tidak valid:', e);
                }
            });
        });

        return result;
    };

    $.fn.setData = function(dataArray) {
        return this.each(function() {
            const container = $(this);
            const table = container.find("> table");

            // Ambil key valid dari header grid
            const validKeys = [];
            table.find(".grid-row-template span.grid-cell").each(function() {
                const key = $(this).attr("column_key");
                if (key && key.trim() !== "") {
                    validKeys.push(key);
                }
            });

            // Kosongkan semua baris
            table.find(".grid-row").not(".grid-row-template").remove();

            dataArray.forEach((dataObj, index) => {
                const cleanedData = {};
                validKeys.forEach(key => {
                    if (dataObj.hasOwnProperty(key)) {
                        cleanedData[key] = dataObj[key];
                    }
                });

                const json = JSON.stringify(cleanedData);

                const template = table.find(".grid-row-template");
                const newRow = template.clone()
                    .removeClass("grid-row-template")
                    .addClass("grid-row")
                    .css("display", "");

                newRow.find(".grid-checkbox-children").prop("checked", false);

                // Gunakan fillValue dari plugin utama
                $.formGridCustom.fillValue(container, newRow, json);
                table.append(newRow);

                $.formGridCustom.updateRowIndex(newRow, index);
            });

            $.formGridCustom.disabledMoveAction(table);
            container.trigger("change");
            $.formGridCustom.showHidePlusIcon(container);
        });
    };

    $.fn.testFunc = function() {
        console.log($('#formDefKeys').val())
    }
})(jQuery);
