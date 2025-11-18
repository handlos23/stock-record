package com.hxc.stockrecord.service;

import com.hxc.stockrecord.settings.StockSettingsComponent;
import com.hxc.stockrecord.settings.StockSettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class WechatBindingService {
    private static final Logger LOG = Logger.getInstance(WechatBindingService.class);
    private static final String WECHAT_QR_CODE_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    private static final int HTTP_TIMEOUT = 10000; // 10 seconds
    private static final int QR_CODE_SIZE = 300;

    private static WechatBindingService instance;
    private StockSettingsComponent settingsComponent;
    private HttpClient httpClient;
    private JDialog currentDialog;
    private com.sun.net.httpserver.HttpServer callbackServer;
    private final AtomicReference<String> authCode = new AtomicReference<>();
    private Timer authCheckTimer;

    private WechatBindingService() {
        // 配置HTTP客户端
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .setSocketTimeout(HTTP_TIMEOUT)
                .build();

        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
    }

    public static WechatBindingService getInstance() {
        if (instance == null) {
            instance = new WechatBindingService();
        }
        return instance;
    }

    public void setSettingsComponent(@NotNull StockSettingsComponent component) {
        this.settingsComponent = component;
    }

    /**
     * 显示微信绑定对话框
     */
    public void showBindingDialog() {
        StockSettingsState settings = StockSettingsState.getInstance();
        if (settings.isWechatBound()) {
            Messages.showInfoMessage("微信已绑定", "提示");
            return;
        }

        // 创建绑定对话框
        JDialog dialog = new JDialog();
        dialog.setTitle("微信绑定");
        dialog.setModal(true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        currentDialog = dialog;

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));

        // 添加说明文本
        JLabel infoLabel = new JLabel("<html><center>请输入微信开放平台配置信息<br/>" +
                "1. AppID<br/>" +
                "2. AppSecret<br/>" +
                "配置完成后将生成二维码进行绑定</center></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(infoLabel, BorderLayout.NORTH);

        // 创建输入面板
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField appIdField = new JTextField();
        JPasswordField secretField = new JPasswordField();

        inputPanel.add(new JLabel("AppID:"));
        inputPanel.add(appIdField);
        inputPanel.add(new JLabel("AppSecret:"));
        inputPanel.add(secretField);

        panel.add(inputPanel, BorderLayout.CENTER);

        // 添加按钮
        JButton bindButton = new JButton("生成二维码并绑定");
        bindButton.addActionListener(e -> {
            String appId = appIdField.getText().trim();
            String secret = new String(secretField.getPassword()).trim();

            if (appId.isEmpty() || secret.isEmpty()) {
                Messages.showErrorDialog("请填写完整信息", "错误");
                return;
            }

            // 在后台任务中生成二维码
            ProgressManager.getInstance().run(new Task.Backgroundable(null, "生成二维码", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setText("正在生成二维码...");
                        String state = UUID.randomUUID().toString();
                        int port = getAvailablePort();
                        String redirectUri = "https://523j0o7253.vicp.fun" + "/callback";

                        // 启动本地服务器接收回调
                        startCallbackServer(port);

                        // 修改scope参数为snsapi_base
                        String qrUrl = String.format("%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=%s#wechat_redirect",
                                WECHAT_QR_CODE_URL,
                                appId,
                                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                                state);

                        // 在EDT线程中显示二维码对话框
                        ApplicationManager.getApplication().invokeLater(() -> {
                            showQRCodeDialog(qrUrl, appId, secret);
                            dialog.dispose();
                        });
                    } catch (Exception ex) {
                        LOG.error("生成二维码失败", ex);
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("生成二维码失败：" + ex.getMessage(), "错误"));
                    }
                }
            });
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(bindButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    /**
     * 启动本地服务器接收回调
     */
    private void startCallbackServer(int port) throws IOException {
        callbackServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        callbackServer.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("code=")) {
                String code = query.split("code=")[1].split("&")[0];
                authCode.set(code);

                // 返回成功页面
                String response = "<html><body><h1>授权成功！</h1><p>请返回应用继续操作。</p></body></html>";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                // 停止服务器
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (callbackServer != null) {
                        callbackServer.stop(0);
                        callbackServer = null;
                    }
                });
            }
        });
        callbackServer.start();
    }

    /**
     * 显示二维码对话框
     */
    private void showQRCodeDialog(@NotNull String qrUrl, @NotNull String appId, @NotNull String secret) {
        JDialog dialog = new JDialog();
        dialog.setTitle("微信扫码绑定");
        dialog.setModal(true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(null);

        currentDialog = dialog;

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));

        // 创建二维码图片
        JLabel qrLabel = new JLabel();
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 在后台任务中生成二维码
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "生成二维码", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("正在生成二维码...");
                    BufferedImage qrImage = generateQRCode(qrUrl);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        qrLabel.setIcon(new ImageIcon(qrImage));
                    });
                } catch (Exception e) {
                    LOG.error("生成二维码失败", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog("生成二维码失败：" + e.getMessage(), "错误");
                        dialog.dispose();
                    });
                }
            }
        });

        panel.add(qrLabel, BorderLayout.CENTER);

        // 添加说明文本
        JLabel tipLabel = new JLabel("<html><center>请使用微信扫描上方二维码进行绑定<br/>" +
                "绑定完成后系统将自动完成认证</center></html>");
        tipLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(tipLabel, BorderLayout.NORTH);

        // 添加状态标签
        JLabel statusLabel = new JLabel("等待扫码...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        // 创建并启动定时器
        authCheckTimer = new Timer(1000, e -> {
            String code = authCode.get();
            if (code != null) {
                statusLabel.setText("正在处理授权...");
                if (authCheckTimer != null) {
                    authCheckTimer.stop();
                }

                // 处理授权
                ProgressManager.getInstance().run(new Task.Backgroundable(null, "处理绑定", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            indicator.setText("正在获取访问令牌...");
                            String accessToken = getAccessToken(appId, secret, code);

                            if (accessToken != null) {
                                indicator.setText("正在获取用户信息...");
                                String openId = getOpenId(accessToken);

                                if (openId != null) {
                                    // 在EDT线程中更新UI
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        try {
                                            // 保存配置
                                            StockSettingsState settings = StockSettingsState.getInstance();
                                            settings.bindWechat(appId, secret, openId);

                                            // 更新UI显示
                                            if (settingsComponent != null) {
                                                settingsComponent.updateWechatConfig(appId, secret, openId);
                                            }
                                            Messages.showInfoMessage("微信绑定成功！", "成功");
                                            dialog.dispose();
                                        } catch (Exception ex) {
                                            LOG.error("保存配置失败", ex);
                                            Messages.showErrorDialog("保存配置失败：" + ex.getMessage(), "错误");
                                        }
                                    });
                                }
                            }
                        } catch (Exception ex) {
                            LOG.error("绑定失败", ex);
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showErrorDialog("绑定失败：" + ex.getMessage(), "错误"));
                        }
                    }
                });
            }
        });
        authCheckTimer.start();

        dialog.setContentPane(panel);

        // 添加窗口关闭监听
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (authCheckTimer != null) {
                    authCheckTimer.stop();
                    authCheckTimer = null;
                }
                if (callbackServer != null) {
                    callbackServer.stop(0);
                    callbackServer = null;
                }
                currentDialog = null;
                authCode.set(null);
            }
        });

        dialog.setVisible(true);
    }

    /**
     * 获取可用的端口号
     */
    private int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            LOG.error("获取可用端口失败", e);
            return 8080; // 默认端口
        }
    }

    /**
     * 生成二维码图片
     */
    @NotNull
    private BufferedImage generateQRCode(@NotNull String text) throws Exception {
        com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix matrix = writer.encode(
                text,
                com.google.zxing.BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE
        );

        BufferedImage image = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < QR_CODE_SIZE; x++) {
            for (int y = 0; y < QR_CODE_SIZE; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    /**
     * 获取访问令牌
     */
    @Nullable
    private String getAccessToken(@NotNull String appId, @NotNull String secret, @NotNull String code) throws Exception {
        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                WECHAT_ACCESS_TOKEN_URL, appId, secret, code);

        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        String result = EntityUtils.toString(response.getEntity());

        JSONObject json = new JSONObject(result);
        if (json.has("access_token")) {
            return json.getString("access_token");
        }
        throw new Exception("获取访问令牌失败：" + result);
    }

    /**
     * 获取用户OpenID
     */
    @Nullable
    private String getOpenId(@NotNull String accessToken) throws Exception {
        String url = String.format("%s?access_token=%s", WECHAT_USER_INFO_URL, accessToken);

        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        String result = EntityUtils.toString(response.getEntity());

        JSONObject json = new JSONObject(result);
        if (json.has("openid")) {
            return json.getString("openid");
        }
        throw new Exception("获取用户信息失败：" + result);
    }

    /**
     * 解除绑定
     */
    public void unbind() {
        // 在EDT线程中执行UI操作
        ApplicationManager.getApplication().invokeAndWait(() -> {
            // 在后台线程中执行解绑操作
            ProgressManager.getInstance().run(new Task.Backgroundable(null, "解除绑定", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setText("正在解除绑定...");
                        StockSettingsState settings = StockSettingsState.getInstance();
                        settings.unbindWechat();

                        // 在EDT线程中更新UI
                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                if (settingsComponent != null) {
                                    settingsComponent.updateWechatConfig("", "", "");
                                }
                                Messages.showInfoMessage("已解除微信绑定", "提示");
                            } catch (Exception e) {
                                LOG.error("更新UI失败", e);
                                Messages.showErrorDialog("更新UI失败：" + e.getMessage(), "错误");
                            }
                        });
                    } catch (Exception ex) {
                        LOG.error("解除绑定失败", ex);
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("解除绑定失败：" + ex.getMessage(), "错误"));
                    }
                }
            });
        });
    }

    /**
     * 关闭当前对话框
     */
    public void closeCurrentDialog() {
        if (currentDialog != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                currentDialog.dispose();
                currentDialog = null;
            });
        }
        if (authCheckTimer != null) {
            authCheckTimer.stop();
            authCheckTimer = null;
        }
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
        }
        authCode.set(null);
    }
}
