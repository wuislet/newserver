package com.buding.card;

import packet.mj.MJ.GameOperPlayerActionSyn;

import com.buding.api.desk.Desk;
import com.buding.api.player.PlayerInfo;
import com.buding.game.GameCardDealer;
import com.buding.game.GameData;


public interface ICardLogic<T extends Desk> {
	
	//组件初始化
	public void init(GameData gameData, T desk);
		
	//调试接口
	public void handleSetGamingData(GameCardDealer mCardDealer, GameData gameData, T desk, String json);
	
	//主循环
	public void gameTick(GameData data, T desk);
	
	//计算应该的鬼牌
	public void setGuiCards(GameData data, T desk);
	
	//发牌
	public void sendCards(GameData data, T desk);
	
	//设置下一个玩家做庄家
	public void selectBanker(GameData data, T desk);
	
	//提示玩家出牌	
	public void player_chu_notify(GameData gameData, T desk);
	
	//重新通知玩家操作
	public void re_notify_current_operation_player(GameData gameData, T desk, int position);
	
	//玩家操作(出、碰、杠、听等)
	public void playerOperation(GameData gameData, T gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl);
	
	//计算胡型
	public int CalHuType(GameData gameData, T desk, PlayerInfo pl, byte newCard);

	//算翻数
	public int CalFanNum(int fanType);
	
	//是否打完一个圈
	public boolean isFinishQuan(GameData gameData, T gt);
	
	//服务器托管自动操作
	//public void playerAutoOper(GameData gameData, T gt, int position);
	
	//重新推送玩家数据,用于断线重连
	public void repushGameData(GameData gameData, T desk, int position);
}
