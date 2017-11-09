package com.buding.mj.dqmj;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buding.api.desk.DQMJDesk;
import com.buding.api.player.PlayerInfo;
import com.buding.game.GameData;
import com.buding.mj.common.MJContext;
import com.buding.mj.common.MjCheckResult;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.helper.MJHelper;
import com.buding.mj.model.ActionWaitingModel;
import com.buding.mj.model.ChuTingModel;
import com.buding.mj.model.DQMjCheckContext;
import com.buding.mj.model.ZhiduiModel;
import com.google.gson.Gson;

public class DQMJProcessor {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public DQMJProcessor() {

	}

	// 碰
	public ActionWaitingModel check_peng(GameData gameData, byte card, PlayerInfo pl) {
		ActionWaitingModel result = null;
		//
		if (gameData.getCardNumInHand(pl.position) <= 4)
			return null;
		//
		int same_card_num = gameData.getXCardNumInHand(card, pl.position);

		if (same_card_num >= 2) {
			result = new ActionWaitingModel();
			result.targetCard = card;
			result.peng_card_value = card;
			result.playerTableIndex = pl.position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_PENG;
		}
		return result;
	}

	public boolean checkHuBaseRule(List<Byte> handcards, List<Integer> cardsDown, DQMjCheckContext ctx) {
		// 清一色不能胡
		if (has2Color(handcards, cardsDown) == false) {
			logger.info("act=checkHuBaseRule;desc=清一色不能胡;");
			return false;
		}
		//
		// 红中的牌数
		int hongzhong_num = getHongZhongNum(handcards, cardsDown);

		// 如果没有红中
		if (hongzhong_num == 0) { // 、红中就可代替这个“幺九牌条件”与“刻牌条件”。(即只要有一张红中，即可忽略以上第2、3条和牌和条件。例：单调红中)
			// 是否有1或9
			if (has19(handcards, cardsDown) == false) {
				logger.info("act=checkHuBaseRule;desc=幺九条件不能胡;");
				return false;
			}
			// 胡的时候一定有刻子
			if (hasKe(handcards, cardsDown) == false) {
				logger.info("act=checkHuBaseRule;desc=无刻子不能胡;");
				return false;
			}
		}
		// 没有顺子, 有红中也要有顺子才能胡
		if (hasShun(handcards, cardsDown) == false) {
			logger.info("act=checkHuBaseRule;desc=没有顺子不能胡;");
			return false;
		}
		// 门清不能胡
		if (!ctx.isQiangTing && isMenQing(cardsDown)) {
			logger.info("act=checkHuBaseRule;desc=门清不能胡;");
			return false;
		}
		return true;
	}

	/**
	 * 获取可以支队的牌
	 * @param handCards
	 * @param chuTingModel
	 * @return
	 * 1.遍历可以听的集合(key为可以出的牌--card2Remove,value为可以胡的牌的集合--tingList)
	 * 2.如果tingList少于两个,return
	 * 3,手牌去除出的牌,找到可以胡的牌的位置,位置大于2满足条件
	 */
	public ZhiduiModel getZhiduiCards(List<Byte> handCards, ChuTingModel chuTingModel) {
		ZhiduiModel model = new ZhiduiModel();
		for(byte card2Remove : chuTingModel.chuAndTingMap.keySet()) { //打出一张牌
			Set<Byte> tingList = chuTingModel.chuAndTingMap.get(card2Remove);//需要听的牌
			if(tingList.size() < 2) {
				continue;
			}
			List<Byte> cards = new ArrayList<Byte>();
			cards.addAll(handCards);
			cards.remove((Object)card2Remove);
			Set<Integer> tmp = new HashSet<Integer>();
			for (byte card2Ting : tingList) {
				int count = MJHelper.getCardCount(cards, card2Ting);
				if(count >= 2) {
					tmp.add((int)card2Ting);
				}
			}
			if(tmp.size() >= 2) {
				model.chuAndZhiduiMap.put(card2Remove, tmp);
			}
			
		}
		return model;
	}
	
//  疑似遗弃。
//	public MjCheckResult canTingThisCard(List<Byte> handCards, List<Integer> cardsDown,  byte card2Remove, byte card2Ting, DQMjCheckContext ctx) {
//		List<Byte> list = new ArrayList<Byte>();
//		list.addAll(handCards);
//		list.remove((Object) card2Remove);// 打出这一张牌
//		
//		//打出一张牌后，依然要满足基本上听条件
//		if(checkTingBaseRule(list, cardsDown, ctx) == false) {
//			logger.info("checkHu:ting={};remove={};result={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), "基本检测不通过");
//			return null;
//		}
//		
//		MJHelper.add2SortedList(card2Ting, list);// 加一张牌
//		
//		logger.info("checkHu:ting={};remove={};hand={};down={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), MJHelper.getSingleCardListName(list), MJHelper.getCompositeCardListName(cardsDown));
//		
//		if(checkHuBaseRule(list, cardsDown, ctx) == false) {
//			logger.info("checkHu:ting={};remove={};result={};", MJHelper.getSingleCardName(card2Ting), MJHelper.getSingleCardName(card2Remove), "基本检测不通过");
//			return null;
//		}
//		
//		MjCheckResult ret = new MjCheckResult(); 
//		mjRule.canHu(list, ret);
//		DQMjCheckContext m = new DQMjCheckContext();
//		m.isQiangTing = true;
//		if(checkHuBaseRule(list,cardsDown,m)){
//			return ret;
//		}
//		return null;
//	}

	public ChuTingModel canTingInternal(List<Byte> handCards, List<Integer> cardsDown, DQMjCheckContext c) {
		Map<Byte, Set<Byte>> chuAndTingMap = new HashMap<Byte, Set<Byte>>();
		Set<Byte> allCards = MJHelper.getAllUniqCard();
		Set<Byte> set = MJHelper.getUniqCardList(handCards);
		for (byte card2Remove : set) {
			if(c.cardCantRemove.contains(card2Remove)) {
				continue; //只检查指定情况的
			}
			for (byte card2Ting : allCards) {
				if(c.cardCantTing.contains(card2Ting)) {
					continue;//检查指定情况的
				}
//				MjCheckResult ret = canTingThisCard(handCards, cardsDown, card2Remove, card2Ting, c);
				//重写
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
	private boolean canTingAndCheckRule(List<Byte> handCards, List<Integer> cardsDown, byte card2Remove, byte card2Ting , DQMjCheckContext c) {
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
		}
		return false;
	}

	/**
	 * 1.清一色,返回false
	 * 2.有两个红中或以上,返回true
	 * 3.有1,9且去除将牌后有顺有有碰
	 * @param handcards
	 * @param cardsDown
	 * @return
	 */
	private boolean checkHuRule(List<Byte> handcards, List<Integer> cardsDown , Byte b) {
		if (!has2Color(handcards, cardsDown)) {
			logger.info("act=checkHuBaseRule;desc=清一色不能胡;");
			return false;
		}
		int hongZhongNum = getHongZhongNum(handcards, cardsDown);
		if(hongZhongNum>=2){
			return true;
		}
		List shouPaiTemp = new ArrayList();
		shouPaiTemp.addAll(handcards);
		shouPaiTemp.remove(Byte.valueOf(b+""));
		shouPaiTemp.remove(Byte.valueOf(b+""));
		return has19(handcards, cardsDown)&&hasKe(shouPaiTemp,cardsDown)&&hasShun(shouPaiTemp,cardsDown);
	}
	
	/**
	 * 判断是否是夹胡
	 */
	public boolean isJiaHu(List<Byte> handcards, DQMJDesk desk, byte card, int zhidui, int position) {
		//夹胡
		{
			List<Byte> cards = new ArrayList<Byte>();
			cards.addAll(handcards);
			cards.add(card);
			Byte card2 = (byte) (card + 1);
			Byte card3 = (byte) (card - 1);
			if (cards.remove((Byte) card) && cards.remove(card2) && cards.remove(card3)) {
			}
		}

		// 3、7 夹
		if (desk.can37Jia()) {
			{
				List<Byte> cards = new ArrayList<Byte>();
				cards.addAll(handcards);
				cards.add(card);
				int num = (card & 0X0F);
				Byte card_1 = (byte) (card - 1);
				Byte card_2 = (byte) (card - 2);
				if (num == 3 && cards.remove((Byte) card) && cards.remove(card_1) && cards.remove(card_2)) {
				}
			}
			{
				List<Byte> cards = new ArrayList<Byte>();
				cards.addAll(handcards);
				cards.add(card);
				int num = (card & 0X0F);
				Byte card1 = (byte) (card + 1);
				Byte card2 = (byte) (card + 2);
				if (num == 7 && cards.remove((Byte) card) && cards.remove(card1) && cards.remove(card2)) {
				}
			}
		}

		// 单调夹
		if (desk.canDanDiaoJia()) {
			List<Byte> cards = new ArrayList<Byte>();
			cards.addAll(handcards);
			cards.add(card);
			if (cards.remove((Byte) card) && cards.remove((Byte) (card))) {
			}
		}

		// 支对夹
		if (desk.canZhiDuiHU()) {
			if(zhidui>0) return (zhidui & 0XFF) == card;
		}
		//对倒
		if(desk.canDuidao()||zhidui==0){
			for(List<Byte> temp:checkDuiDaoHu(handcards)){
				if(temp.contains(card)) return true;
			}
		}
		return false;
	}

	/**
	 * 校验对倒胡
	 * @param cards 不加胡的牌的手牌集合
	 * @return 返回对倒的集合
	 */
	public List<List<Byte>> checkDuiDaoHu(List<Byte> cards) {
		List<Byte> shouPaitemp = new ArrayList<Byte>();
		List<List<Byte>> resultList = new ArrayList<List<Byte>>();
		shouPaitemp.addAll(cards);
		Map<Byte, Byte> maps = new HashMap<Byte, Byte>();
		for(Byte b : shouPaitemp){
			Byte t = maps.get(b);
			if(t == null){
				maps.put(b, (byte)1);
			} else{
				t++;
				maps.put(b, t);
			}
		}
		List<Byte> duiDaoList = new ArrayList<Byte>();
		for(Map.Entry<Byte,Byte> e:maps.entrySet()){
			if(e.getValue()>=2){
				duiDaoList.add(e.getKey());
			}
		}
		if(duiDaoList.size()>=2){
			for(byte b:duiDaoList){
				List<Byte> duiDaoTemp = new ArrayList<Byte>();
				duiDaoTemp.addAll(duiDaoList);
				duiDaoTemp.remove(Byte.valueOf(b+""));
				for(Byte b1:duiDaoTemp){
					shouPaitemp.clear();
					shouPaitemp.addAll(cards);
					shouPaitemp.remove(Byte.valueOf(b+""));
					shouPaitemp.remove(Byte.valueOf(b+""));
					shouPaitemp.remove(Byte.valueOf(b1+""));
					shouPaitemp.remove(Byte.valueOf(b1+""));
				}
			}
		}
		System.out.println("校验胡牌对倒集合为==============="+resultList);
		return resultList;
	}
	public boolean canTing(DQMjCheckContext c) {
		List<Byte> handCards = new ArrayList<Byte>();
		handCards.addAll(c.gameData.mPlayerCards[c.position].cardsInHand);
		if (c.card != 0) {
			MJHelper.add2SortedList(c.card, handCards);
		}

		List<Integer> cardsDown = c.gameData.mPlayerCards[c.position].cardsDown;

		logger.info("act=canTing;handcards={};downcards={}", MJHelper.getSingleCardListName(handCards), MJHelper.getCompositeCardListName(cardsDown));

		if (checkTingBaseRule(handCards, cardsDown, c) == false) {
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

	private boolean checkTingBaseRule(List<Byte> c1, List<Integer> c2, DQMjCheckContext ctx) {
		// 清一色不能胡，所以也不能听(一色上听，加另一色一张牌是不可能胡牌的)
		if (has2Color(c1, c2) == false) {
			logger.info("act=checkTingBaseRule;error=onecolor;desc=清一色不能听;");
			return false;
		}
		// 红中的牌数
		int hongzhong_num = getHongZhongNum(c1, c2);

		// 如果没有红中
		if (hongzhong_num == 0) {
			//没有红中，也没有幺九不能听
			if (has19(c1, c2) == false) {
				logger.info("act=checkTingBaseRule;error=no19;desc=幺九条件不能听;");
				return false;
			}
			// 没有刻子，也没有有2对不可能听，（2对可以听,听刻子）
			if (hasKe(c1, c2) == false && hasTwoPair(c1) == false) {
				logger.info("act=checkTingBaseRule;error=nokezi;desc=没有刻字同时不是听刻子;");
				return false;
			}
		}

		// 门清不能听
		if (!ctx.isQiangTing && isMenQing(c2)) {
			logger.info("act=checkTingBaseRule;error=mengqing;desc=门清不能听;");
			return false;
		}
		return true;
	}
	
	public boolean isMenQing(List<Integer> cardsDown) {
		return cardsDown.size() <= 0;
	}

	// 有几个红中
	public int getHongZhongNum(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int num = 0;
		for (int i = 0; i < cardsInHand.size(); i++) {
			int b = (int) cardsInHand.get(i) & 0xff;
			if (b == MJConstants.MAHJONG_CODE_HONG_ZHONG) {
				num++;
			}
		}

		if (cardsDown == null)
			return num;

		for (int i = 0; i < cardsDown.size(); i++) {
			int bb = cardsDown.get(i) & 0xff;
			if (bb == MJConstants.MAHJONG_CODE_HONG_ZHONG)
				num += 3;
		}

		//
		return num;
	}

	// 有没有“幺”或“九”。
	public boolean has19(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size(); i++) {
			int bb = cardsInHand.get(i);
			if (bb == 1 || bb == 9 || bb == 0x11 || bb == 0x19 || bb == 0x21 || bb == 0x29)
				return true;
		}

		for (int i = 0; i < cardsDown.size(); i++) {
			int bb = cardsDown.get(i);
			byte b1 = (byte) (bb & 0xff);
			byte b2 = (byte) ((bb >> 8) & 0xff);
			byte b3 = (byte) ((bb >> 16) & 0xff);
			//
			if (b1 == 1 || b1 == 9 || b1 == 0x11 || b1 == 0x19 || b1 == 0x21 || b1 == 0x29)
				return true;
			if (b2 == 1 || b2 == 9 || b2 == 0x11 || b2 == 0x19 || b2 == 0x21 || b2 == 0x29)
				return true;
			if (b3 == 1 || b3 == 9 || b3 == 0x11 || b3 == 0x19 || b3 == 0x21 || b3 == 0x29)
				return true;
		}
		return false;
		//
	}

	// 是否有3张一样的“刻”牌
	public boolean hasKe(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size() - 2; i++) {
			Byte b1 = cardsInHand.get(i);
			Byte b2 = cardsInHand.get(i + 1);
			Byte b3 = cardsInHand.get(i + 2);
			if (b1 == b2 && b2 == b3)
				return true;
		}

		for (int i = 0; i < cardsDown.size(); i++) {
			int bb = cardsDown.get(i);
			byte b1 = (byte) (bb & 0xff);
			byte b2 = (byte) ((bb >> 8) & 0xff);
			byte b3 = (byte) ((bb >> 16) & 0xff);

			if (b1 == b2 && b2 == b3)
				return true;
		}

		return false;
	}

	// 手里是否至少有两对牌
	public boolean hasTwoPair(List<Byte> cardsInHand) {
		//
		int pair_num = 0;
		for (int i = 0; i < cardsInHand.size() - 1; i++) {
			Byte b1 = cardsInHand.get(i);
			Byte b2 = cardsInHand.get(i + 1);
			if (b1 == b2)
				pair_num++;
		}
		//
		if (pair_num >= 2)
			return true;

		return false;
	}

	private boolean found_card(int c, List<Byte> cards) {
		for (int i = 0; i < cards.size(); i++) {
			int b1 = cards.get(i) & 0xff;
			if (b1 == c)
				return true;
		}
		//
		return false;
	}

	// 是否有3张连在一起的“顺”牌
	public boolean hasShun(List<Byte> cardsInHand, List<Integer> cardsDown) {
		for (int i = 0; i < cardsInHand.size() - 2; i++) {
			Byte b1 = cardsInHand.get(i);
			if (found_card(b1 + 1, cardsInHand) && found_card(b1 + 2, cardsInHand))// 123,有序
				return true;
		}

		for (int i = 0; i < cardsDown.size(); i++) {
			int bb = cardsDown.get(i);
			byte b1 = (byte) (bb & 0xff);
			byte b2 = (byte) ((bb >> 8) & 0xff);
			byte b3 = (byte) ((bb >> 16) & 0xff);

			if (b1 == b2 - 1 && b2 == b3 - 1)// 123,有序
				return true;

			if (b3 == b2 - 1 && b2 == b1 - 1)// 123,有序
				return true;
		}
		return false;
	}


	// 玩家收牌和吃碰牌，看看是否至少有2种花色
	public boolean has2Color(List<Byte> cardsInHand, List<Integer> cardsDown) {
		int color_num = 0;
		boolean found0 = false; // 万
		boolean found1 = false; // 条
		boolean found2 = false; // 筒
		//
		for (int i = 0; i < cardsInHand.size(); i++) {
			int bb = cardsInHand.get(i) & 0xff;
			// 红中不算色
			if (bb == MJConstants.MAHJONG_CODE_HONG_ZHONG)
				continue;

			int color = bb >> MJConstants.MAHJONG_CODE_COLOR_SHIFTS;
			if (color == 0 && found0 == false) {
				found0 = true;
				color_num++;
			} else if (color == 0x1 && found1 == false) {
				found1 = true;
				color_num++;
			} else if (color == 0x2 && found2 == false) {
				found2 = true;
				color_num++;
			}
			//
			if (color_num >= 2)
				return true;
		}
		//
		for (int i = 0; i < cardsDown.size(); i++) {
			int bb = cardsDown.get(i) & 0xff;
			// 红中不算色
			if (bb == MJConstants.MAHJONG_CODE_HONG_ZHONG)
				continue;
			int color = bb >> MJConstants.MAHJONG_CODE_COLOR_SHIFTS;
			//
			if (color == 0x0 && found0 == false) {
				found0 = true;
				color_num++;
			} else if (color == 0x1 && found1 == false) {
				found1 = true;
				color_num++;
			} else if (color == 0x2 && found2 == false) {
				found2 = true;
				color_num++;
			}
			//
			if (color_num >= 2)
				return true;
		}
		//

		//
		return false;
	}

}
