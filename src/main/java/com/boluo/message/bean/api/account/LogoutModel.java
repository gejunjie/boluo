package com.boluo.message.bean.api.account;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;

public class LogoutModel {
    @Expose
    private String account;
    @Expose
    private String pushId;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    // 校验
    public static boolean check(LogoutModel model) {
        return model != null
                && !Strings.isNullOrEmpty(model.account);
    }
}
