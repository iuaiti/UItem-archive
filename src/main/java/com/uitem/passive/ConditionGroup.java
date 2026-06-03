package com.uitem.passive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConditionGroup {

    private final LogicMode mode;
    private final List<ConditionEntry> entries;

    public ConditionGroup(LogicMode mode, List<ConditionEntry> entries) {
        this.mode = mode == null ? LogicMode.ALL : mode;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static ConditionGroup empty() {
        return new ConditionGroup(LogicMode.ALL, List.of());
    }

    public LogicMode mode() {
        return mode;
    }

    public List<ConditionEntry> entries() {
        return entries;
    }

    public ConditionGroup copy() {
        List<ConditionEntry> copy = new ArrayList<>();
        for (ConditionEntry e : entries) {
            copy.add(e.copy());
        }
        return new ConditionGroup(mode, copy);
    }

    public static final class Builder {
        private LogicMode mode = LogicMode.ALL;
        private final List<ConditionEntry> entries = new ArrayList<>();

        public Builder mode(LogicMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder add(ConditionEntry entry) {
            entries.add(entry);
            return this;
        }

        public Builder set(int index, ConditionEntry entry) {
            if (index >= 0 && index < entries.size()) {
                entries.set(index, entry);
            }
            return this;
        }

        public Builder remove(int index) {
            if (index >= 0 && index < entries.size()) {
                entries.remove(index);
            }
            return this;
        }

        public Builder clear() {
            entries.clear();
            return this;
        }

        public List<ConditionEntry> entries() {
            return entries;
        }

        public ConditionGroup build() {
            return new ConditionGroup(mode, entries);
        }
    }
}