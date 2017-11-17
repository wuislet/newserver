package com.buding.mj.common;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buding.game.GameData;
import com.buding.mj.common.BaseMJRule;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.helper.MJHelper;
import com.buding.mj.model.ActionWaitingModel;
import com.buding.mj.model.ChuTingModel;
import com.buding.mj.model.MjCheckContext;
import com.google.gson.Gson;

public class MJProcessor {
	private Logger logger = LoggerFactory.getLogger(getClass());
	protected BaseMJRule mjRule = new BaseMJRule();
	
	public MJProcessor() {

	}

	// 碰
	public ActionWaitingModel check_peng(GameData gameData, byte card, int position) {
		ActionWaitingModel result = null;
		
		int same_card_num = gameData.getXCardNumInHand(card, position);

		if (same_card_num >= 2) {
			result = new ActionWaitingModel();
			result.targetCard = card;
			result.peng_card_value = card;
			result.playerTableIndex = position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_PENG;
		}
		return result;
	}
	
	// 暗杠
	public ActionWaitingModel check_an_gang(GameData gameData, byte newCard, int position) {
		ActionWaitingModel result = null;

		List<Byte> cardsInHand = gameData.getCardsInHand(position);
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			int same_card_num = gameData.getXCardNumInHand(card, position);
			if ((same_card_num >= 4) || (same_card_num == 3 && card == newCard)) {//手里有4张 || 手里有3张跟新摸的牌一样点数的牌。
				if(result == null) {
					result = new ActionWaitingModel();
					result.playerTableIndex = position;
					result.opertaion = MJConstants.MAHJONG_OPERTAION_AN_GANG;
				}
				result.gangList.add(card);
			}
		}
		return result;
	}
	
	// 补杠
	public ActionWaitingModel check_bu_gang(GameData gameData, byte newCard, int position) {
		ActionWaitingModel result = null;

		List<Byte> cardsInHand = new ArrayList<Byte>();
		cardsInHand.addAll(gameData.getCardsInHand(position));
		if(newCard > 0) {
			cardsInHand.add(newCard);
		}
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if (gameData.isPengCard(card, position)) {
				if(result == null) {
					result = new ActionWaitingModel();
					result.playerTableIndex = position;
					result.opertaion = MJConstants.MAHJONG_OPERTAION_AN_GANG;
				}
				result.gangList.add(card);
			}
		}
		return result;
	}
	
	// 直杠
	public ActionWaitingModel check_zhi_gang(GameData gameData, byte card, int position) {
		ActionWaitingModel result = null;
		
		int same_card_num = gameData.getXCardNumInHand(card, position);
		if (same_card_num >= 3) {
			result = new ActionWaitingModel();
			result.gangList.add(card);
			result.playerTableIndex = position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_ZHI_GANG;
		}
		return result;
	}
	
	// =============== 听牌 ===============
	/**
	 * 检查听的基础的前置规则
	 * @param handcards  把手牌跟新牌放入另一个数组，并在这传进来，避免影响全局数据。
	 * @param ctx        全局数据。
	 * @return
	 */
	protected boolean checkTingBaseRule(List<Byte> handcards, MjCheckContext ctx) {
		return true;
	}
	
	public boolean canTing(MjCheckContext ctx) {
		List<Byte> handCards = new ArrayList<Byte>();
		handCards.addAll(ctx.gameData.getCardsInHand(ctx.position));
		if (ctx.card != 0) {
			MJHelper.add2SortedList(ctx.card, handCards);
		}

		List<Integer> cardsDown = ctx.gameData.getCardsDown(ctx.position);
		logger.info("act=canTing;handcards={};downcards={}", MJHelper.getSingleCardListName(handCards), MJHelper.getCompositeCardListName(cardsDown));

		if (!checkTingBaseRule(handCards, ctx)) {
			return false;
		}

		ChuTingModel model = canTingInternal(handCards, ctx);

		if (model == null || model.chuAndTingMap.isEmpty()) {
			return false;
		}
		
		ctx.gameData.mTingCards[ctx.position].chuAndTingMap = model.chuAndTingMap;
		logger.info("act=checkTing;deskId={};result={}", ctx.desk.getDeskID(), new Gson().toJson(model.chuAndTingMap));
		return true;
	}

	public ChuTingModel canTingInternal(List<Byte> handCards, MjCheckContext ctx) {
		List<Integer> cardsDown = ctx.gameData.getCardsDown(ctx.position);
		Map<Byte, Set<Byte>> chuAndTingMap = new HashMap<Byte, Set<Byte>>();
		Set<Byte> allCards = MJHelper.getAllUniqCard();
		Set<Byte> set = MJHelper.getUniqCardList(handCards);
		for (byte card2Remove : set) { //测试打掉每个牌
			if(ctx.cardCantRemove.contains(card2Remove)) {
				continue; //只检查指定情况的
			}
			for (byte card2Ting : allCards) {  //测试每种要听的牌
				if(ctx.cardCantTing.contains(card2Ting)) {
					continue;//检查指定情况的
				}

				List<Byte> shouPaiTemp = new ArrayList<Byte>();
				shouPaiTemp.addAll(handCards);
				shouPaiTemp.remove((Byte)card2Remove);
				MJHelper.add2SortedList(card2Ting, shouPaiTemp);
				if(!finalCheckHu(shouPaiTemp, ctx)) { //不可以胡
					continue;
				}
				
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
	
	// =============== 胡牌 ===============
	/**
	 * 检查胡的基础的前置规则
	 * @param handcards  把手牌跟新牌放入另一个数组，并在这传进来，避免影响全局数据。
	 * @param ctx        全局数据。
	 * @return
	 */
	protected boolean checkHuBaseRule(List<Byte> handcards, MjCheckContext ctx) {
		return true;
	}
	
	public boolean canHu(MjCheckContext ctx) {
		List<Byte> handCards = new ArrayList<Byte>();
		handCards.addAll(ctx.gameData.getCardsInHand(ctx.position));
		if (ctx.card != 0) {
			MJHelper.add2SortedList(ctx.card, handCards);
		}

		List<Integer> cardsDown = ctx.gameData.getCardsDown(ctx.position);
		logger.info("act=canHu;handcards={};downcards={}", MJHelper.getSingleCardListName(handCards), MJHelper.getCompositeCardListName(cardsDown));

		if (!checkHuBaseRule(handCards, ctx)) {
			return false;
		}
		
		logger.info("act=checkHu;deskId={};cards={};newcard={}", ctx.desk.getDeskID(), new Gson().toJson(handCards), ctx.card);
		return finalCheckHu(handCards, ctx);
	}
	
	/**
	 * 校验是否可以胡牌
	 * @param handCards 手牌
	 * @param newCard 假装加入的牌
	 * @return
	 */
	private boolean finalCheckHu(List<Byte> handCards, MjCheckContext ctx) {
		if(ctx.desk.canQiXiaoDui() && mjRule.canHuQiXiaoDui(handCards)){
			return true;
		}
		return mjRule.canHu(handCards, (int)ctx.gameData.guiCards.get(0)); //TODO wxd 目前只验证第一张鬼牌，须扩展多个鬼牌的情况。
	}
	
	// =============== 胡型检查相关 ===============
	/**
	 * 判断是否是夹胡
	 */
	public boolean isJiaHu(List<Byte> handcards, byte card) {
		List<Byte> cards = new ArrayList<Byte>();
		cards.addAll(handcards);
		Byte card1 = (byte) (card + 1);
		Byte card2 = (byte) (card - 1);
		if (cards.remove(card1) && cards.remove(card2)) {
			return mjRule.canHu(cards, -1);
		}
		return false;
	}
	
	/**
	 * 判断是否是单吊将
	 */
	public boolean isDanDiao(List<Byte> handcards, byte card) {
		List<Byte> cards = new ArrayList<Byte>();
		cards.addAll(handcards);
		if (cards.remove((Byte)card) && cards.remove((Byte)card)) {
			return mjRule.canChengPai(cards); //TODO WXd hutype 改成不使用成牌的方法。成牌不是对外接口。
		}
		return false;
	}
	
	public boolean isMenQing(List<Integer> cardsDown) {
		return cardsDown.size() <= 0;
	}
	
	public boolean isQiXiaoDui(List<Byte> handcards){
		return mjRule.canHuQiXiaoDui(handcards);
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
	
	public boolean hasOne2Nine(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int numFlag = 0;
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if (!MJHelper.isNormalCard(card)) {
				continue;
			}
			int num = MJHelper.getCardNum(card);
			numFlag |= (1 << num);
		}
		
		for (int i = 0; i < cardsDown.size(); i++) {
			byte card = (byte)(cardsDown.get(i) & 0xff);
			if (!MJHelper.isNormalCard(card)) {
				continue;
			}
			if(card == (byte)MJConstants.MAHJONG_CODE_GANG_CARD){
				continue;
			}
			int num = MJHelper.getCardNum(card);
			numFlag |= (1 << num);
		}
		return numFlag == 511; //(1 << 9 - 1)
	}

	public int getColorNumber(List<Byte> cardsInHand, List<Integer> cardsDown) { //TODO WXD 区分清混
		int colorFlag = 0;
		for (int i = 0; i < cardsInHand.size(); i++) {
			byte card = cardsInHand.get(i);
			if (!MJHelper.isNormalCard(card)) {
				continue;
			}
			int color = MJHelper.getCardColor(card);
			colorFlag |= (1 << color);
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
			colorFlag |= (1 << color);
		}
		return colorFlag;
	}
	
	public boolean hasZi(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		return ((flag & 8) != 0);
	}
	
	public boolean has0Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		flag &= 7; //忽略风字牌。
		return (flag == 0);
	}
	
	public boolean has1Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		flag &= 7; //忽略风字牌。
		return (flag == 1) || (flag == 2) || (flag == 4);
	}

	public boolean has2Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		flag &= 7; //忽略风字牌。
		return (flag == 3) || (flag == 5) || (flag == 6);
	}

	public boolean has3Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int flag = getColorNumber(cardsInHand, cardsDown);
		flag &= 7; //忽略风字牌。
		return (flag == 7);
	}
}
