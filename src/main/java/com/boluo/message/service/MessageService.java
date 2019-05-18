package com.boluo.message.service;

import com.boluo.message.bean.api.base.ResponseModel;
import com.boluo.message.bean.api.message.MessageCreateModel;
import com.boluo.message.bean.card.MessageCard;
import com.boluo.message.bean.db.Group;
import com.boluo.message.bean.db.Message;
import com.boluo.message.bean.db.User;
import com.boluo.message.factory.GroupFactory;
import com.boluo.message.factory.MessageFactory;
import com.boluo.message.factory.PushFactory;
import com.boluo.message.factory.UserFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.awt.*;

public class MessageService extends BaseService {
    // 发送一条消息到服务器
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<MessageCard> pushMessage(MessageCreateModel model){
        if (!MessageCreateModel.check(model)){
            return ResponseModel.buildParameterError();
        }
        User self = getSelf();
        // 查询是否已经在数据库中有了
        Message message = MessageFactory.findById(model.getId());
        if (message != null){
            return ResponseModel.buildOk(new MessageCard(message));
        }
        if (model.getType() == Message.RECEIVER_TYPE_GROUP){
            return pushToGroup(self, model);
        } else {
            return pushToUser(self, model);
        }
    }

    //发送给人
    private ResponseModel<MessageCard> pushToUser(User sender, MessageCreateModel model) {
        User receiver = UserFactory.findById(model.getId());
        if (receiver == null){
            return ResponseModel.buildNotFoundUserError("receiver not exist");
        }
        if (receiver.getId().equalsIgnoreCase(sender.getId())){
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_USER);
        }
        Message message = MessageFactory.add(sender, receiver, model);
        return buildAndPushResponse(sender, message);
    }

    //发送到群
    private ResponseModel<MessageCard> pushToGroup(User sender, MessageCreateModel model) {
        // 找群是有权限性质的找
        Group group = GroupFactory.findById(sender, model.getReceiverId());
        if (group == null){
            // 没有找到接收者群，有可能是你不是群的成员
            return ResponseModel.buildNotFoundUserError("Con't find receiver group");
        }
        Message message = MessageFactory.add(sender, group, model);
        return buildAndPushResponse(sender, message);
    }

    private ResponseModel<MessageCard> buildAndPushResponse(User sender, Message message) {
        if (message == null){
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
        }
        PushFactory.pushNewMessage(sender, message);
        return ResponseModel.buildOk(new MessageCard(message));
    }
}
