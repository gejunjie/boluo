package com.boluo.message.factory;

import com.boluo.message.bean.api.base.PushModel;
import com.boluo.message.bean.card.GroupMemberCard;
import com.boluo.message.bean.card.MessageCard;
import com.boluo.message.bean.db.*;
import com.boluo.message.utils.Hib;
import com.boluo.message.utils.PushDispatcher;
import com.boluo.message.utils.TextUtil;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PushFactory {

    // 发送一条消息，并在当前的发送历史记录中存储记录
    public static void pushNewMessage(User sender, Message message) {
        if ( sender == null || message == null ){
            return;
        }
        MessageCard card = new MessageCard(message);
        String entity = TextUtil.toJson(card);

        PushDispatcher dispatcher = new PushDispatcher();

        if (message.getGroup() == null && Strings.isNullOrEmpty(message.getGroupId())){
            User recevier = UserFactory.findById(message.getReceiverId());
            PushHistory history = new PushHistory();
            history.setEntity(entity);
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE);
            history.setReceiver(recevier);
            history.setReceiverPushId(recevier.getPushId());

            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(), history.getEntity());
            dispatcher.add(recevier, pushModel);
            Hib.queryOnly(session -> session.save(history));
        }else {
            Group group = message.getGroup();
            // 因为延迟加载情况可能为null，需要通过Id查询
            if (group == null){
                group = GroupFactory.findById(message.getGroupId());
            }
            // 如果群真的没有，则返回
            if (group == null) return;

            // 给群成员发送消息
            Set<GroupMember> members = GroupFactory.getMembers(group);
            if (members == null || members.size() == 0) return;
            // 过滤我自己
            members = members.stream()
                    .filter(groupMember -> !groupMember.getUserId()
                    .equalsIgnoreCase(sender.getId()))
                    .collect(Collectors.toSet());
            if (members.size() == 0) return;
            // 一个历史记录列表
            List<PushHistory> histories = new ArrayList<>();
            addGroupMembersPushModel(dispatcher, histories, members, entity,PushModel.ENTITY_TYPE_MESSAGE);
            Hib.queryOnly(session -> {
                for (PushHistory history : histories){
                    session.saveOrUpdate(history);
                }
            });
            dispatcher.submit();
        }
    }
    /**
     * 给群成员构建一个消息，
     * 把消息存储到数据库的历史记录中，每个人，每条消息都是一个记录
     */
    private static void addGroupMembersPushModel(PushDispatcher dispatcher,
                                                 List<PushHistory> histories,
                                                 Set<GroupMember> members,
                                                 String entity,
                                                 int entityTypeMessage) {
        for (GroupMember member : members){
            User receiver = member.getUser();
            if (receiver == null) return;

            PushHistory history = new PushHistory();
            history.setEntityType(entityTypeMessage);
            history.setEntity(entity);
            history.setReceiver(receiver);
            history.setReceiverPushId(receiver.getPushId());
            histories.add(history);

            // 构建一个消息Model
            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(),history.getEntity());
            dispatcher.add(receiver, pushModel);
        }
    }

    /**
     * 通知一些成员，被加入了XXX群
     *
     * @param members 被加入群的成员
     */
    public static void pushJoinGroup(Set<GroupMember> members) {
        PushDispatcher dispatcher = new PushDispatcher();
        List<PushHistory> histories = new ArrayList<>();
        for (GroupMember member : members){
            User receiver = member.getUser();
            if (receiver == null) return;
            GroupMemberCard groupMemberCard = new GroupMemberCard(member);
        }
    }

}
