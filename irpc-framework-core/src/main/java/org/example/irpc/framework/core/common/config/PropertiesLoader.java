package org.example.irpc.framework.core.common.config;

import org.example.irpc.framework.core.common.utils.CommonUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesLoader {
    private static Properties properties;
    private static String DEFAULT_PROPERTIES_FILE = "/Users/harvey/Developer/src/java/Exercise/irpc-framework/irpc-framework/irpc-framework-core/src/main/resources/irpc.properties";
    private static Map<String, String> propertiesMap = new HashMap<>();

    public static void loadConfiguration() throws IOException {
        if (properties != null) {
            return;
        }
        properties = new Properties();
        FileInputStream in = null;
        in = new FileInputStream(DEFAULT_PROPERTIES_FILE);
        properties.load(in);
    }

    public static String getPropertiesStr(String key) {
        if (properties == null) {
            return null;
        }
        if (CommonUtils.isEmpty(key)) {
            return null;
        }
        if (!propertiesMap.containsKey(key)) {
            String value = properties.getProperty(key);
            propertiesMap.put(key, value);
        }
        return String.valueOf(propertiesMap.get(key));
    }

    public static Integer getPropertiesInteger(String key) {
        if (properties == null) {
            return null;
        }
        if (CommonUtils.isEmpty(key)) {
            return null;
        }
        if (!propertiesMap.containsKey(key)) {
            String value = properties.getProperty(key);
            propertiesMap.put(key, value);
        }
        return Integer.valueOf(propertiesMap.get(key));
    }

}
