package com.boluo.message.service;

import com.boluo.message.bean.api.account.AccountRspModel;
import com.boluo.message.bean.api.account.LoginModel;
import com.boluo.message.bean.api.account.RegisterModel;
import com.boluo.message.bean.api.base.ResponseModel;
import com.boluo.message.bean.db.User;
import com.boluo.message.factory.UserFactory;
import com.google.common.base.Strings;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/account")
public class AccountService extends BaseService {
    // 登录
    @POST
    @Path("/login")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> login(LoginModel model) {
        if (!LoginModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.login(model.getAccount(), model.getPassword());
        if (user != null) {
            // 如果有携带PushId
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }
            // 返回当前的账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            // 登录失败
            return ResponseModel.buildLoginError();
        }
    }

//    // 退出登录
//    @POST
//    @Path("/logout")
//    // 指定请求与返回的相应体为JSON
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public ResponseModel<AccountRspModel> logout(LogoutModel model) {
//        if (!LogoutModel.check(model)) {
//            return ResponseModel.buildParameterError();
//        }
//
//        User user = UserFactory.logout(model.getAccount());
//        if (user != null) {
//            // 返回当前的账户
//            AccountRspModel rspModel = new AccountRspModel(user);
//            return ResponseModel.buildOk(rspModel);
//        } else {
//            // 登录失败
//            return ResponseModel.buildParameterError();
//        }
//    }
    // 注册
//    {
//        "account" : "1111",
//        "password" : "1111",
//        "name" : "a",
//        "pushId" : "1"
//    }
    @POST
    @Path("/register")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> register(RegisterModel model) {
        if (!RegisterModel.check(model)){
            // 返回参数异常
            return ResponseModel.buildParameterError();
        }
        User user = UserFactory.findByPhone(model.getAccount().trim());
        if (user != null){
            return ResponseModel.buildHaveAccountError();
        }
        user = UserFactory.findByName(model.getName().trim());
        if (user != null){
            return ResponseModel.buildHaveNameError();
        }
        user = UserFactory.register(model.getAccount(), model.getPassword(), model.getName());
        if (user != null){
            if (!Strings.isNullOrEmpty(model.getPushId())){
                return bind(user,model.getPushId());
            }
            // 返回当前的账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        }else {
            //注册异常
            return ResponseModel.buildRegisterError();
        }
    }

    @POST
    @Path("/bind/{pushId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> bind(@HeaderParam("token") String token,
                                               @PathParam("pushId") String pushId){
        if (Strings.isNullOrEmpty(token) || Strings.isNullOrEmpty(pushId)){
            return ResponseModel.buildParameterError();
        }
        User self = getSelf();
        return bind(self, pushId);
    }

    /**
     * 绑定的操作
     *
     * @param self   自己
     * @param pushId PushId
     * @return User
     */
    private ResponseModel<AccountRspModel> bind(User self, String pushId) {
        // 进行设备Id绑定的操作
        User user = UserFactory.bindPushId(self, pushId);

        if (user == null) {
            // 绑定失败则是服务器异常
            return ResponseModel.buildServiceError();
        }

        // 返回当前的账户, 并且已经绑定了
        AccountRspModel rspModel = new AccountRspModel(user, true);
        return ResponseModel.buildOk(rspModel);

    }
}
