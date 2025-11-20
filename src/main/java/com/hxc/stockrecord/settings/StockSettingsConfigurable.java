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
import java.util.Objects;

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

        JComponent component = settingsComponent.getPanel();
        component.setPreferredSize(new Dimension(800, 500));
        return component;
    }

    @Override
    public boolean isModified() {
        if (settingsComponent == null || settings == null) {
            return false;
        }

        List<StockData> currentStocks = settingsComponent.getStocks();
        boolean stocksModified = !Objects.equals(currentStocks, settings.stocks);

        boolean wechatModified = !Objects.equals(settingsComponent.getAppidText(), settings.getAppid())
                || !Objects.equals(settingsComponent.getSecretText(), settings.getSecret())
                || !Objects.equals(settingsComponent.getOpenIdText(), settings.getOpenId())
                || !Objects.equals(settingsComponent.getTemplateNumberText(), settings.templateNumber);

        return stocksModified || wechatModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsComponent == null || settings == null) {
            throw new ConfigurationException("Configuration not initialized");
        }

        // 验证股票数据
        List<StockData> newStocks = settingsComponent.getStocks();
        if (newStocks != null) {
            for (StockData stock : newStocks) {
                if (stock.getCode() == null || stock.getCode().trim().isEmpty()) {
                    throw new ConfigurationException("股票代码不能为空");
                }
                if (stock.getBuyPrice() < 0 || stock.getSellPrice() < 0) {
                    throw new ConfigurationException("买入价和卖出价不能为负数");
                }
            }
            settings.stocks = new ArrayList<>(newStocks);
        } else {
            settings.stocks = new ArrayList<>();
        }

        // 验证微信配置
        String appid = settingsComponent.getAppidText();
        String secret = settingsComponent.getSecretText();
        String openId = settingsComponent.getOpenIdText();
        String templateNumber = settingsComponent.getTemplateNumberText();

        if (appid != null && !appid.trim().isEmpty()) {
            if (secret == null || secret.trim().isEmpty()) {
                throw new ConfigurationException("配置了AppID时，Secret不能为空");
            }
            if (openId == null || openId.trim().isEmpty()) {
                throw new ConfigurationException("配置了AppID时，OpenID不能为空");
            }
            settings.bindWechat(appid, secret, openId);
        } else {
            settings.unbindWechat();
        }
        settings.templateNumber = templateNumber;
    }

    @Override
    public void reset() {
        if (settingsComponent == null || settings == null) {
            return;
        }

        // 重置股票配置
        settingsComponent.setStocks(new ArrayList<>(settings.stocks));

        // 重置微信配置
        settingsComponent.setAppidText(settings.getAppid() != null ? settings.getAppid() : "");
        settingsComponent.setSecretText(settings.getSecret() != null ? settings.getSecret() : "");
        settingsComponent.setOpenIdText(settings.getOpenId() != null ? settings.getOpenId() : "");
        settingsComponent.setTemplateNumberText(settings.templateNumber != null ? settings.templateNumber : "");
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
