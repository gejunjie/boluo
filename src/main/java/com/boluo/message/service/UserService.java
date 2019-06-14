package com.boluo.message.service;

import com.boluo.message.bean.api.base.ResponseModel;
import com.boluo.message.bean.api.user.UpdateInfoModel;
import com.boluo.message.bean.card.UserCard;
import com.boluo.message.bean.db.User;
import com.boluo.message.factory.UserFactory;
import com.google.common.base.Strings;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/user")
public class UserService extends BaseService {
    /**
     * 用户信息修改接口
     * 返回自己的个人信息
     * @param model
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model){
        if (!UpdateInfoModel.check(model)){
            return ResponseModel.buildParameterError();
        }
        User self = getSelf();
        self = model.updateToUser(self);
        self = UserFactory.update(self);
        UserCard userCard = new UserCard(self);
        return ResponseModel.buildOk(userCard);
    }

    /**
     * 拉取联系人
     * @return
     */
    @GET
    @Path("/contact")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> contact(){
        User self = getSelf();
        List<User> users = UserFactory.contacts(self);
        // map操作，相当于转置操作，User->UserCard
        List<UserCard> userCards = users.stream()
                .map(user -> new UserCard(user, true))
                .collect(Collectors.toList());

//        List<UserCard> userCards = new ArrayList<>();
//        for (User user : users) {
//            UserCard userCard = new UserCard(user, true);
//            userCards.add(userCard);
//        }
        return ResponseModel.buildOk(userCards);
    }

    /**
     * 添加好友
     * @return
     */
    @PUT
    @Path("/follow/{followId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> follow(@PathParam("followId") String followId){
        User self = getSelf();
        //不能添加自己
        if (followId.equalsIgnoreCase(self.getId())
                || Strings.isNullOrEmpty(followId)){
            return ResponseModel.buildParameterError();
        }

        User followUser = UserFactory.findById(followId);
        if (followUser == null){
            return ResponseModel.buildNotFoundUserError(followId);
        }
        followUser = UserFactory.follow(self, followUser, null);
        if (followUser == null){
            return ResponseModel.buildServiceError();
        }
        return ResponseModel.buildOk(new UserCard(followUser, true));
    }

    /**
     * 按名字搜索联系人
     * @return
     */
    @GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> search(@DefaultValue("") @PathParam("name") String name){
        User self = getSelf();
        List<User> searchUsers = UserFactory.search(name);
        List<User> contacts = UserFactory.contacts(self);

        List<UserCard> userCards = searchUsers.stream()
                .map(user -> {
                    boolean isFollow = user.getId().equalsIgnoreCase(self.getId()) // 进行联系人的任意匹配，匹配其中的Id字段
                            || contacts.stream().anyMatch(
                            contactUser -> contactUser.getId()
                                    .equalsIgnoreCase(user.getId())
                    );
                    return new UserCard(user, isFollow);
                }).collect(Collectors.toList());
        return ResponseModel.buildOk(userCards);
    }

    /**
     *  获取某人的信息
     */
    @GET
    @Path("{id}") // http://127.0.0.1/api/user/{id}
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> getUser(@PathParam("id") String id) {
        if (Strings.isNullOrEmpty(id)) {
            return ResponseModel.buildParameterError();
        }
        User self = getSelf();
        if (self.getId().equalsIgnoreCase(id)) {
            // 返回自己，不必查询数据库
            return ResponseModel.buildOk(new UserCard(self, true));
        }
        User user = UserFactory.findById(id);
        if (user == null){
            return ResponseModel.buildNotFoundUserError(null);
        }
        // 如果我们直接有关注的记录，则我已关注需要查询信息的用户
        boolean isFollow = UserFactory.getUserFollow(self, user) != null;
        return ResponseModel.buildOk(new UserCard(user, isFollow));
    }
}
