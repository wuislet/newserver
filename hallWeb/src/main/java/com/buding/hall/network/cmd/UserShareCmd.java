package com.buding.hall.network.cmd;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import packet.msgbase.MsgBase.PacketType;
import packet.user.User.ShareRequest;

import com.buding.db.model.User;
import com.buding.hall.helper.HallPushHelper;
import com.buding.hall.module.shop.channel.ChannelRepostory;
import com.buding.hall.module.user.service.UserService;
import com.buding.hall.network.HallSession;

@Component
public class UserShareCmd extends HallCmd {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected ChannelRepostory channelRepostory;
	
	@Autowired
	UserService userService;
	
	@Autowired
	HallPushHelper pushHelper;
	
	@Override
	public void execute(CmdData data) throws Exception {
		ShareRequest req = ShareRequest.parseFrom(data.packet.getData());
		
		int userId = data.session.userId;
		HallSession session = data.session;
		if(userId == 0) {
			pushHelper.pushErrorMsg(session, PacketType.GlobalMsgSyn, "没有登录");
			return;
		}
		
		userService.doShare(userId);
		
		pushHelper.pushUserInfoSyn(userId);
	}

	@Override
	public PacketType getKey() {
		return PacketType.ShareRequest;
	}	
}
