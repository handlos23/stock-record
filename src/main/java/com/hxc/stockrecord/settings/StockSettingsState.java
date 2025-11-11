package com.hxc.stockrecord.settings;

import com.hxc.stockrecord.model.StockData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Service
@State(
        name = "StockSettingsState",
        storages = @Storage("StockSettingsState.xml")
)
public final class StockSettingsState implements PersistentStateComponent<StockSettingsState> {
    public List<StockData> stocks = new ArrayList<>();


    @Nullable
    @Override
    public StockSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull StockSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static StockSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(StockSettingsState.class);
    }
}
