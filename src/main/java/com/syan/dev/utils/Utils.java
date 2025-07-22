package com.syan.dev.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public String escapeJson(Object obj) {
        if (obj == null) return "";
        return obj.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String extractFieldIds(String jsonString) {
        JSONObject root = new JSONObject(jsonString);
        JSONArray elements = root.optJSONArray("elements");

        List<String> ids = new ArrayList<>();
        extractIdsRecursive(elements, ids);

        return String.join(",", ids);  // Bisa juga pakai "\n"
    }

    private static void extractIdsRecursive(JSONArray elements, List<String> ids) {
        if (elements == null) return;

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.optJSONObject(i);
            if (element == null) continue;

            // Cek apakah punya properties.id
            JSONObject props = element.optJSONObject("properties");
            if (props != null && props.has("id")) {
                ids.add(props.optString("id"));
            }

            // Telusuri anak-anaknya (jika ada)
            JSONArray childElements = element.optJSONArray("elements");
            if (childElements != null) {
                extractIdsRecursive(childElements, ids);
            }
        }
    }
}
