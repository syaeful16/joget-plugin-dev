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
})(jQuery);
