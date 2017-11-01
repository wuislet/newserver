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
public class MJCardDealer extends GameCardDealer<MJDesk<byte[]>> {

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

		int cardType = mDesk.getCardType();
		// 加风
		if((cardType & 0x1) != 0){
			for (int i = 1; i <= 7; i++) {
				int ib = (3 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i * 2 - 1;
				byte b = (byte) (ib & 0xff);
				cards.add(b);
				cards.add(b);
				cards.add(b);
				cards.add(b);
			}
		}
		
		// 加花
		if((cardType & 0x2) != 0){
			for (int i = 1; i <= 8; i++) {
				int ib = (4 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
				byte b = (byte) (ib & 0xff);
				cards.add(b);
			}
		}
		
		// 额外加鬼牌
		if((cardType & 0x4) != 0 && (mDesk.getGui() != -1)){
			byte gui = (byte)mDesk.getGui();
			cards.add(gui);
			cards.add(gui);
			cards.add(gui);
			cards.add(gui);
		}
		
		cards = new CardWasher().wash(cards);
		this.mGameData.mDeskCard.cards.addAll(cards);
	}
}
