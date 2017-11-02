package com.buding.mj.states;

import com.buding.game.events.DispatchEvent;
import com.buding.game.events.GameLogicEvent;
import com.buding.game.events.PlayerEvent;
import com.buding.mj.constants.MJConstants;

/**
 * @author islet
 * @Description: 准备发牌阶段
 * 
 */
public class MJStatePrapareDeal extends MJStateCommon {

	@Override
	public void handlePlayerStatusChange(int position) {
	}

	@Override
	public void handleReconnectFor(int position) {
		logger.info("act=handleReconnectFor;state=ready;position={}", position);
	}

	@Override
	public void onEnter() {
		System.out.println(" S=〉  enter   PrapareDeal ");
		this.mGameTimer.SetDeskTimer(100);
	}

	@Override
	public void onPlayer(PlayerEvent event) {
		this.logger.info(" S=〉 praparedeal onplayer = playerid is " + event.info.playerId + " position " + event.info.position + " eventid " + event.eventID + " is robot " + event.info.robot);

		switch (event.eventID) {

		case GameLogicEvent.Player_Agree:
			System.out.println("     ready  " + event.info.playerId + "  - " + event.info.checkReadyPhase(1));
			break;
		default :
			super.onPlayer(event);
			break;
		}
	}

	@Override
	public void onDeskTimer() {
		this.logger.info(" S=〉 prapare Deal End , onDeskTimer is called; deskId={}", mDesk.getDeskID());
		
		this.mGameTimer.KillDeskTimer();

		////跳转到发牌
		DispatchEvent event = new DispatchEvent();
		event.eventID = MJConstants.MJStateDeal;
		this.mDispatcher.StateDispatch(event);
	}

	@Override
	public void onPlayerTimerEvent(int position) {
		
	}

	@Override
	public void onExit() {
		
	}

	@Override
	public void handlePlayerHangup(int position) {
		
	}

}
