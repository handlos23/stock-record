package com.hxc.stockrecord.settings;

import com.hxc.stockrecord.model.StockData;
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
    public List<StockData> stocks = new ArrayList<>();

    // 微信配置字段
    private String appid;
    private String secret;
    private String openId;
    public String templateNumber;  // 保持手动配置

    // 微信绑定状态
    private boolean isWechatBound = false;

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

    // 微信绑定相关方法
    public void bindWechat(String appid, String secret, String openId) {
        this.appid = appid;
        this.secret = secret;
        this.openId = openId;
        this.isWechatBound = true;
    }

    public void unbindWechat() {
        this.appid = null;
        this.secret = null;
        this.openId = null;
        this.isWechatBound = false;
    }

    public boolean isWechatBound() {
        return isWechatBound;
    }

    // Getter 方法
    public String getAppid() {
        return appid;
    }

    public String getSecret() {
        return secret;
    }

    public String getOpenId() {
        return openId;
    }
}
