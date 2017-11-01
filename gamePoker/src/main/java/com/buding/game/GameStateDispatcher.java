package com.buding.game;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buding.api.desk.Desk;
import com.buding.card.ICardLogic;
import com.buding.game.events.DispatchEvent;
import com.buding.game.events.NetEvent;
import com.buding.game.events.PlatformEvent;
import com.buding.game.events.PlayerEvent;
import com.buding.game.events.TimerEvent;
import com.google.gson.Gson;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public abstract class GameStateDispatcher<T extends Desk> {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected GameTimerMgr mTimerMgr = new GameTimerMgr();

	protected GameData mGameData = new GameData();

	public void setGameParam(String param) {
		if(StringUtils.isBlank(param)) {
			return;
		}
		this.mGameData.mGameParam = new Gson().fromJson(param, GameParam.class);
		logger.info("GameParam:"+param);
	}

	/*
	 * 
	 */
	public void HandlePlatformEvent(PlatformEvent event) {

		if (null == this.m_currentState) {
			this.logger.error("HandlePlatformEvent currentstate is null");
			return;
		}

		this.m_currentState.onPlatform(event);

	}

	public void HandlePlayerEvent(PlayerEvent event) {
		if (null == this.m_currentState) {
			this.logger.error("HandlePlayerEvent currentstate is null");
			return;
		}

		this.m_currentState.onPlayer(event);

	}

	public void HandleNetEvent(NetEvent event) {
		if (null == this.m_currentState) {
			this.logger.error("HandleNetEvent currentstate is null");
			return;
		}

		this.m_currentState.onNet(event);
	}
	
	public void setGamingDate(String gameData) {
		if (null == this.m_currentState) {
			this.logger.error("HandleNetEvent currentstate is null");
			return;
		}
		this.m_currentState.setGamingDate(gameData);
	}

	public void HandleTimerEvent(TimerEvent event) {
//		this.logger.info("HandleTimerEvent timerid is " + event.timerID);

		if (event.timerID == this.mTimerMgr.GetDeskTimerID()) {
			this.m_currentState.onDeskTimer();
		} else {
			for (int position = 0; position < GameConstants.MyGame_Max_Players_Count; position++) {
				if (event.timerID == this.mTimerMgr.getPlayerTimerID(position)) {
					this.m_currentState.onPlayerTimerEvent(position);
					break;
				}
			}
			this.logger.error("HandleTimerEvent can not find one way out");
		}
	}

	protected ICardLogic mCardLogic;
	protected GameCardDealer mCardDealer;
	
	private GameState m_currentState = null;
	protected Map<Integer, GameState> mStateList = new HashMap<Integer, GameState>();

	public void StateDispatch(DispatchEvent event){
		if(mStateList.containsKey(event.eventID)){
			GameState state = mStateList.get(event.eventID);
			this.goTo(state);
		} else {
			this.logger.error("no state to goto:" + event.eventID);
		}
	}
	
	public void SetDesk(T desk) {
		this.mTimerMgr.Init(desk);
		this.mCardLogic.init(this.mGameData, desk);
		this.mCardDealer.Init(this.mGameData,desk,this.mCardLogic);
		
		for(GameState state : mStateList.values()){
			state.Init(desk, this.mTimerMgr, this, this.mGameData, this.mCardLogic, this.mCardDealer);
		}
		
		////初始状态 
		this.goTo(this.mStateList.get(1));
	}

	// //////状态转换函数，子类不能覆盖，只能调用
	protected void goTo(GameState<T> pNextState) {
		if (null == pNextState) {
			this.logger.error("pNextState should not be null");
			return;
		}

		// ///这里在转换状态之前需要kill定时器啊
		this.mTimerMgr.KillDeskTimer();

		// /上一个状态的exit
		if (null != this.m_currentState) {
			this.m_currentState.onExit();
		}

		this.m_currentState = pNextState;

		// //下一个状态
		this.m_currentState.onEnter();
	}
}
