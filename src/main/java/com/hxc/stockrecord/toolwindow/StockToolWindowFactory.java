package com.hxc.stockrecord.toolwindow;

import com.hxc.stockrecord.settings.StockSettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class StockToolWindowFactory implements ToolWindowFactory {
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

        public StockToolWindow(Project project) {
            this.project = project;
            String[] columnNames = {"code", "name", "currencyPrice", "buyPrice", "sellPrice", "increase(%)", "diffencyPrice"};
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

            // 添加刷新按钮
            JButton refreshButton = new JButton("刷新");
            refreshButton.addActionListener(e -> refreshData());
            buttonPanel.add(refreshButton);

            // 添加添加按钮
            JButton addButton = new JButton("添加");
            addButton.addActionListener(e -> addStock());
            buttonPanel.add(addButton);

            // 添加删除按钮
            JButton deleteButton = new JButton("删除");
            deleteButton.addActionListener(e -> deleteStock());
            buttonPanel.add(deleteButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            // 初始化数据
            refreshData();
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

                tableModel.setRowCount(0);
                for (StockSettingsState.Stock stock : state.stocks) {
                    Object[] row = {
                            stock.getCode(),
                            stock.getName(),
                            stock.getCurrentPrice(),
                            stock.getBuyPrice(),
                            stock.getSellPrice(),
                            stock.getIncreaseRate(),
                            stock.getDifference()
                    };
                    tableModel.addRow(row);
                }
            });
        }

        private void addStock() {
            ApplicationManager.getApplication().invokeLater(() -> {
                StockSettingsState state = StockSettingsState.getInstance();
                if (state == null) {
                    state = new StockSettingsState();
                }

                // 创建一个对话框来输入股票信息
                JPanel panel = new JPanel(new GridLayout(0, 2));
                JTextField codeField = new JTextField();
                JTextField nameField = new JTextField();
                JTextField priceField = new JTextField();
                JTextField buyPriceField = new JTextField();
                JTextField sellPriceField = new JTextField();

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

                int result = JOptionPane.showConfirmDialog(
                        this.panel,
                        panel,
                        "add Stock",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result == JOptionPane.OK_OPTION) {
                    try {
                        StockSettingsState.Stock newStock = new StockSettingsState.Stock();
                        newStock.setCode(codeField.getText());
                        newStock.setName(nameField.getText());
                        newStock.setCurrentPrice(Double.parseDouble(priceField.getText()));
                        newStock.setBuyPrice(Double.parseDouble(buyPriceField.getText()));
                        newStock.setSellPrice(Double.parseDouble(sellPriceField.getText()));

                        state.stocks.add(newStock);
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
                    JOptionPane.showMessageDialog(panel, "请先选择要删除的股票");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        panel,
                        "确定要删除选中的股票吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    StockSettingsState state = StockSettingsState.getInstance();
                    if (state != null && state.stocks != null) {
                        // 转换为模型行索引
                        int modelRow = table.convertRowIndexToModel(selectedRow);
                        state.stocks.remove(modelRow);
                        refreshData();
                    }
                }
            });
        }

        public JComponent getContent() {
            return panel;
        }
    }
}
