// src/main/java/com/kanchancast/model/StageEnum.java
package com.kanchancast.model;

public enum StageEnum {
    RAW_MATERIAL_PROCUREMENT,
    MELTING,
    CASTING,
    FILING,
    POLISHING,
    STONE_SETTING,
    ENGRAVING,
    PLATING,
    QUALITY_CONTROL,
    PACKAGING,
    DISPATCH;

    public static String firstStageName() {
        return values()[0].name();
    }

    public static boolean isValid(String status) {
        for (StageEnum s : values()) if (s.name().equalsIgnoreCase(status)) return true;
        return false;
    }
}

