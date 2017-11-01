package com.buding.mj.model;

import com.buding.api.desk.DQMJDesk;
import com.buding.game.GameData;

/**
 * @author wuislet
 * @Description:
 * 检测听牌、胡牌的上下文
 * 
 */
public class DQMjCheckContext extends MjCheckContext<DQMJDesk> {
	public boolean isQiangTing = false;
	
	public DQMjCheckContext() {
		super();
	}
	
	public DQMjCheckContext(GameData gameData, DQMJDesk desk, byte card, int position, boolean isQiangTing) {
		super(gameData, desk, card, position);
		this.isQiangTing = isQiangTing;
	}
}
