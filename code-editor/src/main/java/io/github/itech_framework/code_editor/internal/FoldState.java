package io.github.itech_framework.code_editor.internal;

import java.util.List;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class FoldState {

    private final SimpleIntegerProperty revision = new SimpleIntegerProperty(this, "revision", 0);

    private List<FoldLogic.Range> ranges = List.of();

    public List<FoldLogic.Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<FoldLogic.Range> value) {
        ranges = List.copyOf(value);
        touch();
    }

    public void touch() {
        revision.set(revision.get() + 1);
    }

    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }
}