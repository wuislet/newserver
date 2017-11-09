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
				int ib = (3 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + 2 * i - 1;
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

		List<Byte> tmpcards = new ArrayList<Byte>(); //TODO WXD cheat del
//		tmpcards.add((byte)1);
//		tmpcards.add((byte)1);
//		tmpcards.add((byte)1);
//		tmpcards.add((byte)2);
//		tmpcards.add((byte)3);
//		tmpcards.add((byte)4);
//		tmpcards.add((byte)5);
//		tmpcards.add((byte)6);
//		tmpcards.add((byte)7);
//		tmpcards.add((byte)8);
//		tmpcards.add((byte)9);
//		tmpcards.add((byte)9);
//		tmpcards.add((byte)9);

//		tmpcards.add((byte)1);
//		tmpcards.add((byte)1);
//		tmpcards.add((byte)1);
//		tmpcards.add((byte)4);
//		tmpcards.add((byte)6);
//		tmpcards.add((byte)9);
//		tmpcards.add((byte)9);
//		tmpcards.add((byte)9);
//		tmpcards.add((byte)17);
//		tmpcards.add((byte)17);
//		tmpcards.add((byte)17);
//		tmpcards.add((byte)19);
//		tmpcards.add((byte)57);
		for(int i = 0; i < cards.size(); i++){
			tmpcards.add(cards.get(i));
		}
		this.mGameData.mDeskCard.cards.addAll(tmpcards);
	}
}
