package com.boluo.message.factory;

import com.boluo.message.bean.db.User;
import com.boluo.message.bean.db.UserFollow;
import com.boluo.message.utils.Hib;
import com.boluo.message.utils.TextUtil;
import com.google.common.base.Strings;
import org.hibernate.Session;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserFactory {
    // 通过Token字段查询用户信息
    // 只能自己使用，查询的信息是个人信息，非他人信息
    public static User findByToken(String token) {
        return Hib.query(new Hib.Query<User>() {
            @Override
            public User query(Session session) {
                return (User) session
                        .createQuery("from User where token=:token")
                        .setParameter("token", token)
                        .uniqueResult();
            }
        });
    }

    public static User findByPhone(String phone){
        return Hib.query(session -> (User) session
        .createQuery("from User where phone=:phone")
        .setParameter("phone", phone)
        .uniqueResult());
    }

    public static User findByName(String name){
        return Hib.query(session -> (User) session
                .createQuery("from User where name=:name")
                .setParameter("name", name)
                .uniqueResult());
    }

    public static User findById(String id){
        return Hib.query(session -> session.get(User.class, id));
    }

    public static User login(String account, String passpword){
        final String accountStr = account.trim();
        final String encodePassowrd = encodePassword(passpword);
        User user = Hib.query(session -> (User) session
        .createQuery("from User where phone=:phone and password=:password")
        .setParameter("phone",accountStr)
        .setParameter("password",encodePassowrd)
        .uniqueResult());
        if (user != null){
            user = login(user);
        }
        return user;
    }

    public static User logout(String account){
        final String accountStr = account.trim();
        User user = findByPhone(accountStr);
        if (user != null){
            user = login(user);
        }
        return user;
    }

    /**
     * 用户注册
     * 注册的操作需要写入数据库，并返回数据库中的User信息
     *
     * @param account  账户
     * @param password 密码
     * @param name     用户名
     * @return User
     */
    public static User register(String account, String password, String name) {
        // 去除账户中的首位空格
        account = account.trim();
        // 处理密码
        password = encodePassword(password);

        User user = createUser(account, password, name);
        if (user != null) {
            user = login(user);
        }
        return user;
    }

    /**
     * 创建账户
     * @param account 账户
     * @param password 密码
     * @param name 名字
     * @return
     */
    private static User createUser(String account, String password, String name) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setPhone(account);
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });
    }

    /**
     * 给当前的账户绑定PushId
     *
     * @param user   自己的User
     * @param pushId 自己设备的PushId
     * @return User
     */
    public static User bindPushId(User user, String pushId) {
        if (Strings.isNullOrEmpty(pushId)) return null;
        // 第一步，查询是否有其他账户绑定了这个设备
        // 取消绑定，避免推送混乱
        // 查询的列表不能包括自己
        Hib.queryOnly(session -> {
            List<User> userList = session.createQuery("from User where lower(pushId)=:pushId and id!=:userId")
                    .setParameter("pushId", pushId.toLowerCase())
                    .setParameter("userId", user.getId())
                    .list();
            for (User u : userList){
                u.setPushId(null);
                session.saveOrUpdate(u);
            }
        });

        if (pushId.equalsIgnoreCase(user.getPushId())) {
            // 如果当前需要绑定的设备Id，之前已经绑定过了
            // 那么不需要额外绑定
            return user;
        } else {
            // 如果当前账户之前的设备Id，和需要绑定的不同
            // 那么需要单点登录，让之前的设备退出账户，
            // 给之前的设备推送一条退出消息
            if (Strings.isNullOrEmpty(user.getPushId())) {
                // TODO 推送一个退出消息
            }

            // 更新新的设备Id
            user.setPushId(pushId);
            return update(user);
        }
    }

    public static User login(User user){
        String newToken = UUID.randomUUID().toString();
        newToken = TextUtil.encodeBase64(newToken);
        user.setToken(newToken);
        return update(user);
    }

//    public static User logout(User user){
//        user.setToken("");
//        return update(user);
//    }

    public static User update(User user){
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });

    }

    /**
     * 对密码进行加密操作
     *
     * @param password 原文
     * @return 密文
     */
    private static String encodePassword(String password) {
        // 密码去除首位空格
        password = password.trim();
        // 进行MD5非对称加密，加盐会更安全，盐也需要存储
        password = TextUtil.getMD5(password);
        // 再进行一次对称的Base64加密，当然可以采取加盐的方案
        return TextUtil.encodeBase64(password);
    }

    /**
     * 获取联系人列表
     * @param self
     * @return
     */
    public static List<User> contacts(User self) {

        return Hib.query(session -> {
            // 重新加载一次用户信息到self中，和当前的session绑定
            session.load(self, self.getId());

            // 获取我关注的人
            Set<UserFollow> flows = self.getFollowing();

            return flows.stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toList());

        });
    }

    /**
     * 好友添加
     * @param origin 发起者
     * @param target 被添加的人
     * @param alias 备注名
     * @return
     */
    public static User follow(final User origin, final User target, final String alias) {
        UserFollow userFollow = getUserFollow(origin, target);
        if (userFollow != null){
            return userFollow.getTarget();
        }
        return Hib.query(session -> {
            // 操作懒加载的数据，需要重新load一次
            session.load(origin, origin.getId());
            session.load(target, target.getId());

            // 我关注人的时候，同时他也关注我，
            // 所有需要添加两条UserFollow数据
            UserFollow originFollow = new UserFollow();
            originFollow.setOrigin(origin);
            originFollow.setTarget(target);
            // 备注是我对他的备注，他对我默认没有备注
            originFollow.setAlias(alias);

            // 发起者是他，我是被关注的人的记录
            UserFollow targetFollow = new UserFollow();
            targetFollow.setOrigin(target);
            targetFollow.setTarget(origin);

            // 保存数据库
            session.save(originFollow);
            session.save(targetFollow);

            return target;
        });
    }

    /**
     * 查询两个人是否已经是好友关系
     *
     * @param origin 发起者
     * @param target 被关注人
     * @return 返回中间类UserFollow
     */
    public static UserFollow getUserFollow(final User origin, final User target) {
        return Hib.query(session -> (UserFollow) session
                .createQuery("from UserFollow where originId = :originId and targetId = :targetId")
                .setParameter("originId", origin.getId())
                .setParameter("targetId", target.getId())
                .setMaxResults(1)
                // 唯一查询返回
                .uniqueResult());
    }

    /**
     * 查询联系人
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<User> search(String name) {
        if (Strings.isNullOrEmpty(name)) name = "";
        final String searchName = "%" + name + "%";
        return Hib.query(session -> (List<User>) session.createQuery(
                "from User where lower(name) like :name and portrait is not null and description is not null")
                .setParameter("name", searchName)
                .setMaxResults(20) // 至多20条
                .list());
    }
}
