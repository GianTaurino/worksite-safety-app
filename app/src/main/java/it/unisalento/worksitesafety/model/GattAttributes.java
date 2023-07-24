package it.unisalento.worksitesafety.model;

import java.util.HashMap;

public class GattAttributes {

    private static HashMap<String, String> attributes = new HashMap<>();
    public static String UUID_DANGER = "00000002-710e-4a5b-8d75-3e5b444bc3cf";
    public static String UUID_DANGER_DESCRIPTOR = "00000003-710e-4a5b-8d75-3e5b444bc3cf";

    static {
        // Service
        attributes.put("00000001-710e-4a5b-8d75-3e5b444bc3cf", "Danger Service");
        // Characteristic
        attributes.put(UUID_DANGER, "Danger");
        attributes.put(UUID_DANGER_DESCRIPTOR, "Danger Service Descriptor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

}
