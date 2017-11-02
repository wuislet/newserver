package com.buding.mj.common;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buding.api.player.PlayerInfo;
import com.buding.game.GameData;
import com.buding.mj.common.BaseMJRule;
import com.buding.mj.common.MJContext;
import com.buding.mj.common.MjCheckResult;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.helper.MJHelper;
import com.buding.mj.model.ActionWaitingModel;
import com.buding.mj.model.ChuTingModel;
import com.buding.mj.model.MjCheckContext;
import com.google.gson.Gson;

public class MJProcessor { //TODO WXD 实装    MjCheckContext
	private Logger logger = LoggerFactory.getLogger(getClass());
	protected BaseMJRule mjRule = new BaseMJRule();
	
	public MJProcessor() {

	}

	// 碰
	public ActionWaitingModel check_peng(GameData gameData, byte card, PlayerInfo pl) {
		int cd = card & 0xff;
		ActionWaitingModel result = null;
		
		int same_card_num = gameData.getXCardNumInHand(card, pl.position);

		if (same_card_num >= 2) {
			result = new ActionWaitingModel();
			result.targetCard = card;
			result.peng_card_value = cd | (cd << 8);
			result.playerTableIndex = pl.position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_PENG;
		}
		return result;
	}
	
	// 暗杠
	public ActionWaitingModel check_an_gang(GameData gameData, PlayerInfo pl) {
		ActionWaitingModel result = null;
		
		List<Byte> cardsInHand = gameData.getCardsInHand(pl.position);
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			int same_card_num = gameData.getXCardNumInHand(card, pl.position);
			if (same_card_num >= 4) {
				if(result == null) {
					result = new ActionWaitingModel();
					result.playerTableIndex = pl.position;
					result.opertaion = MJConstants.MAHJONG_OPERTAION_AN_GANG;
				}
				result.gangList.add(card);
			}
		}
		return result;
	}
	
	// 补杠
	public ActionWaitingModel check_bu_gang(GameData gameData, PlayerInfo pl) {
		ActionWaitingModel result = null;
		
		List<Byte> cardsInHand = gameData.getCardsInHand(pl.position);
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if (gameData.isPengCard(card, pl.position)) {
				if(result == null) {
					result = new ActionWaitingModel();
					result.playerTableIndex = pl.position;
					result.opertaion = MJConstants.MAHJONG_OPERTAION_AN_GANG;
				}
				result.gangList.add(card);
			}
		}
		return result;
	}
	
	// 直杠
	public ActionWaitingModel check_zhi_gang(GameData gameData, byte card, PlayerInfo pl) {
		ActionWaitingModel result = null;
		
		int same_card_num = gameData.getXCardNumInHand(card, pl.position);
		if (same_card_num >= 3) {
			result = new ActionWaitingModel();
			result.gangList.add(card);
			result.playerTableIndex = pl.position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_ZHI_GANG;
		}
		return result;
	}

	public boolean checkHuBaseRule(List<Byte> handcards, List<Integer> cardsDown, MjCheckContext ctx) {
		return true;
	}

	public MjCheckResult canTingThisCard(List<Byte> handCards, List<Integer> cardsDown,  byte card2Remove, byte card2Ting, MjCheckContext ctx) {
		List<Byte> list = new ArrayList<Byte>();
		list.addAll(handCards);
		list.remove((Object) card2Remove);// 打出这一张牌
		
		//打出一张牌后，依然要满足基本上听条件
		if(!checkTingBaseRule(list, cardsDown, ctx)) {
			logger.info("checkHu:ting={};remove={};result={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), "基本检测不通过");
			return null;
		}
		
		MJHelper.add2SortedList(card2Ting, list);// 加一张牌
		logger.info("checkHu:ting={};remove={};hand={};down={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), MJHelper.getSingleCardListName(list), MJHelper.getCompositeCardListName(cardsDown));
		
		if(checkHuBaseRule(list, cardsDown, ctx) == false) {
			logger.info("checkHu:ting={};remove={};result={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), "基本检测不通过");
			return null;
		}
				
		MJContext c = new MJContext();
		c.cardsInCard.addAll(list);
		return mjRule.canHu(c);
	}

	public ChuTingModel canTingInternal(List<Byte> handCards, List<Integer> cardsDown, MjCheckContext c) {
		Map<Byte, Set<Byte>> chuAndTingMap = new HashMap<Byte, Set<Byte>>();
		Set<Byte> allCards = MJHelper.getAllUniqCard();
		Set<Byte> set = MJHelper.getUniqCardList(handCards);
		for (byte card2Remove : set) {
			if(c.card2Remove > 0 && c.card2Remove != card2Remove) {
				continue; //只检查指定情况的
			}
			for (byte card2Ting : allCards) {
				if(c.card2Ting > 0 && c.card2Ting != card2Ting) {
					continue;//检查指定情况的
				}
				boolean canTingAndCheckRule = canTingAndCheckRule(handCards, cardsDown, card2Remove, card2Ting,c);
				if(!canTingAndCheckRule) {
					continue;
				}
				// 可以胡
				if (chuAndTingMap.get(card2Remove) == null) {
					chuAndTingMap.put(card2Remove, new HashSet<Byte>());
				}
				chuAndTingMap.get(card2Remove).add(card2Ting);
			}
		}
		ChuTingModel model = new ChuTingModel();
		model.chuAndTingMap = chuAndTingMap;
		return model;
	}

	/**
	 * 校验是否可以听牌
	 * 1.选举出所有可以成为将牌的牌
	 * 2.遍历将牌集合,删除他可以成牌.
	 * @param handCards 手牌(包含即将打出去的牌)
	 * @param cardsDown 吃/碰/杠/粘的牌
	 * @param card2Remove 即将打出去的牌
	 * @param card2Ting 检测可以听的牌
	 * @return
	 */
	private boolean canTingAndCheckRule(List<Byte> handCards, List<Integer> cardsDown, byte card2Remove, byte card2Ting , MjCheckContext c) {
		List<Byte> jiangPaiList = new ArrayList<Byte>();
		List<Byte> shouPaiTemp = new ArrayList<Byte>();
		shouPaiTemp.addAll(handCards);
		shouPaiTemp.remove(Byte.valueOf(card2Remove+""));
		shouPaiTemp.add(card2Ting);
		Collections.sort(shouPaiTemp);
		Map<Byte, Byte> maps = new HashMap<Byte, Byte>();
		for(Byte b : shouPaiTemp){
			Byte t = maps.get(b);
			if(t == null){
				maps.put(b, (byte)1);
			} else{
				t++;
				maps.put(b, t);
			}
		}
		for(Map.Entry<Byte,Byte> e:maps.entrySet()){
			if(e.getValue()>=2){
				jiangPaiList.add(e.getKey());
			}
		}
		for(Byte b:jiangPaiList){
			List shouPaiTemp2 = new ArrayList();
			shouPaiTemp2.addAll(shouPaiTemp);
			shouPaiTemp2.remove(Byte.valueOf(b+""));
			shouPaiTemp2.remove(Byte.valueOf(b+""));
			if(mjRule.canChengPai(shouPaiTemp2,new MjCheckResult())){
				if(checkHuRule(shouPaiTemp,cardsDown,b)){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param handcards
	 * @param cardsDown
	 * @return
	 */
	private boolean checkHuRule(List<Byte> handcards, List<Integer> cardsDown , Byte b) {
		List shouPaiTemp = new ArrayList();
		shouPaiTemp.addAll(handcards);
		shouPaiTemp.remove(Byte.valueOf(b+"")); //去除将牌
		shouPaiTemp.remove(Byte.valueOf(b+""));
		return true;
	}

	protected MjCheckResult canHu(List<Byte> cards) {
		MJContext ctx = new MJContext();
		ctx.cardsInCard = cards;
		MjCheckResult ret = mjRule.canHu(ctx);
		return ret;
	}
	
	/**
	 * 判断是否是夹胡
	 */
	public boolean isJiaHu(List<Byte> handcards, byte card) {
		List<Byte> cards = new ArrayList<Byte>();
		cards.addAll(handcards);
		cards.add(card);
		Byte card2 = (byte) (card + 1);
		Byte card3 = (byte) (card - 1);
		if (cards.remove((Byte) card) && cards.remove(card2) && cards.remove(card3)) {
			if (canHu(cards) != null)
				return true;
		}
		return false;
	}

	public boolean canTing(MjCheckContext c) {
		List<Byte> handCards = new ArrayList<Byte>();
		handCards.addAll(c.gameData.mPlayerCards[c.position].cardsInHand);
		if (c.card != 0) {
			MJHelper.add2SortedList(c.card, handCards);
		}

		List<Integer> cardsDown = c.gameData.mPlayerCards[c.position].cardsDown;
		logger.info("act=canTing;handcards={};downcards={}", MJHelper.getSingleCardListName(handCards), MJHelper.getCompositeCardListName(cardsDown));

		if (!checkTingBaseRule(handCards, cardsDown, c)) {
			return false;
		}

		ChuTingModel model = canTingInternal(handCards, cardsDown, c);

		if (model == null || model.chuAndTingMap.isEmpty()) {
			return false;
		}
		
		c.gameData.mTingCards[c.position].chuAndTingMap = model.chuAndTingMap;
		logger.info("act=checkTing;deskId={};result={}", c.desk.getDeskID(), new Gson().toJson(model.chuAndTingMap));
		return true;
	}

	private boolean checkTingBaseRule(List<Byte> c1, List<Integer> c2, MjCheckContext c) {
		return true;
	}

	// 有没有“幺”或“九”。
	public boolean has19(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if(!MJHelper.isNormalCard(card)){
				continue;
			}
			int num = MJHelper.getCardNum(card);
			if (num == 1 || num == 9) {
				return true;
			}
		}

		for (int i = 0; i < cardsDown.size(); i++) {
			int card = cardsDown.get(i);
			byte card1 = (byte) (card & 0xff);
			byte card2 = (byte) ((card >> 8) & 0xff);
			byte card3 = (byte) ((card >> 16) & 0xff);
			if(!MJHelper.isNormalCard(card1)){
				continue;
			}
			int num1 = MJHelper.getCardNum(card1);
			int num2 = MJHelper.getCardNum(card2);
			int num3 = MJHelper.getCardNum(card3);
			if (num1 == 1 || num1 == 9 || num2 == 1 || num2 == 9 || num3 == 1 || num3 == 9) {
				return true;
			}
		}
		return false;
	}

	// 是否有3张一样的“刻”牌
	public boolean hasKe(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size() - 2; i++) {
			Byte card1 = cardsInHand.get(i);
			Byte card2 = cardsInHand.get(i + 1);
			Byte card3 = cardsInHand.get(i + 2);
			if (card1 == card2 && card2 == card3) {
				return true;
			}
		}
		
		for (int i = 0; i < cardsDown.size(); i++) {
			int card = cardsDown.get(i);
			byte card1 = (byte) (card & 0xff);
			byte card2 = (byte) ((card >> 8) & 0xff);
			byte card3 = (byte) ((card >> 16) & 0xff);

			if (card1 == card2 && card2 == card3) {
				return true;
			}
		}
		return false;
	}

	// 是否有3张连在一起的“顺”牌
	public boolean hasShun(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size() - 2; i++) {//TODO WXD 前置条件是排好序。
			Byte card = cardsInHand.get(i);
			if(!MJHelper.isNormalCard(card)){
				continue;
			}
			Byte card1 = cardsInHand.get(i + 1);
			Byte card2 = cardsInHand.get(i + 2);
			if (card + 1 == card1 && card + 2 == card2) { // 123,有序
				return true;
			}
		}

		for (int i = 0; i < cardsDown.size(); i++) {
			int card = cardsDown.get(i);
			byte card1 = (byte) (card & 0xff);
			byte card2 = (byte) ((card >> 8) & 0xff);
			byte card3 = (byte) ((card >> 16) & 0xff);
			if (card1 + 1 == card2 && card1 + 2 == card3) {// 123,有序
				return true;
			}
			if (card1 == card2 + 1 && card1 == card3 + 2) {// 321,有序
				return true;
			}
		}
		return false;
	}

	public int getColorNumber(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int color_flag = 0;
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if (!MJHelper.isNormalCard(card)) {
				continue;
			}
			int color = MJHelper.getCardColor(card);
			color_flag |= (1 << color);
		}
		
		for (int i = 0; i < cardsDown.size(); i++) {
			byte card = (byte)(cardsDown.get(i) & 0xff);
			if (!MJHelper.isNormalCard(card)) {
				continue;
			}
			if(card == (byte)MJConstants.MAHJONG_CODE_GANG_CARD){
				continue;
			}
			int color = MJHelper.getCardColor(card);
			color_flag |= (1 << color);
		}
		return color_flag;
	}
	
	public boolean has0Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		return (flag == 0);
	}
	
	public boolean has1Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		return (flag == 1) || (flag == 2) || (flag == 4);
	}

	public boolean has2Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		return (flag == 3) || (flag == 5) || (flag == 6);
	}

	public boolean has3Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		return (flag == 7);
	}
}
