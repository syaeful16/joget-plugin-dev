package com.syan.dev.utils;

import org.joget.commons.util.LogUtil;
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
        LogUtil.info("Util 1", elements.toString());

        List<String> ids = new ArrayList<>();
        extractIdsRecursive(elements, ids);

        return String.join(",", ids);
    }

    private static void extractIdsRecursive(JSONArray elements, List<String> ids) {
        if (elements == null) return;

        JSONArray array = elements.getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONArray("elements");
        LogUtil.info("Utils 2", array.toString());

        for (int i = 0; i < array.length(); i++) {
            JSONObject objectProperties = array.getJSONObject(i).getJSONObject("properties");

            if (objectProperties.has("id")) {
                String resultID = objectProperties.optString("id");
                ids.add(resultID);
            }
        }
    }
}
