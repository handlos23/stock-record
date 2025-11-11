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

    public StockData(String code, String name, double currentPrice, double buyPrice, double sellPrice) {
        this.code = code;
        this.name = name;
        this.currentPrice = currentPrice;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public StockData() {
        this.code = this.code;
        this.name = this.name;
        this.currentPrice = this.currentPrice;
        this.buyPrice = this.buyPrice;
        this.sellPrice = this.sellPrice;
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
}
