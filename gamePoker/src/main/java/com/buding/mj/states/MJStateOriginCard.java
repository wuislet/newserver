package com.buding.mj.states;

import com.buding.api.player.PlayerInfo;
import com.buding.game.events.DispatchEvent;
import com.buding.game.events.GameLogicEvent;
import com.buding.game.events.NetEvent;
import com.buding.game.events.PlatformEvent;
import com.buding.game.events.PlayerEvent;
import com.buding.mj.constants.MJConstants;
import com.google.protobuf.InvalidProtocolBufferException;

import packet.mj.MJ.GameOperPlayerActionSyn;
import packet.mj.MJBase.GameOperation;

/**
 * @author islet
 * @Description: 初始手牌阶段
 * 
 */
public class MJStateOriginCard extends MJStateCommon {

	@Override
	public void handlePlayerStatusChange(int position) {
	}

	@Override
	public void handleReconnectFor(int position) {
		logger.info("act=handleReconnectFor;state=ready;position={}", position);
	}

	@Override
	public void onEnter() {
		System.out.println(" S=〉  enter  OriginCard");
		this.mGameTimer.SetDeskTimer(20000);
	}
	
	@Override
	public void onPlayer(PlayerEvent event) {
		this.logger.info(" S=〉 origincard onplayer = playerid is " + event.info.playerId + " position " + event.info.position + " eventid " + event.eventID + " is robot " + event.info.robot);

		switch (event.eventID) {

		case GameLogicEvent.Player_Agree:
			System.out.println("   !!!!!!  ready  " + event.info.playerId + "  - " + event.info.checkReadyPhase(event.phase) + " ? " + event.phase + " + " + event.state);
			boolean flag = true;
			for (PlayerInfo p : mDesk.getPlayers()){
				if (p.isRobot()){
					continue;
				}
				if(p.checkReadyPhase(2) == false){
					flag = false;
					break;
				}
			}
			if (flag) {
				this.mGameTimer.KillDeskTimer();
				this.mGameTimer.SetDeskTimer(100);
			}
			break;
		default :
			super.onPlayer(event);
			break;
		}
	}

	@Override
	public void onNet(NetEvent event) {
		try {
			byte[] data = (byte[]) event.msg;
			GameOperation p = GameOperation.parseFrom(data);
			switch (p.getOperType()) {
			case GameOperPlayerActionSyn: {
				GameOperPlayerActionSyn.Builder gb = GameOperPlayerActionSyn.newBuilder();
				gb.mergeFrom(p.getContent());
				gb.setPosition(event.position);
				mCardLogic.playerOperation(mGameData, mDesk, gb, mDesk.getDeskPlayer(gb.getPosition()));
			}
				break;
			default: {
				throw new RuntimeException("不支持的类型:" + p.getOperType());
			}
			}
		} catch (InvalidProtocolBufferException e) {
			logger.error("", e);
		}
	}

	@Override
	public void onDeskTimer() {
		this.logger.info(" S=〉 OriginCard End , onDeskTimer is called; deskId={}", mDesk.getDeskID());
		
		this.mGameTimer.KillDeskTimer();

		////跳转到发牌
		DispatchEvent event = new DispatchEvent();
		event.eventID = MJConstants.MJStateRun;
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
