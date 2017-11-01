package com.buding.mj.common;

import java.util.ArrayList;
import java.util.List;
import com.buding.api.desk.MJDesk;
import com.buding.game.GameCardDealer;
import com.buding.mj.constants.MJConstants;

/**
 * 
 * @author Administrator
 *
 */
public class DQMJCardDealer extends GameCardDealer<MJDesk<byte[]>> {

	@Override
	public void dealCard() {
		//洗牌
		this.washCards();
	}

	// 洗牌
	public void washCards() {
		this.mGameData.mDeskCard.reset();
		
		// 万, 条, 筒。
		List<Byte> cards = new ArrayList<Byte>();
		for (int j = 0; j < 3; j++) {
			for (int i = 1; i <= 9; i++) {
				int ib = (j << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
				byte b = (byte) (ib & 0xff);
				cards.add(b);
				cards.add(b);
				cards.add(b);
				cards.add(b);
			}
		}
		
		// 额外加红中
		byte hongzhong = (byte)MJConstants.MAHJONG_CODE_HONG_ZHONG;
		cards.add(hongzhong);
		cards.add(hongzhong);
		cards.add(hongzhong);
		cards.add(hongzhong);
		
		cards = new CardWasher().wash(cards);
		this.mGameData.mDeskCard.cards.addAll(cards);
	}
}
