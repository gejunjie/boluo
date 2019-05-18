package com.boluo.message.service;

import com.boluo.message.bean.api.base.ResponseModel;
import com.boluo.message.bean.api.group.GroupCreateModel;
import com.boluo.message.bean.card.GroupCard;
import com.boluo.message.bean.db.Group;
import com.boluo.message.bean.db.GroupMember;
import com.boluo.message.bean.db.User;
import com.boluo.message.factory.GroupFactory;
import com.boluo.message.factory.PushFactory;
import com.boluo.message.factory.UserFactory;
import com.boluo.message.provider.LocalDateTimeConverter;
import com.google.common.base.Strings;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/group")
public class GroupService extends BaseService {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> create(GroupCreateModel model){
        if (GroupCreateModel.check(model)){
            return ResponseModel.buildParameterError();
        }
        User creater = getSelf();
        //创建者并不在列表中
        model.getUsers().remove(creater.getId());
        if (model.getUsers().size() == 0){
            return ResponseModel.buildParameterError();
        }
        //检查是否已有
        if (GroupFactory.findByName(model.getName()) != null){
            return ResponseModel.buildHaveAccountError();
        }
        List<User> users = new ArrayList<>();
        for (String s : model.getUsers()){
            User user = UserFactory.findById(s);
            if (user == null) continue;
            users.add(user);
        }
        if (users.size() == 0){
            return ResponseModel.buildParameterError();
        }
        Group group= GroupFactory.create(creater, model, users);
        if (group == null){
            return ResponseModel.buildServiceError();
        }
        // 拿管理员的信息（自己的信息）
        GroupMember creatorMember = GroupFactory.getMember(creater.getId(), group.getId());
        if (creatorMember == null) {
            // 服务器异常
            return ResponseModel.buildServiceError();
        }

        // 拿到群的成员，给所有的群成员发送信息，已经被添加到群的信息
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if (members == null) {
            // 服务器异常
            return ResponseModel.buildServiceError();
        }

        members = members.stream()
                .filter(groupMember -> !groupMember.getId().equalsIgnoreCase(creatorMember.getId()))
                .collect(Collectors.toSet());

        // 开始发起推送
        PushFactory.pushJoinGroup(members);

        return ResponseModel.buildOk(new GroupCard(creatorMember));
    }

    /**
     * 查找群，没有传递参数就是搜索最近所有的群
     *
     * @param name 搜索的参数
     * @return 群信息列表
     */
    @GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> search(@PathParam("name") @DefaultValue("") String name){
        User self = getSelf();
        List<Group> groups = GroupFactory.search(name);
        if (groups != null && groups.size() > 0){
            //    List<GroupCard> groupCards = groups.stream()
//            .map(group -> {
//                GroupMember member = GroupFactory.getMember(self.getId(), group.getId());
//                return new GroupCard(group, member);
//            }).collect(Collectors.toList());
            List<GroupCard> groupCards = groups.stream()
                    .map(new Function<Group, GroupCard>() {
                        @Override
                        public GroupCard apply(Group group) {
                            GroupMember member = GroupFactory.getMember(self.getId(), group.getId());
                            return new GroupCard(group, member);
                        }
                    }).collect(Collectors.toList());
            return ResponseModel.buildOk(groupCards);
        }
        return ResponseModel.buildOk();
    }

    /**
     * 拉取自己当前的群的列表
     *
     * @param dateStr 时间字段，不传递，则返回全部当前的群列表；有时间，则返回这个时间之后的加入的群
     * @return 群信息列表
     */
    @GET
    @Path("/list/{date:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> list(@PathParam("date") @DefaultValue("") String dateStr){
        User self = getSelf();
        LocalDateTime localDateTime = null;
        if (!Strings.isNullOrEmpty(dateStr)){
            localDateTime = LocalDateTime.parse(dateStr, LocalDateTimeConverter.FORMATTER);
        }
        Set<GroupMember> members = GroupFactory.getMembers(self);
        if (members == null || members.size() == 0)
            return ResponseModel.buildOk();
        final LocalDateTime finalDateTime = localDateTime;
        List<GroupCard> groupCards = members.stream()
                .filter(groupMember -> finalDateTime == null
                || groupMember.getUpdateAt().isAfter(finalDateTime))
                .map(GroupCard::new)
                .collect(Collectors.toList());
        return ResponseModel.buildOk(groupCards);
    }
}
