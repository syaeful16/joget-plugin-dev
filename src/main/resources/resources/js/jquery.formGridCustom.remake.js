(function($) {
    // Tambahkan method langsung ke jQuery
    $.fn.getTotalChecked = function() {
        return this.find('.grid-checkbox-children:checked').length;
    };

    // Ambil array JSON dari semua row yang dicentang
    $.fn.getCheckedJsonData = function(...keysToInclude) {
        let result = [];

        $(this).find('.grid-checkbox-children:checked').each(function() {
            const row = $(this).closest('tr.grid-row');

            // 🚫 Skip jika baris sedang disembunyikan
            if (!row.is(':visible')) {
                return;
            }

            const json = row.find('textarea').val();

            try {
                const parsed = JSON.parse(json);
                let cleaned;

                if (keysToInclude.length > 0) {
                    cleaned = {};
                    keysToInclude.forEach(k => {
                        if (parsed.hasOwnProperty(k)) {
                            cleaned[k] = parsed[k];
                        }
                    });
                } else {
                    cleaned = Object.fromEntries(
                        Object.entries(parsed).filter(([key]) =>
                            key.trim() !== "" && !key.startsWith("_temp")
                        )
                    );
                }

                result.push(cleaned);
            } catch (e) {
                console.warn('Data JSON not valid:', e);
            }
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
                    console.warn('Data JSON not valid:', e);
                }
            });
        });

        return result;
    };

    $.fn.setData = function(dataArray) {
        const formDefKeys = $('#formDefKeys').val()?.split(',') || [];

        return this.each(function() {
            const container = $(this);
            const table = container.find("> table");
            const template = table.find(".grid-row-template");

            // Bersihkan baris yang bukan template
            table.find(".grid-row").not(".grid-row-template").remove();

            dataArray.forEach((dataObj, index) => {
                // Tambahkan atau validasi ID
                if (!dataObj.id || !syanUtils.isValidUUID(dataObj.id)) {
                    dataObj.id = syanUtils.generateUUID();
                }

                const rowData = { id: dataObj.id }; // Tambahkan id secara eksplisit

                // Ambil hanya key yang sesuai dengan formDefKeys
                formDefKeys.forEach(key => {
                    if (dataObj.hasOwnProperty(key)) {
                        rowData[key] = dataObj[key];
                    }
                });

                const newRow = template.clone()
                    .removeClass("grid-row-template")
                    .addClass("grid-row")
                    .show();

                newRow.find(".grid-checkbox-children").prop("checked", false);

                $.formGridCustom.decorateRow(newRow);

                $.formGridCustom.fillValue(container, newRow, JSON.stringify(rowData));
                table.append(newRow);

                $.formGridCustom.updateRowIndex(newRow, index);
            });

            $.formGridCustom.disabledMoveAction(table);
            $.formGridCustom.showHidePlusIcon(container);
            container.trigger("change");
        });
    };

    $.fn.updateRowData = function(updateFn) {
        return this.each(function() {
            const container = $(this);
            const table = container.find("> table");

            container.find("tr.grid-row").each(function(rowIndex) {
                const $textarea = $(this).find("textarea");
                try {
                    let data = JSON.parse($textarea.val());

                    // Jalankan callback untuk update
                    updateFn(data, $(this));

                    // Simpan kembali ke textarea
                    const newJson = JSON.stringify(data);
                    $textarea.val(newJson);

                    // Refresh tampilan cell sesuai JSON
                    $.formGridCustom.fillValue(container, $(this), newJson);

                    // Update index dan styling row
                    $.formGridCustom.updateRowIndex($(this), rowIndex);
                } catch (e) {
                    console.warn("Failed to process JSON on line: ", e);
                }
            });

            // Pastikan tombol move dan plus icon sinkron
            $.formGridCustom.disabledMoveAction(table);
            $.formGridCustom.showHidePlusIcon(container);
        });
    };

    $.fn.hideAllRows = function() {
        return this.each(function() {
            $(this).find("tr.grid-row").hide();
        });
    };

    $.fn.showAllRows = function() {
        return this.each(function() {
            $(this).find("tr.grid-row").show();
        });
    };

    $.fn.hideRowsByCondition = function(conditionFn) {
        return this.each(function() {
            $(this).find("tr.grid-row").each(function() {
                const json = $(this).find("textarea").val();
                try {
                    const data = JSON.parse(json);
                    if (conditionFn(data)) {
                        $(this).show();
                    } else {
                        $(this).hide();
                    }
                } catch (e) {
                    console.warn("JSON error:", e);
                }
            });
        });
    };
})(jQuery);
