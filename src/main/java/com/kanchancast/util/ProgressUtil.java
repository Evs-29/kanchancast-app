package com.kanchancast.util;

import com.kanchancast.model.StageEnum;

public final class ProgressUtil {
    private ProgressUtil() {}

    public static int percentForStatus(String status) {
        if (status == null || status.isBlank()) return 0;
        StageEnum[] stages = StageEnum.values();
        for (int i = 0; i < stages.length; i++) {
            if (stages[i].name().equalsIgnoreCase(status.trim())) {
                return Math.round(((i + 1) * 100.0f) / stages.length);
            }
        }
        return 0;
    }

    public static String renderBar(int percent) {
        percent = Math.max(0, Math.min(100, percent));
        int blocks = percent / 10; // 10 segments
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) sb.append(i < blocks ? "#" : ".");
        sb.append("] ").append(percent).append("%");
        return sb.toString();
    }
}
