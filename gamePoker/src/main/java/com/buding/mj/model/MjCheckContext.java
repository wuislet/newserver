package com.buding.mj.model;

import com.buding.api.desk.MJDesk;
import com.buding.game.GameData;

/**
 * @author wuislet
 * @Description:
 * 检测听牌、胡牌的上下文
 * 
 */
public class MjCheckContext<T extends MJDesk> {
	public GameData gameData;
	public T desk;
	public byte card;
	public int position;
	
	public byte card2Remove = 0;
	public byte card2Ting = 0;

	public MjCheckContext() {
	}
	
	public MjCheckContext(GameData gameData, T desk, byte card, int position) {
		this.gameData = gameData;
		this.desk = desk;
		this.card = card;
		this.position = position;
	}

	public void setCard2Remove(byte card2Remove) {
		this.card2Remove = card2Remove;
	}

	public byte getCard2Ting() {
		return card2Ting;
	}
}
