package com.hxc.stockrecord.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class StockSettingsConfigurable implements Configurable {
    private StockSettingsComponent settingsComponent;
    private StockSettingsState settings;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Stock Demo Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new StockSettingsComponent();
        settings = StockSettingsState.getInstance();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        List<StockSettingsState.Stock> currentStocks = settingsComponent.getStocks();
        return !currentStocks.equals(settings.stocks);
    }

    @Override
    public void apply() throws ConfigurationException {
        List<StockSettingsState.Stock> newStocks = settingsComponent.getStocks();
        // 确保类型匹配
        settings.stocks = newStocks != null ? new ArrayList<>(newStocks) : new ArrayList<>();
    }

    @Override
    public void reset() {
        settingsComponent.setStocks(new ArrayList<>(settings.stocks));
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
