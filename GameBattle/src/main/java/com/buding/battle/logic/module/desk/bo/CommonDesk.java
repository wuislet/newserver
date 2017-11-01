package com.buding.battle.logic.module.desk.bo;

import java.util.List;

import com.buding.api.desk.Desk;
import com.buding.api.player.PlayerInfo;
import com.buding.battle.logic.module.common.BattleContext;
import com.buding.battle.logic.module.common.DeskStatus;
import com.buding.battle.logic.module.common.ParentAware;
import com.buding.battle.logic.module.room.bo.Room;
import com.buding.common.monitor.Monitorable;
import com.buding.hall.config.DeskConfig;
import com.buding.hall.module.game.model.DeskModel;

public interface CommonDesk<MsgType> extends Desk<MsgType>, Monitorable, ParentAware<Room> {   
	/**
	 * 玩家就位,返回玩家的座位索引
	 * @param playerId
	 */
	public int playerSit(BattleContext ctx);
	
	/**
	 * 玩家离开
	 * @param playerId
	 */
	@Deprecated
	public void playerExit(int playerId, PlayerExitType type);
	
	/**
	 * 销毁
	 */
	public void destroy(DeskDestoryReason type);
	
	/**
	 * 桌子是否已空
	 * @return
	 */
	public boolean isEmpty();
	
	/**
	 * 桌子是否已满
	 * @return
	 */
	public boolean isFull();
	
	/**
	 * 检查是否可以开赛
	 */
	public void tryStartGame();
	
	/**
	 * 收到用户准备数据包
	 * @param playerId
	 */
	public void onPlayerReadyPacketReceived(int playerId, int state, int phase);
	
	/**
	 * 换桌
	 * @param playerId
	 */
	public void onPlayerChangeDeskPacketReceived(int playerId);
	
	/**
	 * 重连
	 * @param playerId
	 */
	public void onPlayerReconnectPacketReceived(int playerId);
	
	/**
	 * 退出游戏
	 * @param playerId
	 */
	public void onPlayerExitPacketReceived(int playerId);
	
	/**
	 * 离开游戏
	 * @param playerId
	 */
	public void onPlayerAwayPacketReceived(int playerId);
	
	/**
	 * 回到游戏
	 * @param playerId
	 */
	public void onPlayerComeBackPacketReceived(int playerId);
	
	/**
	 * 断线
	 * @param playerId
	 */
	public void onPlayerOfflinePacketReceived(int playerId);
	
	/**
	 * 收到玩家解散请求
	 * @param playerId
	 */
	public void onPlayerDissVotePacketReceived(int playerId, boolean agree);
	
	/**
	 * 收到游戏数据包
	 * @param playerID
	 * @param content
	 */
	public void onGameMsgPacketReceived(int playerID, MsgType content);
	
	/**
	 * 收到聊天数据包
	 * @param playerID
	 * @param content
	 */
	public void onChatMsgPacketReceived(int playerID, int contentType, byte[] content);
	
	public void onPlayerHangupPacketReceived(int playerID);
	
	public void onPlayerCancelHangupPacketReceived(int playerID);
	
	/**
	 * 收到踢人数据包
	 * @param playerId
	 * @param targetPlayerId
	 */
	public void onKickoutPacketReceived(int playerId, int targetPlayerId);
	
	/**
	 * 获取玩家数量
	 * @return
	 */
	public int getPlayerCount();
	
	/**
	 * 获取桌子状态
	 * @return
	 */
	public DeskStatus getStatus();
	
	/**
	 * 是否自动换桌
	 * @return
	 */
	public boolean isAutoChangeDesk();
	
	/**
	 * 重置桌子状态
	 */
	public void reset();
	
	public List<PlayerInfo> getPlayers();
	
	public DeskConfig getDeskConfig();
	
	public void setDeskConfig(DeskConfig conf);
	
	public void setDeskOwner(int ownerId);
	
	public double getDeskDelayStatus();
	
	public void kickout(int playerId, String msg);
	
	public void onDismissPacketRequest();
	
	public void setDeskId(String id);
	
	public boolean isAutoReady();
	
	public void onSetGamingDataReq(String json);
	
	public void markAsAdminUse();
	
	public boolean isAdminUse();
	
	public String dumpGameData();
	
	public int getGameCount();
	
	public DeskModel getDeskInfo();
	
	public String printGameDetail();
	
	public boolean isHasPlayer(int playerId);
}
