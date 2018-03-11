package org.alfresco.deployment.util;

import java.util.Map;

public class ChartUtil {

    /**
     * Recursively searches the values map and finds a value for a given key
     *
     * @param k         key whose value is to be retrieved
     * @param values    a map of values such as from a values.yaml file
     * @return          the value of the key
     */
    String getValue(String k, Map<String, Object> values) {
        if (values != null) {
            String head = k;
            String tail = "";
            if ((k.contains("."))) {
                head = k.substring(0, k.indexOf('.')); // pop the first segment off the key string
                tail = k.substring(k.indexOf('.') + 1, k.length());
            }
            if (!head.isEmpty()) {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    if (head.equals(entry.getKey())) {
                        if (entry.getValue() instanceof String) {    // if this is a leaf
                            return (String) entry.getValue(); // end the recursion
                        }
                        else if (!tail.isEmpty() && entry.getValue() instanceof Map) {
                            return getValue(tail, (Map<String, Object>) entry.getValue()); // continue the recursion
                        }
                    }
                }
            }
        }
        return null;
    }
}
