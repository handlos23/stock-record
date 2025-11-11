package com.hxc.stockrecord.settings;

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
    public List<Stock> stocks = new ArrayList<>();

    public static class Stock {
        private String code;
        private String name;
        private double currentPrice;
        private double buyPrice;
        private double sellPrice;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public void setBuyPrice(double buyPrice) {
            this.buyPrice = buyPrice;
        }

        public double getSellPrice() {
            return sellPrice;
        }

        public void setSellPrice(double sellPrice) {
            this.sellPrice = sellPrice;
        }

        public double getIncreaseRate() {
            if (buyPrice == 0) {return 0;}
            return ((sellPrice - buyPrice) / buyPrice) * 100;
        }

        public double getDifference() {
            return sellPrice - buyPrice;
        }
    }

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
