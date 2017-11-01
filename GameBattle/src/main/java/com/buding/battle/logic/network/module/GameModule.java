package com.buding.battle.logic.network.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import packet.msgbase.MsgBase.PacketBase;

import com.buding.battle.common.network.session.BattleSession;
import com.buding.battle.logic.module.common.PushService;
import com.buding.battle.logic.module.desk.bo.CommonDesk;
import com.buding.battle.logic.module.game.service.GameService;
import com.buding.battle.logic.module.game.service.VipService;
import com.buding.battle.logic.module.user.service.LoginService;
import com.buding.battle.logic.network.BaseModule;

/**
 * 玩家进入准备状态开始,后续网络消息由此模块处理
 * 
 * @author Administrator
 * 
 */
@Component
public class GameModule extends BaseModule {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	GameService gameService;

	@Autowired
	PushService pushService;

	@Autowired
	LoginService loginService;

	@Autowired
	VipService vipService;

	@Override
	public void onGamePacketReceived(BattleSession session, PacketBase packet) {
		try {
			int playerId = session.getPlayerId();
			CommonDesk desk = session.getDesk();
			if (desk != null) {
				desk.onGameMsgPacketReceived(playerId, packet.getData().toByteArray());
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void onDumpGamePacketReceived(BattleSession session, PacketBase packet) throws Exception {
		CommonDesk desk = session.getDesk();
		if (desk != null) {
			desk.dumpGameData();
		}
	}
}
