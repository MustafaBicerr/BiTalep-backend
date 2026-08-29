package com.bitalep.entity;

import java.util.Map;

public enum FormType {
    LEAVE,
    TRAINING,
    ADVANCE,
    MATERIAL,
    TASK;

    private static final Map<FormType, String> NAMES = Map.of(
            LEAVE, "İzin",
            TRAINING, "Eğitim",
            ADVANCE, "Avans",
            MATERIAL, "Malzeme",
            TASK, "Görev"
    );

    public String displayName() {
        return NAMES.get(this);
    }
}
