package com.hxc.stockrecord.settings;

import com.hxc.stockrecord.model.StockData;
import com.hxc.stockrecord.service.WechatBindingService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StockSettingsComponent {
    private JPanel mainPanel;
    private JTable stockTable;
    private StockTableModel tableModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final JTextField appidField = new JTextField();
    private final JTextField secretField = new JTextField();
    private final JTextField openIdField = new JTextField();
    private final JTextField templateNumberField = new JTextField();

    public StockSettingsComponent() {
        // 创建主面板，使用垂直布局
        mainPanel = new JPanel(new BorderLayout(0, 5));

        // 创建股票配置面板
        JPanel stockPanel = createStockPanel();

        // 创建微信配置面板
        JPanel wechatPanel = createWechatPanel();

        // 使用选项卡布局来组织配置
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("股票配置", stockPanel);
        tabbedPane.addTab("微信配置", wechatPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createStockPanel() {
        tableModel = new StockTableModel();
        stockTable = new JTable(tableModel);

        stockTable.setRowHeight(24); // 减小行高
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.setAutoCreateRowSorter(true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton addButton = new JButton("添加");
        JButton removeButton = new JButton("删除");

        addButton.addActionListener(e -> addStock());
        removeButton.addActionListener(e -> removeStock());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        // 创建股票配置面板
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加边距
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createWechatPanel() {
        // 创建微信配置面板
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 添加微信配置字段
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("微信AppID:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
//        appidField.setEditable(false); // 设置为只读，通过扫码绑定获取
        panel.add(appidField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("微信Secret:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
//        secretField.setEditable(false); // 设置为只读，通过扫码绑定获取
        panel.add(secretField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("微信OpenID:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
//        openIdField.setEditable(false); // 设置为只读，通过扫码绑定获取
        panel.add(openIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("消息模板编号:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        panel.add(templateNumberField, gbc);

        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bindButton = new JButton("扫码绑定");
        JButton unbindButton = new JButton("解除绑定");

        bindButton.addActionListener(e -> {
            // 调用微信绑定服务
            WechatBindingService.getInstance().showBindingDialog();
        });

        unbindButton.addActionListener(e -> {
            // 解除绑定
            WechatBindingService.getInstance().unbind();
            // 清空显示
            appidField.setText("");
            secretField.setText("");
            openIdField.setText("");
        });

        buttonPanel.add(bindButton);
        buttonPanel.add(unbindButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buttonPanel, gbc);

        // 添加边距
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    private void addStock() {
        StockData newStock = new StockData();
        newStock.setCode("");
        newStock.setName("");
        newStock.setCurrentPrice(0.0);
        newStock.setBuyPrice(0.0);
        newStock.setSellPrice(0.0);
        newStock.setUpdateTime(dateFormat.format(new Date()));
        newStock.setSendMessage(true);
        newStock.setBuyPercent("-5");
        newStock.setSellPercent("5");
        tableModel.addStock(newStock);
    }

    private void removeStock() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = stockTable.convertRowIndexToModel(selectedRow);
            tableModel.removeStock(modelRow);
        }
    }

    // 添加微信配置的getter和setter方法
    public String getAppidText() {
        return appidField.getText();
    }

    public void setAppidText(String text) {
        ApplicationManager.getApplication().invokeLater(() -> appidField.setText(text));
    }

    public String getSecretText() {
        return secretField.getText();
    }

    public void setSecretText(String text) {
        ApplicationManager.getApplication().invokeLater(() -> secretField.setText(text));
    }

    public String getOpenIdText() {
        return openIdField.getText();
    }

    public void setOpenIdText(String text) {
        ApplicationManager.getApplication().invokeLater(() -> openIdField.setText(text));
    }

    public String getTemplateNumberText() {
        return templateNumberField.getText();
    }

    public void setTemplateNumberText(String text) {
        templateNumberField.setText(text);
    }

    // 更新微信配置信息
    public void updateWechatConfig(String appid, String secret, String openId) {
        ApplicationManager.getApplication().invokeLater(() -> {
            appidField.setText(appid);
            secretField.setText(secret);
            openIdField.setText(openId);
        });
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return stockTable;
    }

    public List<StockData> getStocks() {
        return tableModel.getStocks();
    }

    public void setStocks(List<StockData> stocks) {
        tableModel.setStocks(stocks);
    }

    private static class StockTableModel extends javax.swing.table.AbstractTableModel {
        private final String[] columnNames = {"code", "name", "currencyPrice", "buyPrice", "sellPrice"};
        private List<StockData> stocks = new ArrayList<>();

        public void addStock(StockData stock) {
            stocks.add(stock);
            int row = stocks.size() - 1;
            fireTableRowsInserted(row, row);
        }

        public void removeStock(int row) {
            stocks.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public List<StockData> getStocks() {
            return new ArrayList<>(stocks);
        }

        public void setStocks(List<StockData> stocks) {
            this.stocks = new ArrayList<>(stocks);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return stocks.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex < 2 ? String.class : Double.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StockData stock = stocks.get(rowIndex);
            switch (columnIndex) {
                case 0: return stock.getCode();
                case 1: return stock.getName();
                case 2: return stock.getCurrentPrice();
                case 3: return stock.getBuyPrice();
                case 4: return stock.getSellPrice();
                case 10: return stock.isSendMessage();
                case 11: return stock.getBuyPercent();
                case 12: return stock.getSellPercent();
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            StockData stock = stocks.get(rowIndex);
            switch (columnIndex) {
                case 0: stock.setCode((String) aValue); break;
                case 1: stock.setName((String) aValue); break;
                case 2: stock.setCurrentPrice((Double) aValue); break;
                case 3: stock.setBuyPrice((Double) aValue); break;
                case 4: stock.setSellPrice((Double) aValue); break;
                case 10: stock.setSendMessage((Boolean) aValue); break;
                case 11: stock.setBuyPercent((String) aValue); break;
                case 12: stock.setSellPercent((String) aValue); break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
