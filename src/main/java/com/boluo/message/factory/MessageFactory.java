package com.boluo.message.factory;

import com.boluo.message.bean.api.message.MessageCreateModel;
import com.boluo.message.bean.db.Group;
import com.boluo.message.bean.db.Message;
import com.boluo.message.bean.db.User;
import com.boluo.message.utils.Hib;

public class MessageFactory {
    //查询某一个消息
    public static Message findById(String id){
        return Hib.query(session -> session.get(Message.class, id));
    }

    //添加一条普通消息
    public static Message add(User sender, User receiver, MessageCreateModel messageCreateModel){
        Message message = new Message(sender, receiver, messageCreateModel);
        return save(message);
    }

    //添加一条群消息
    public static Message add(User sender, Group group, MessageCreateModel messageCreateModel){
        Message message = new Message(sender, group, messageCreateModel);
        return save(message);
    }

    public static Message save(Message message){
        return Hib.query(session -> {
            session.save(message);
            // 写入到数据库
            session.flush();
            session.refresh(message);
            return message;
        });
    }
}
