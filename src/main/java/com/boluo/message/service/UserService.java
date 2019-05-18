package com.boluo.message.service;

import com.boluo.message.bean.api.base.ResponseModel;
import com.boluo.message.bean.api.user.UpdateInfoModel;
import com.boluo.message.bean.card.UserCard;
import com.boluo.message.bean.db.User;
import com.boluo.message.factory.UserFactory;
import org.hibernate.sql.Update;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
@Path("user")
public class UserService extends BaseService {
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
}
