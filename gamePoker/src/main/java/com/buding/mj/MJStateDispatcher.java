package com.buding.mj;

import java.util.HashMap;
import java.util.Map;
import com.buding.api.desk.MJDesk;
import com.buding.card.ICardLogic;
import com.buding.game.GameCardDealer;
import com.buding.game.GameState;
import com.buding.game.GameStateDispatcher;
import com.buding.game.events.DispatchEvent;
import com.buding.mj.common.MJCardDealer;
import com.buding.mj.common.MJCardLogic;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.states.MJStateDeal;
import com.buding.mj.states.MJStateFinish;
import com.buding.mj.states.MJStateOriginCard;
import com.buding.mj.states.MJStatePrapareDeal;
import com.buding.mj.states.MJStateReady;
import com.buding.mj.states.MJStateRun;

@SuppressWarnings("all")
public class MJStateDispatcher<T extends MJDesk> extends GameStateDispatcher<T> {
	private MJStateReady mStateReady = new MJStateReady();                    //准备阶段
	private MJStatePrapareDeal mPrapareDeal = new MJStatePrapareDeal();       //准备发牌阶段
	private MJStateDeal mStateDeal = new MJStateDeal();                       //发牌阶段
	private MJStateOriginCard mStateOriginCard = new MJStateOriginCard();     //初始手牌阶段
	private MJStateRun mStateRun = new MJStateRun();                          //游戏阶段
	private MJStateFinish mStateFinish = new MJStateFinish();                 //结算阶段
	
	public MJStateDispatcher(){
		this.mCardDealer = new MJCardDealer();
		this.mCardLogic = new MJCardLogic();
		this.mStateList.put(MJConstants.MJStateReady, mStateReady);
		this.mStateList.put(MJConstants.MJStatePrapareDeal, mPrapareDeal);
		this.mStateList.put(MJConstants.MJStateDeal, mStateDeal);
		this.mStateList.put(MJConstants.MJStateOriginCard, mStateOriginCard);
		this.mStateList.put(MJConstants.MJStateRun, mStateRun);
		this.mStateList.put(MJConstants.MJStateFinish, mStateFinish);
	}
}
