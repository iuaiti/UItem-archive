package com.uitem.passive.gui;

public record ParamSpec(String key, String label, ParamKind kind, String defaultValue) {

    public ParamSpec(String key, String label, ParamKind kind) {
        this(key, label, kind, "");
    }
}