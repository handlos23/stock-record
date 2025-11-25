package com.hxc.stockrecord.toolwindow;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.hxc.stockrecord.entity.StockInfo;
import com.hxc.stockrecord.model.StockData;
import com.hxc.stockrecord.settings.StockSettingsState;
import com.hxc.stockrecord.utils.HttpClientPool;
import com.hxc.stockrecord.utils.StockUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StockToolWindowFactory implements ToolWindowFactory {
    private static final String URL = "http://qt.gtimg.cn/q=";
    private static final Pattern DEFAULT_STOCK_PATTERN = Pattern.compile("var hq_str_(\\w+?)=\"(.*?)\";");
    private static final Logger log = LoggerFactory.getLogger(StockToolWindowFactory.class);
    public static final String NAME = "Stock";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        StockToolWindow stockToolWindow = new StockToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(stockToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class StockToolWindow {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JTable table = new JTable();
        private final DefaultTableModel tableModel;
        private final Project project;
        private Timer refreshTimer;
        private final String WECHAT_MP_GET_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
        private final String WECHAT_MP_SEND_MSG_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";

        public StockToolWindow(Project project) {
            this.project = project;
//            String[] columnNames = {"code", "name", "currencyPrice", "buyPrice", "sellPrice", "increase(%)", "diffencyPrice", "updateTime", "change", "changePercent", "sendMessage", "buyPercent", "sellPercent","alertPrice"};
            String[] columnNames = {"code", "name", "currencyPrice", "change", "changePercent", "sendMessage", "alertPrice"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex < 2 ? String.class : Double.class;
                }
            };

            table.setModel(tableModel);
            table.setAutoCreateRowSorter(true);

            // 设置渲染器以更好地显示数字
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    if (value instanceof Double) {
                        value = String.format("%.2f", (Double) value);
                    }
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    // 根据涨跌设置颜色
                    if (column == 5 && value instanceof Double) { // 涨幅列
                        double rate = (Double) value;
                        if (rate > 0) {
                            component.setForeground(Color.RED);
                        } else if (rate < 0) {
                            component.setForeground(Color.GREEN);
                        } else {
                            component.setForeground(Color.BLACK);
                        }
                    }
                    return component;
                }
            };

            for (int i = 2; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(renderer);
            }

            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            // 添加按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            JButton refreshButton = new JButton("刷新");
            refreshButton.addActionListener(e -> refreshData());
            buttonPanel.add(refreshButton);

            JButton addButton = new JButton("添加");
            addButton.addActionListener(e -> addStock());
            buttonPanel.add(addButton);

            JButton editButton = new JButton("修改");
            editButton.addActionListener(e -> editStock());
            buttonPanel.add(editButton);

            JButton deleteButton = new JButton("删除");
            deleteButton.addActionListener(e -> deleteStock());
            buttonPanel.add(deleteButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            // 初始化数据
            refreshData();
        }

        private void startRefreshTimer() {
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            refreshTimer = new Timer(10000, e -> refreshData());
            refreshTimer.start();
        }

        private void stopRefreshTimer() {
            if (refreshTimer != null) {
                refreshTimer.stop();
                refreshTimer = null;
            }
        }

        private void checkAndToggleTimer() {
            StockSettingsState state = StockSettingsState.getInstance();
            if (state != null && state.stocks != null && !state.stocks.isEmpty()) {
                if (refreshTimer == null) {
                    startRefreshTimer();
                }
            } else {
                stopRefreshTimer();
            }
        }

        private void refreshData() {
            ApplicationManager.getApplication().invokeLater(() -> {
                StockSettingsState state = StockSettingsState.getInstance();
                if (state == null) {
                    state = new StockSettingsState();
                }
                if (state.stocks == null) {
                    state.stocks = new ArrayList<>();
                }

                List<String> codeList = state.stocks.stream().map(StockData::getCode).toList();
                Map<String, StockInfo> stockMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(codeList)) {
                    stockMap = pollStock(codeList).stream().collect(Collectors.toMap(StockInfo::getCode, Function.identity()));
                }

                // 更新表格数据
                tableModel.setRowCount(0);
                for (StockData stock : state.stocks) {
                    StockInfo stockInfo = stockMap.get(stock.getCode());
                    if (stockInfo != null) {
                        // 更新StockData中的实时数据
                        stock.setCurrentPrice(Double.parseDouble(stockInfo.getNow()));
                        stock.setUpdateTime(stockInfo.getTime());
//                        stock.setIncreaseRate(Double.parseDouble(stockInfo.getChangePercent()));
//                        stock.setDifference(Double.parseDouble(stockInfo.getChange()));
                    }

                    Object[] row = {
                            stock.getCode(),
                            stock.getName(),
                            stock.getCurrentPrice(),
                            stock.getBuyPrice(),
                            stock.getSellPrice(),
                            stock.getIncreaseRate(),
                            stock.getDifference(),
                            stock.getUpdateTime(),
                            stockInfo != null ? stockInfo.getChange() : "-",
                            stockInfo != null ? stockInfo.getChangePercent() : "-",
                            stock.isSendMessage(),
                            stock.getBuyPercent() + "%",
                            stock.getSellPercent() + "%",
                            stock.getAlertPrice()
                    };
                    tableModel.addRow(row);

                    // 检查是否需要发送微信消息
                    if (stock.isSendMessage() && stockInfo != null) {
                        BigDecimal alertPrice = new BigDecimal(stock.getAlertPrice());

                        String changePercent = stockInfo.getChangePercent();
                        if (changePercent != null) {
                            BigDecimal now = new BigDecimal(stockInfo.getNow());
                            if (stock.getAlertPrice() < 0){
                                alertPrice = new BigDecimal(Math.abs(stock.getAlertPrice()));
                                if (now.compareTo(alertPrice) < 0) {
                                    sendWxMessage(stockInfo, "今天看好的", "买");
                                }
                            }else {
                                if (now.compareTo(alertPrice) > 0) {
                                    sendWxMessage(stockInfo, "今天持有的", "卖");
                                }
                            }



//                            BigDecimal changePercentValue = new BigDecimal(changePercent);

//                            if (changePercentValue.compareTo(new BigDecimal(stock.getSellPercent())) > 0) {
//                                sendWxMessage(stockInfo, "今天持有的", "卖");
//                            } else if (changePercentValue.compareTo(new BigDecimal(stock.getBuyPercent())) < 0) {
//                                sendWxMessage(stockInfo, "今天看好的", "买");
//                            }
//                            BigDecimal now = new BigDecimal(stockInfo.getNow());
//                            BigDecimal buyPrice = new BigDecimal(stock.getBuyPrice());
//                            BigDecimal totalPercent = (now.subtract(buyPrice)).multiply(new BigDecimal("100"))
//                                    .divide(buyPrice, 2, RoundingMode.HALF_UP);
//                            if (totalPercent.compareTo(new BigDecimal("5")) > 0) {
//                                sendWxMessage(stockInfo, "一直持有的", "卖");
//                            }
                        }
                    }
                }
                checkAndToggleTimer();
            });
        }

        private void addStock() {
            ApplicationManager.getApplication().invokeLater(() -> {
                StockSettingsState state = StockSettingsState.getInstance();
                if (state == null) {
                    state = new StockSettingsState();
                }

                JPanel panel = new JPanel(new GridLayout(0, 2));
                JTextField codeField = new JTextField();
                JTextField nameField = new JTextField();
                JTextField priceField = new JTextField();
                JTextField buyPriceField = new JTextField();
                JTextField sellPriceField = new JTextField();
                JCheckBox sendMessageField = new JCheckBox("是否发送", true);
                JTextField buyPercentField = new JTextField();
                JTextField sellPercentField = new JTextField();
                JTextField alertPriceField = new JTextField();

                panel.add(new JLabel("code:"));
                panel.add(codeField);
                panel.add(new JLabel("name:"));
                panel.add(nameField);
                panel.add(new JLabel("currencyPrice:"));
                panel.add(priceField);
                panel.add(new JLabel("buyPrice:"));
                panel.add(buyPriceField);
                panel.add(new JLabel("sellPrice:"));
                panel.add(sellPriceField);
                panel.add(new JLabel("sendMessage:"));
                panel.add(sendMessageField);
                panel.add(new JLabel("buyPercent(%):"));
                panel.add(buyPercentField);
                panel.add(new JLabel("sellPercent(%):"));
                panel.add(sellPercentField);
                panel.add(new JLabel("alertPrice:"));
                panel.add(alertPriceField);

                int result = JOptionPane.showConfirmDialog(
                        this.panel,
                        panel,
                        "add Stock",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result == JOptionPane.OK_OPTION) {
                    try {
                        StockData newStock = new StockData();
                        newStock.setCode(codeField.getText());
                        newStock.setName(nameField.getText());
                        newStock.setCurrentPrice(Double.parseDouble(priceField.getText()));
                        newStock.setBuyPrice(Double.parseDouble(buyPriceField.getText()));
                        newStock.setSellPrice(Double.parseDouble(sellPriceField.getText()));
                        newStock.setUpdateTime(dateFormat.format(new Date()));
                        newStock.setSendMessage(sendMessageField.isSelected());
                        newStock.setBuyPercent(buyPercentField.getText());
                        newStock.setSellPercent(sellPercentField.getText());
                        newStock.setAlertPrice(Double.parseDouble(alertPriceField.getText()));
                        state.stocks.add(newStock);
                        refreshData();
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this.panel, "请输入有效的数字！");
                    }
                }
            });
        }

        private void editStock() {
            ApplicationManager.getApplication().invokeLater(() -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(panel, "请先选择要修改的数据");
                    return;
                }

                StockSettingsState state = StockSettingsState.getInstance();
                if (state == null || state.stocks == null) {
                    return;
                }

                int modelRow = table.convertRowIndexToModel(selectedRow);
                StockData selectedStock = state.stocks.get(modelRow);

                JPanel panel = new JPanel(new GridLayout(0, 2));
                JTextField codeField = new JTextField(selectedStock.getCode());
                JTextField nameField = new JTextField(selectedStock.getName());
                JTextField priceField = new JTextField(String.valueOf(selectedStock.getCurrentPrice()));
                JTextField buyPriceField = new JTextField(String.valueOf(selectedStock.getBuyPrice()));
                JTextField sellPriceField = new JTextField(String.valueOf(selectedStock.getSellPrice()));
                JCheckBox sendMessageField = new JCheckBox("是否发送", selectedStock.isSendMessage());
                JTextField buyPercentField = new JTextField(selectedStock.getBuyPercent());
                JTextField sellPercentField = new JTextField(selectedStock.getSellPercent());
                JTextField alertPriceField = new JTextField(String.valueOf(selectedStock.getAlertPrice()));

                panel.add(new JLabel("code:"));
                panel.add(codeField);
                panel.add(new JLabel("name:"));
                panel.add(nameField);
                panel.add(new JLabel("currencyPrice:"));
                panel.add(priceField);
                panel.add(new JLabel("buyPrice:"));
                panel.add(buyPriceField);
                panel.add(new JLabel("sellPrice:"));
                panel.add(sellPriceField);
                panel.add(new JLabel("sendMessage:"));
                panel.add(sendMessageField);
                panel.add(new JLabel("buyPercent(%):"));
                panel.add(buyPercentField);
                panel.add(new JLabel("sellPercent(%):"));
                panel.add(sellPercentField);
                panel.add(new JLabel("alertPrice:"));
                panel.add(alertPriceField);

                int result = JOptionPane.showConfirmDialog(
                        this.panel,
                        panel,
                        "修改",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result == JOptionPane.OK_OPTION) {
                    try {
                        selectedStock.setCode(codeField.getText());
                        selectedStock.setName(nameField.getText());
                        selectedStock.setCurrentPrice(Double.parseDouble(priceField.getText()));
                        selectedStock.setBuyPrice(Double.parseDouble(buyPriceField.getText()));
                        selectedStock.setSellPrice(Double.parseDouble(sellPriceField.getText()));
                        selectedStock.setUpdateTime(dateFormat.format(new Date()));
                        selectedStock.setSendMessage(sendMessageField.isSelected());
                        selectedStock.setBuyPercent(buyPercentField.getText());
                        selectedStock.setSellPercent(sellPercentField.getText());
                        selectedStock.setAlertPrice(Double.parseDouble(alertPriceField.getText()));
                        refreshData();
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this.panel, "请输入有效的数字！");
                    }
                }
            });
        }

        private void deleteStock() {
            ApplicationManager.getApplication().invokeLater(() -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(panel, "请先选择要删除的数据");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        panel,
                        "确定要删除选中的数据吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    StockSettingsState state = StockSettingsState.getInstance();
                    if (state != null && state.stocks != null) {
                        int modelRow = table.convertRowIndexToModel(selectedRow);
                        state.stocks.remove(modelRow);
                        refreshData();
                    }
                }
            });
        }

        private void sendWxMessage(StockInfo stockInfo, String sourceType, String operateType) {
            try {
                StockSettingsState state = StockSettingsState.getInstance();
                String urlString = WECHAT_MP_SEND_MSG_URL + "?access_token=" + getWxToken();
                JSONObject body = new JSONObject();
                body.put("template_id", state.templateNumber);
                body.put("url", "www.baidu.com");
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("{\"first\":{\"value\":\"")
                        .append(sourceType + "股票：" + stockInfo.getCode() + "_" + stockInfo.getName())
                        .append("\",\"color\":\"#FF0000\"},")
                        .append("\"keyword1\":{\"value\":\"")
                        .append(stockInfo.getNow())
                        .append("\",\"color\":\"#FF0000\"},")
                        .append("\"keyword2\":{\"value\":\"")
                        .append("涨跌为" + stockInfo.getChange() + "，涨跌幅度为：" + stockInfo.getChangePercent() + "%")
                        .append("\",\"color\":\"#173177\"},")
                        .append("\"remark\":{\"value\":\"")
                        .append(operateType)
                        .append("\",\"color\":\"#173177\"}}");

                body.put("data", JSONObject.parse(stringBuffer.toString()));
                body.put("touser", state.getOpenId());
                log.info(":::WxSendMsgImpl msgBody:{}", body.toString());
                String result = HttpUtil.post(urlString, body.toString());
                log.info(":::WxSendMsgImpl send message result:{}", result);

                try {
                    JSONObject jsonObject = JSON.parseObject(result);
                    if (jsonObject.containsKey("errmsg")) {
                        String errmsg = jsonObject.getString("errmsg");
                        if (CharSequenceUtil.isNotEmpty(errmsg) && errmsg.contains("invalid credential")) {
                            urlString = WECHAT_MP_SEND_MSG_URL + "?access_token=" + getWxToken();
                            result = HttpUtil.post(urlString, body.toString());
                            log.info("发送消息结果：{}", result);
                        }
                    }
                } catch (Exception e) {
                    log.error("处理微信消息响应异常", e);
                }
            } catch (Exception e) {
                log.error("WxSendMsgImpl.doSendMsg 微信发送消息异常:", e);
            }
        }

        private String getWxToken() {
            StockSettingsState state = StockSettingsState.getInstance();
            String token = null;
            JSONObject object = new JSONObject();
            object.put("appid", state.getAppid());
            object.put("secret", state.getSecret());
            String invokeUrl = WECHAT_MP_GET_TOKEN_URL + "&" + StockUtils.getUrlParamByJson(object);
            String result = HttpUtil.get(invokeUrl);
            JSONObject resultObj = JSONObject.parseObject(result);
            if (resultObj.containsKey("access_token")) {
                token = resultObj.getString("access_token");
            } else {
                return resultObj.getString("errmsg");
            }
            return token;
        }

        public JComponent getContent() {
            return panel;
        }
    }

    private static List<StockInfo> pollStock(List<String> code) {
        List<StockInfo> stockInfoList = new ArrayList<>();
        List<String> codeList = new ArrayList<>();
        Map<String, String[]> codeMap = new HashMap<>();
        for (String str : code) {
            String[] strArray;
            if (str.contains(",")) {
                strArray = str.split(",");
            } else {
                strArray = new String[]{str};
            }
            codeList.add(strArray[0]);
            codeMap.put(strArray[0], strArray);
        }

        String params = Joiner.on(",").join(codeList);
        try {
            String res = HttpClientPool.getHttpClient().get(URL + params);
            stockInfoList = handleResponse(res, codeMap);
        } catch (Exception e) {
            log.error("实时数据获取失败", e.getMessage());
        }
        return stockInfoList;
    }

    public static List<StockInfo> handleResponse(String response, Map<String, String[]> codeMap) {
        List<StockInfo> stockInfoList = new ArrayList<>();
        for (String line : response.split("\n")) {
            String code = line.substring(line.indexOf("_") + 1, line.indexOf("="));
            String dataStr = line.substring(line.indexOf("=") + 2, line.length() - 2);
            String[] values = dataStr.split("~");
            StockInfo bean = new StockInfo(code, codeMap);
            bean.setName(values[1]);
            bean.setNow(values[3]);
            bean.setChange(values[31]);
            bean.setChangePercent(values[32]);
            bean.setTime(values[30]);
            bean.setMax(values[33]);
            bean.setMin(values[34]);

            BigDecimal now = new BigDecimal(values[3]);
            String costPriceStr = bean.getCostPrise();
            if (StringUtils.isNotEmpty(costPriceStr)) {
                BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                BigDecimal incomeDiff = now.add(costPriceDec.negate());
                if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                    bean.setIncomePercent("0");
                } else {
                    BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 5, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.TEN)
                            .multiply(BigDecimal.TEN)
                            .setScale(3, RoundingMode.HALF_UP);
                    bean.setIncomePercent(incomePercentDec.toString());
                }

                String bondStr = bean.getBonds();
                if (StringUtils.isNotEmpty(bondStr)) {
                    BigDecimal bondDec = new BigDecimal(bondStr);
                    BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                            .setScale(2, RoundingMode.HALF_UP);
                    bean.setIncome(incomeDec.toString());
                }
            }
            stockInfoList.add(bean);
        }
        return stockInfoList;
    }
}
