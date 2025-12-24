package com.hxc.stockrecord.entity;

import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import java.util.Objects;

public class StockInfo {
    private String code;
    private String name;
    private String now;
    private String change;
    private String changePercent;
    private String time;
    private String max;
    private String min;
    private String costPrise;
    private String bonds;
    private String incomePercent;
    private String income;
    private String buyPriceDiffenece;
    private String buyPriceDiffenecePercent;
    //换手率
    private String turnoverRate;

    public StockInfo() {
    }

    public StockInfo(String code) {
        if (StringUtils.isNotBlank(code)) {
            String[] codeStr = code.split(",");
            if (codeStr.length > 2) {
                this.code = codeStr[0];
                this.costPrise = codeStr[1];
                this.bonds = codeStr[2];
            } else {
                this.code = codeStr[0];
                this.costPrise = "--";
                this.bonds = "--";
            }
        } else {
            this.code = code;
        }
        this.name = "--";
    }

    public StockInfo(String code, Map<String, String[]> codeMap) {
        this.code = code;
        if(codeMap.containsKey(code)) {
            String[] codeStr = codeMap.get(code);
            if (codeStr.length > 2) {
                this.code = codeStr[0];
                this.costPrise = codeStr[1];
                this.bonds = codeStr[2];
            }
        }
    }

    // Getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNow() { return now; }
    public void setNow(String now) { this.now = now; }
    public String getChange() { return change; }
    public void setChange(String change) { this.change = change; }
    public String getChangePercent() { return changePercent; }
    public void setChangePercent(String changePercent) { this.changePercent = changePercent; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getMax() { return max; }
    public void setMax(String max) { this.max = max; }
    public String getMin() { return min; }
    public void setMin(String min) { this.min = min; }
    public String getCostPrise() { return costPrise; }
    public void setCostPrise(String costPrise) { this.costPrise = costPrise; }
    public String getBonds() { return bonds; }
    public void setBonds(String bonds) { this.bonds = bonds; }
    public String getIncomePercent() { return incomePercent; }
    public void setIncomePercent(String incomePercent) { this.incomePercent = incomePercent; }
    public String getIncome() { return income; }
    public void setIncome(String income) { this.income = income; }

    public String getBuyPriceDiffenece() { return buyPriceDiffenece; }
    public void setBuyPriceDiffenece(String buyPriceDiffenece) { this.buyPriceDiffenece = buyPriceDiffenece; }
    public String getBuyPriceDiffenecePercent() { return buyPriceDiffenecePercent; }
    public void setBuyPriceDiffenecePercent(String buyPriceDiffenecePercent) { this.buyPriceDiffenecePercent = buyPriceDiffenecePercent; }

    public String getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(String turnoverRate) { this.turnoverRate = turnoverRate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockInfo bean = (StockInfo) o;
        return Objects.equals(code, bean.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    public String getValueByColumn(String colums, boolean colorful) {
        switch (colums) {
            case "编码":
                return this.getCode();
            case "股票名称":
                return this.getName();
            case "当前价":
                return this.getNow();
            case "涨跌":
                String changeStr = "--";
                if (this.getChange() != null) {
                    changeStr = this.getChange().startsWith("-") ? this.getChange() : "+" + this.getChange();
                }
                return changeStr;
            case "涨跌幅":
                String changePercentStr = "--";
                if (this.getChangePercent() != null) {
                    changePercentStr = this.getChangePercent().startsWith("-") ? this.getChangePercent() : "+" + this.getChangePercent();
                }
                return changePercentStr + "%";
            case "最高价":
                return this.getMax();
            case "最低价":
                return this.getMin();
            case "成本价":
                return this.getCostPrise();
            case "持仓":
                return this.getBonds();
            case "收益率":
                return this.getCostPrise() != null ? this.getIncomePercent() + "%" : this.getIncomePercent();
            case "收益":
                return this.getIncome();
            case "更新时间":
                String timeStr = "--";
                if (this.getTime() != null) {
                    timeStr = this.getTime().substring(8);
                }
                return timeStr;
            default:
                return "";
        }
    }
}
