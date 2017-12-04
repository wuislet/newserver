package com.buding.hall.network.cmd;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import packet.msgbase.MsgBase.PacketBase;
import packet.msgbase.MsgBase.PacketType;
import packet.user.User.LoginRequest;

import com.buding.common.cache.RedisClient;
import com.buding.common.result.Result;
import com.buding.common.token.TokenServer;
import com.buding.db.model.User;
import com.buding.hall.helper.HallPushHelper;
import com.buding.hall.module.common.constants.ClientType;
import com.buding.hall.module.common.constants.UserType;
import com.buding.hall.module.user.dao.UserDao;
import com.buding.hall.module.user.helper.UserSecurityHelper;
import com.buding.hall.module.user.service.UserService;
import com.buding.hall.network.HallSessionManager;
import com.ifp.wechat.entity.user.UserWeiXin;
import com.ifp.wechat.service.OAuthService;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
@Component
public class LoginCmd extends HallCmd {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	UserService userService;
	
	@Autowired
	UserDao userDao;

	@Autowired
	TokenServer tokenServer;

	@Autowired
	UserSecurityHelper userSecurityHelper;
	
	@Autowired
	HallSessionManager hallSessionManager;
	
	@Autowired
	HallPushHelper pushHelper;
	
	@Autowired
	RedisClient redisClient;

	@Override
	public void execute(CmdData data) throws Exception {	
		PacketBase packet = data.packet;
		LoginRequest ur = LoginRequest.parseFrom(packet.getData());
		
		logger.info("login cmd, username={}; passwd={}; ip = {}", ur.getUsername(), ur.getPassward(), ur.getIp());

		if (StringUtils.isBlank(ur.getUsername()) || StringUtils.isBlank(ur.getPassward())) {
			pushHelper.pushErrorMsg(data.session, packet.getPacketType(), "缺少参数");
			return;
		}
		
		User user = null;
		
		//微信登录
		if (ur.getType() == ClientType.WEIXIN) {
			logger.info("wx login ");
			UserWeiXin wxUser = OAuthService.getUserInfoOauth(ur.getPassward(), ur.getUsername());
			if(wxUser == null) {
				logger.info("wx user is null ");
				pushHelper.pushErrorMsg(data.session, packet.getPacketType(), "微信登录失败,用户不存在");
				return;
			}
			user = userService.getByUserName(wxUser.getOpenid());
			if(user == null) {//微信首次登陆的注册
				user = userService.initUser();
				user.setUserName(wxUser.getOpenid());
				user.setPasswd(ur.getPassward());
				user.setUserType(UserType.WX_USER);
				user.setNickname("微信用户");
				user.setDeviceType(ur.getDeviceFlag());
				if(StringUtils.isNotBlank(wxUser.getHeadimgurl())) {
					user.setHeadImg(wxUser.getHeadimgurl());
				}
				if(wxUser.getSex() != 0) {
					user.setGender(wxUser.getSex());
				}
				Result result = userService.register(user);
				if(result.isFail()) {
					logger.info("register wx user null ");
					pushHelper.pushErrorMsg(data.session, packet.getPacketType(), "微信登录失败,用户不存在");
					return;
				}
			}
			if(StringUtils.isNotBlank(wxUser.getHeadimgurl())) {
				user.setHeadImg(wxUser.getHeadimgurl());
			}
			if(wxUser.getSex() != 0) {
				user.setGender(wxUser.getSex());
			}
			if(StringUtils.isNotBlank(wxUser.getNickname())) {				
				user.setNickname(wxUser.getNickname());
			}
			userService.updateUser(user);
			logger.info("login wx ok");
		} else {
			//用户名密码登录
			String name = ur.getUsername();
			String pwd = ur.getPassward();
			
			user = userService.login(name, pwd);
			if (user == null) {
				logger.error("act=hallLoginFailUserNotFound;username={}", ur.getUsername());
				pushHelper.pushErrorMsg(data.session, packet.getPacketType(), "登录失败,用户名或密码错误");
				return;
			}

			if (StringUtils.isNotBlank(user.getBindedMatch())) {
				logger.error("act=hallLoginFailRobotNotAllow;username={}", ur.getUsername());
				pushHelper.pushErrorMsg(data.session, packet.getPacketType(), "非法的用户登录");
				return;
			}
		}
		user.setIp(ur.getIp());
		
		// 更新token
		String token = tokenServer.updateToken(user.getId(), false);
		
		data.session.userId = user.getId();
		
		hallSessionManager.removeFromAnonymousList(data.session.getSessionId());
		hallSessionManager.put2OnlineList(data.session.userId, data.session);
		
		Set<String> hallServer = redisClient.zrange("serverSet_hall", 0, 0);
		Set<String> msgServer = redisClient.zrange("serverSet_msg", 0, 0);
		Set<String> battleServer = redisClient.zrange("serverSet_battle", 0, 0);
		
		String hallAddr = hallServer.iterator().next();
		String msgAddr ="default_192.168.10.100:4000" ;//msgServer.iterator().next();
		String battleAddr ="default_192.168.10.100:7000";battleServer.iterator().next();
		pushHelper.pushLoginRsp(data.session, user, token, msgAddr.split("_")[1], battleAddr.split("_")[1]);

		pushHelper.pushUserInfoSyn(user.getId());
		
		userService.onUserLogin(user);
	}

	@Override
	public PacketType getKey() {
		return PacketType.LoginRequest;
	}

}
