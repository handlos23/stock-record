package com.hxc.stockrecord.settings;

import com.hxc.stockrecord.model.StockData;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
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

        // 设置首选大小
        JComponent component = settingsComponent.getPanel();
        component.setPreferredSize(new Dimension(600, 400));
        return component;
    }

    @Override
    public boolean isModified() {
        List<StockData> currentStocks = settingsComponent.getStocks();
        boolean stocksModified = !currentStocks.equals(settings.stocks);

        // 新增微信配置检查
        boolean wechatModified = !settingsComponent.getAppidText().equals(settings.appid)
                || !settingsComponent.getSecretText().equals(settings.secret)
                || !settingsComponent.getOpenIdText().equals(settings.openId)
                || !settingsComponent.getTemplateNumberText().equals(settings.templateNumber);

        return stocksModified || wechatModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<StockData> newStocks = settingsComponent.getStocks();
        // 确保类型匹配
        settings.stocks = newStocks != null ? new ArrayList<>(newStocks) : new ArrayList<>();

        // 保存微信配置
        settings.appid = settingsComponent.getAppidText();
        settings.secret = settingsComponent.getSecretText();
        settings.openId = settingsComponent.getOpenIdText();
        settings.templateNumber = settingsComponent.getTemplateNumberText();
    }

    @Override
    public void reset() {
        settingsComponent.setStocks(new ArrayList<>(settings.stocks));
        // 重置微信配置
        settingsComponent.setAppidText(settings.appid);
        settingsComponent.setSecretText(settings.secret);
        settingsComponent.setOpenIdText(settings.openId);
        settingsComponent.setTemplateNumberText(settings.templateNumber);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
