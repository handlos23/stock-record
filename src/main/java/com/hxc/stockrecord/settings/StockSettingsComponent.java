package com.hxc.stockrecord.settings;

import com.hxc.stockrecord.model.StockData;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StockSettingsComponent {
    private final JPanel mainPanel;
    private final JTable stockTable;
    private final StockTableModel tableModel;

    public StockSettingsComponent() {
        tableModel = new StockTableModel();
        stockTable = new JTable(tableModel);

        // 设置表格属性
        stockTable.setRowHeight(JBUI.scale(24));
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.setAutoCreateRowSorter(true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加");
        JButton removeButton = new JButton("删除");

        addButton.addActionListener(e -> addStock());
        removeButton.addActionListener(e -> removeStock());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        // 创建主面板
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tablePanel, BorderLayout.CENTER);
    }

    private void addStock() {
        StockData newStock = new StockData();
        newStock.setCode("");
        newStock.setName("");
        newStock.setCurrentPrice(0.0);
        newStock.setBuyPrice(0.0);
        newStock.setSellPrice(0.0);
        tableModel.addStock(newStock);
    }

    private void removeStock() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = stockTable.convertRowIndexToModel(selectedRow);
            tableModel.removeStock(modelRow);
        }
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
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
