package com.buding.mj.model;

import java.util.ArrayList;
import java.util.List;
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
	
	public List<Byte> cardCantRemove = new ArrayList<Byte>();
	public List<Byte> cardCantTing = new ArrayList<Byte>();

	public MjCheckContext() {
	}
	
	public MjCheckContext(GameData gameData, T desk, byte card, int position) {
		this.gameData = gameData;
		this.desk = desk;
		this.card = card;
		this.position = position;
	}
}
