package com.hxc.stockrecord.model;

/**
 * @Description:
 * @Author: huangxingchang
 * @Date: 2025-11-10
 * @Version: V1.0
 */
public class StockData {
    private String code;
    private String name;
    private double currentPrice;
    private double buyPrice;
    private double sellPrice;
    private String updateTime;
    private String change;//涨跌
    private String changePercent;
    private boolean sendMessage;
    private String buyPercent;
    private String sellPercent;
    private double alertPrice;
    private String max;
    private String min;
    private double buyAlertPrice;
    private boolean isBuy;
    private double totalNow;
    private String totalNowPercent;


    public StockData(String code, String name, double currentPrice, double buyPrice, double sellPrice, String updateTime, String change, String changePercent, boolean sendMessage, String buyPercent, String sellPercent ,double alertPrice, String max, String min, double buyAlertPrice, boolean isBuy, double totalNow, String totalNowPercent) {
        this.code = code;
        this.name = name;
        this.currentPrice = currentPrice;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.updateTime = updateTime;
        this.change = change;
        this.changePercent = changePercent;
        this.sendMessage = sendMessage;
        this.buyPercent = buyPercent;
        this.sellPercent = sellPercent;
        this.alertPrice = alertPrice;
        this.max = max;
        this.min = min;
        this.buyAlertPrice = buyAlertPrice;
        this.isBuy = isBuy;
        this.totalNow = totalNow;
        this.totalNowPercent = totalNowPercent;
    }

    public StockData() {
        this.code = this.code;
        this.name = this.name;
        this.currentPrice = this.currentPrice;
        this.buyPrice = this.buyPrice;
        this.sellPrice = this.sellPrice;
        this.updateTime = this.updateTime;
        this.change = this.change;
        this.changePercent = this.changePercent;
        this.sendMessage = this.sendMessage;
        this.buyPercent = this.buyPercent;
        this.sellPercent = this.sellPercent;
        this.alertPrice = this.alertPrice;
        this.max = this.max;
        this.min = this.min;
        this.buyAlertPrice = this.buyAlertPrice;
        this.isBuy = this.isBuy;
        this.totalNow = this.totalNow;
        this.totalNowPercent = this.totalNowPercent;
    }

    public double getIncreaseRate() {
        if (buyPrice == 0) {
            return 0;
        }
        return ((sellPrice - buyPrice) / buyPrice) * 100;
    }

    public double getDifference() {
        return sellPrice - buyPrice;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public String getUpdateTime() { return updateTime; }

    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

    public String getChange() { return change; }

    public void setChange(String change) { this.change = change; }

    public String getChangePercent() { return changePercent; }

    public void setChangePercent(String changePercent) { this.changePercent = changePercent; }

    public boolean isSendMessage() { return sendMessage; }

    public void setSendMessage(boolean sendMessage) { this.sendMessage = sendMessage; }

    public String getBuyPercent() { return buyPercent; }

    public void setBuyPercent(String buyPercent) { this.buyPercent = buyPercent; }

    public String getSellPercent() { return sellPercent; }

    public void setSellPercent(String sellPercent) { this.sellPercent = sellPercent; }

    public double getAlertPrice() { return alertPrice; }

    public void setAlertPrice(double alertPrice) { this.alertPrice = alertPrice; }

    public String getMax() { return max; }
    public void setMax(String max) { this.max = max; }

    public String getMin() { return min; }
    public void setMin(String min) { this.min = min; }

    public double getBuyAlertPrice() { return buyAlertPrice; }

    public void setBuyAlertPrice(double buyAlertPrice) { this.buyAlertPrice = buyAlertPrice; }

    public boolean isBuy() { return isBuy; }

    public void setBuy(boolean isBuy) { this.isBuy = isBuy; }

    public double getTotalNow() { return totalNow; }

    public void setTotalNow(double totalNow) { this.totalNow = totalNow; }

    public String getTotalNowPercent() { return totalNowPercent; }

    public void setTotalNowPercent(String totalNowPercent) { this.totalNowPercent = totalNowPercent; }

}
