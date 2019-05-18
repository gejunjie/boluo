package com.boluo.message.factory;

import com.boluo.message.bean.api.group.GroupCreateModel;
import com.boluo.message.bean.db.Group;
import com.boluo.message.bean.db.GroupMember;
import com.boluo.message.bean.db.User;
import com.boluo.message.utils.Hib;
import com.google.common.base.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupFactory {

    public static Group findById(String groupId){
        return Hib.query(session -> session.get(Group.class, groupId));
    }

    public static Group findById(User user, String groupId){
        GroupMember member = getMember(user.getId(), groupId);
        return member == null ? null : member.getGroup();
    }

    public static Group findByName(String name){
        return Hib.query(session -> (Group) session
                .createQuery("from Group where lower(name)=:name ")
                .setParameter("name", name.toLowerCase())
        .        uniqueResult());
    }

    public static GroupMember getMember(String userId, String groupId) {
        return Hib.query(session -> (GroupMember) session
        .createQuery("from GroupMember where userId=:userId and groupId=:groupId")
        .setParameter("userId", userId)
        .setParameter("groupId",groupId)
        .uniqueResult());
    }
    // 获取一个群的所有成员
    public static  Set<GroupMember> getMembers(Group group){
        return Hib.query(session -> {
            List<GroupMember> members = session.createQuery("from GroupMember where group=:group")
                    .setParameter("group", group)
                    .list();
            return new HashSet<>(members);
        });
    }

    // 获取一个人加入的所有群
    public static Set<GroupMember> getMembers(User user){
        return Hib.query(session -> {
            List<GroupMember> members = session.createQuery("from GroupMember where userId=:userId")
                    .setParameter("userId", user.getId())
                    .list();
            return new HashSet<>(members);
        });
    }

    public static List<Group> search(String name){
        if (Strings.isNullOrEmpty(name)) return null;
        String searchName = "%" + name + "%";

        return Hib.query(session -> {
            return session.createQuery("from Group where lower(name) like :name")
                    .setParameter("name", searchName)
                    .setMaxResults(20)
                    .list();
        });
    }
    // 创建群
    public static Group create(User creator, GroupCreateModel model, List<User> users) {
        return Hib.query(session -> {
            Group group = new Group(creator, model);
            session.save(group);
            GroupMember ownerMember = new GroupMember(creator, group);
            ownerMember.setPermissionType(GroupMember.PERMISSION_TYPE_ADMIN_SU);
            session.save(ownerMember);
            for (User user : users){
                GroupMember member = new GroupMember(user, group);
                session.save(member);
            }
            return group;
        });
    }
}
