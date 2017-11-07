package com.buding.mj.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import packet.mj.MJ.GameOperChiArg;
import packet.mj.MJ.GameOperPlayerActionNotify;

import com.buding.mj.common.CardCombo;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.model.Card;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class MJHelper {
	public static Map<Integer, String> singleCardMap = new HashMap<Integer, String>();

	static {
		singleCardMap.put(MJConstants.MAHJONG_CODE_HONG_ZHONG + 0, "红中");
		for (int i = 1; i <= 9; i++) {
			int ib = (0 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
			byte b = (byte) (ib & 0xff);
			singleCardMap.put(b + 0, i + "万");
		}
		for (int i = 1; i <= 9; i++) {
			int ib = (1 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
			byte b = (byte) (ib & 0xff);
			singleCardMap.put(b + 0, i + "条");
		}
		for (int i = 1; i <= 9; i++) {
			int ib = (2 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
			byte b = (byte) (ib & 0xff);
			singleCardMap.put(b + 0, i + "筒");
		}
		for (int i = 1; i <= 7; i++) {
			int ib = (3 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
			byte b = (byte) (ib & 0xff);
			singleCardMap.put(b + 0, i + "风");
		}
		for (int i = 1; i <= 4; i++) {
			int ib = (4 << MJConstants.MAHJONG_CODE_COLOR_SHIFTS) + i;
			byte b = (byte) (ib & 0xff);
			singleCardMap.put(b + 0, i + "花");
		}
		System.out.println(new Gson().toJson(singleCardMap));
	}

	public static Set<Byte> getAllUniqCard() {
		Set<Byte> set = new HashSet<Byte>();
		for (int card : singleCardMap.keySet()) {
			byte c = (byte) (card & 0xFF);
			set.add(c);
		}
		return set;
	}

	public static List<Byte> getCardCodeList(List<Card> cards) {
		List<Byte> list = new ArrayList<Byte>();
		for (Card card : cards) {
			list.add((byte) (card.code & 0XFF));
		}
		return list;
	}

	public static String getSingleCardCodeNames(List<Byte> cards) {
		List<String> list = new ArrayList<String>();
		for (int card : cards) {
			list.add(new Card(card, singleCardMap.get(card)).toString());
		}
		return new GsonBuilder().create().toJson(list);
	}

	public static String getCompositeCardCodeNames(List<Integer> cards) {
		List<String> list = new ArrayList<String>();
		for (int card : cards) {
			int card1 = card & 0xFF;
			int card2 = (card >> 8) & 0xFF;
			int card3 = (card >> 16) & 0xFF;
			if (card1 > 0) {
				list.add(new Card(card1, singleCardMap.get(card1)).toString());
			}
			if (card2 > 0) {
				list.add(new Card(card2, singleCardMap.get(card2)).toString());
			}
			if (card3 > 0) {
				list.add(new Card(card3, singleCardMap.get(card3)).toString());
			}
		}
		return new GsonBuilder().create().toJson(list);
	}

	public static String getSingleCardListName(List<Byte> cards) {
		List<String> ret = new ArrayList<String>();
		for (int card : cards) {
			ret.add(getSingleCardName(card));
		}
		return new Gson().toJson(ret);
	}

	public static String getCompositeCardListName(List<Integer> cards) {
		List<String> ret = new ArrayList<String>();
		for (int card : cards) {
			int card1 = card & 0xFF;
			int card2 = (card >> 8) & 0xFF;
			int card3 = (card >> 16) & 0xFF;
			if (card1 > 0) {
				ret.add(getSingleCardName(card1));
			}
			if (card2 > 0) {
				ret.add(getSingleCardName(card2));
			}
			if (card3 > 0) {
				ret.add(getSingleCardName(card3));
			}
		}
		return new Gson().toJson(ret);
	}

	public static String getSingleCardName(int card) {
		if (card == -1) {
			return "背面";
		}
		if (card == 0) {
			return "";
		}
		if (card == MJConstants.MAHJONG_CODE_GANG_CARD){
			return "杠";
		}
		return singleCardMap.get(card);
	}

	private static int convertAct(List<String> ret, int multiAct, int singleAct, String actName) {
		if ((multiAct & singleAct) == singleAct) {
			ret.add(actName);
			return multiAct - singleAct;
		}
		return multiAct;
	}

	public static Set<Byte> getUniqCardList(List<Byte> list) {
		Set<Byte> set = new HashSet<Byte>();
		set.addAll(list);
		return set;
	}

	public static String getActionName(int act) {
		List<String> ret = new ArrayList<String>();
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_CHI, "吃");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_PENG, "碰");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_AN_GANG, "暗杠");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_BU_GANG, "补杠");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_ZHI_GANG, "直杠");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_CHU, "出");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_HU, "胡");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_TING, "听");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_CANCEL, "取消");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_MO, "摸牌");
		
		//大庆麻将
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_CHI_TING, "吃听");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_PENG_TING, "碰听");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_ZD_TING, "支对");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_CHNAGE_BAO, "换宝");
		
		//金昌麻将
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_SHOUPAO, "收炮");
		act = convertAct(ret, act, MJConstants.MAHJONG_OPERTAION_SHUAIJIUYAO, "甩九幺");

		if (act > 0) {
			throw new RuntimeException("0x" + Integer.toHexString(act) + "未知牌操作");
		}
		return new Gson().toJson(ret);
	}

	public static void copyChiArg(List<CardCombo> combos, GameOperPlayerActionNotify.Builder msg) {
		if (combos == null) {
			return;
		}
		for (CardCombo c : combos) {
			GameOperChiArg.Builder gb = GameOperChiArg.newBuilder();
			gb.setMyCard1(c.card1);
			gb.setMyCard2(c.card2);
			gb.setTargetCard(c.targetCard);
			msg.addChiArg(gb);
		}
	}

	public static void copyPengArg(int newCard, GameOperPlayerActionNotify.Builder msg) {
		if (newCard <= 0) {
			return;
		}
		msg.setPengArg(newCard);
	}

	public static void add2SortedList(int b, List<Integer> cards) {
		boolean inserted = false;
		//
		for (int i = 0; i < cards.size(); i++) {
			int bb = cards.get(i);
			if (b < bb) {
				cards.add(i, b);
				inserted = true;
				break;
			}
		}
		//
		if (inserted == false) {
			cards.add(b);
		}
	}

	public static boolean findInSortList(List<Byte> list, byte card1, byte card2) {
		int ind = list.indexOf((Object) card1);
		int size = list.size();
		if (ind == -1) {
			return false;
		}
		boolean found1 = false;
		boolean found2 = false;
		for (; ind < size; ind++) {
			if (list.get(ind) == card1) {
				found1 = true;
				break;
			}
			if (list.get(ind) > card1) {
				return false;
			}
		}

		for (; ind < size; ind++) {
			if (list.get(ind) == card2) {
				found2 = true;
				break;
			}
			if (list.get(ind) > card2) {
				return false;
			}
		}
		return found1 && found2;
	}

	public static void add2SortedList(byte b, List<Byte> cards) {
		boolean inserted = false;
		//TODO WXD 2分查找
		for (int i = 0; i < cards.size(); i++) {
			byte bb = cards.get(i);
			if (b < bb) {
				cards.add(i, b);
				inserted = true;
				break;
			}
		}
		//
		if (inserted == false) {
			cards.add(b);
		}
	}

	public static String getResultTypeDesc(int fanType) {
		if ((fanType & MJConstants.MAHJONG_HU_CODE_DAILOU) == MJConstants.MAHJONG_HU_CODE_DAILOU) {
			return "宝中宝";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_GUADAFENG) == MJConstants.MAHJONG_HU_CODE_GUADAFENG) {
			return "刮大风";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_KAIPAIZHA) == MJConstants.MAHJONG_HU_CODE_KAIPAIZHA) {
			return "开牌炸";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_HONGZHONGMTF) == MJConstants.MAHJONG_HU_CODE_HONGZHONGMTF) {
			return "红中满天飞";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_BAO_ZHONG_BAO) == MJConstants.MAHJONG_HU_CODE_BAO_ZHONG_BAO) {
			return "宝中宝";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_MO_BAO) == MJConstants.MAHJONG_HU_CODE_MO_BAO) {
			return "摸宝";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_WIN) == MJConstants.MAHJONG_HU_CODE_WIN) {
			return "平胡";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_LOSE) == MJConstants.MAHJONG_HU_CODE_LOSE) {
			return "输";
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_LIUJU) == MJConstants.MAHJONG_HU_CODE_LIUJU) {
			return "流局";
		}
		return "无";
	}

	public static int getHuType(int fanType) {
		// 开牌炸
		if ((fanType & MJConstants.MAHJONG_HU_CODE_KAIPAIZHA) == MJConstants.MAHJONG_HU_CODE_KAIPAIZHA) {
			return MJConstants.MAHJONG_HU_CODE_KAIPAIZHA;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_DAILOU) == MJConstants.MAHJONG_HU_CODE_DAILOU) {
			return MJConstants.MAHJONG_HU_CODE_DAILOU;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_BAO_ZHONG_BAO) == MJConstants.MAHJONG_HU_CODE_BAO_ZHONG_BAO) {
			return MJConstants.MAHJONG_HU_CODE_BAO_ZHONG_BAO;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_GUADAFENG) == MJConstants.MAHJONG_HU_CODE_GUADAFENG) {
			return MJConstants.MAHJONG_HU_CODE_GUADAFENG;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_HONGZHONGMTF) == MJConstants.MAHJONG_HU_CODE_HONGZHONGMTF) {
			return MJConstants.MAHJONG_HU_CODE_HONGZHONGMTF;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_MO_BAO) == MJConstants.MAHJONG_HU_CODE_MO_BAO) {
			return MJConstants.MAHJONG_HU_CODE_MO_BAO;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_ZI_MO) == MJConstants.MAHJONG_HU_CODE_ZI_MO) {
			return MJConstants.MAHJONG_HU_CODE_ZI_MO;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_WIN) == MJConstants.MAHJONG_HU_CODE_WIN) {
			return MJConstants.MAHJONG_HU_CODE_WIN;
		}
		if ((fanType & MJConstants.MAHJONG_HU_CODE_LIUJU) == MJConstants.MAHJONG_HU_CODE_LIUJU) {
			return MJConstants.MAHJONG_HU_CODE_LIUJU;
		}
		return MJConstants.MAHJONG_HU_CODE_WIN;
	}

	public static int getResultType(int fanType) {
		if ((fanType & MJConstants.MAHJONG_HU_CODE_WIN) == MJConstants.MAHJONG_HU_CODE_WIN) {
			return MJConstants.MAHJONG_HU_CODE_WIN;
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_LOSE) == MJConstants.MAHJONG_HU_CODE_LOSE) {
			return MJConstants.MAHJONG_HU_CODE_LOSE;
		} else if ((fanType & MJConstants.MAHJONG_HU_CODE_LIUJU) == MJConstants.MAHJONG_HU_CODE_LIUJU) {
			return MJConstants.MAHJONG_HU_CODE_LIUJU;
		}
		return 0;
	}

	public static String getFanDescList2String(int fanType) {
		List<String> descList = getFanDescList(fanType);
		return new Gson().toJson(descList);
	}

	public static List<String> getFanDescList(int fanType) {
		List<String> list = new ArrayList<String>();

		int i = 0;
		if ((fanType & MJConstants.MAHJONG_HU_CODE_WIN) == MJConstants.MAHJONG_HU_CODE_WIN) {
			if ((fanType & MJConstants.MAHJONG_HU_CODE_ZI_MO) == MJConstants.MAHJONG_HU_CODE_ZI_MO) {
				fanType -= MJConstants.MAHJONG_HU_CODE_ZI_MO;
				i++;
				list.add("自摸 ");
			}

			if ((fanType & MJConstants.MAHJONG_HU_CODE_MEN_QING) == MJConstants.MAHJONG_HU_CODE_MEN_QING) {
				fanType -= MJConstants.MAHJONG_HU_CODE_MEN_QING;
				i++;
				list.add("门清 ");
			}

			if ((fanType & MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI) == MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI) {
				fanType -= MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI;
				i++;
				list.add("七小对");
			}

			if ((fanType & MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU) == MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU) {
				fanType -= MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU;
				i++;
				list.add("对对胡 ");
			}
			
			if ((fanType & MJConstants.MAHJONG_HU_CODE_JIA_HU) == MJConstants.MAHJONG_HU_CODE_JIA_HU) {
				fanType -= MJConstants.MAHJONG_HU_CODE_JIA_HU;
				i++;
				list.add("夹胡 ");
			}

			if (i == 0) {
				list.add("平胡 ");
			}
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_TING) != MJConstants.MAHJONG_HU_CODE_TING) {
			list.add("未上听 ");
		}
		
		if ((fanType & MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA) == MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA) {
			fanType -= MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA;
			list.add("庄家 ");
		}

		return list;
	}

	public static String getFanDesc(int fanType) { //Only for server print. not matter very much.
		String fanDesc = "";
		if ((fanType & MJConstants.MAHJONG_HU_CODE_WIN) == MJConstants.MAHJONG_HU_CODE_WIN) {
			fanDesc += "赢 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_LOSE) == MJConstants.MAHJONG_HU_CODE_LOSE) {
			fanDesc += "输 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_TING) == MJConstants.MAHJONG_HU_CODE_TING) {
			fanDesc += "已上听 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_TING) != MJConstants.MAHJONG_HU_CODE_TING) {
			fanDesc += "未上听 ";
		}
		
		if ((fanType & MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA) == MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA) {
			fanDesc += "我是庄家 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_TARGET_ZHUANG_JIA) == MJConstants.MAHJONG_HU_CODE_TARGET_ZHUANG_JIA) {
			fanDesc += "对手庄家 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_ZI_MO) == MJConstants.MAHJONG_HU_CODE_ZI_MO) {
			fanDesc += "自摸一番 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_MEN_QING) == MJConstants.MAHJONG_HU_CODE_MEN_QING) {
			fanDesc += "门清 ";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI) == MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI) {
			fanDesc += "七小对";
		}

		if ((fanType & MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU) == MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU) {
			fanDesc += "对对胡 ";
		}

		return fanDesc;
	}

	public static String getChiComboStr(List<GameOperChiArg> list) {
		if (list == null) {
			return "";
		}
		List<String> ret = new ArrayList<String>();
		for (GameOperChiArg arg : list) {
			int card = arg.getMyCard1() & 0XFF;
			int card2 = arg.getMyCard2() & 0XFF;
			int target = arg.getTargetCard() & 0XFF;
			ret.add(new Card(card, MJHelper.getSingleCardName(card)).toString());
			ret.add(new Card(card2, MJHelper.getSingleCardName(card2)).toString());
			ret.add(new Card(target, MJHelper.getSingleCardName(target)).toString());
		}
		return new Gson().toJson(ret);
	}

	public static List<CardCombo> getAllShunzi(List<Byte> cards) {
		List<CardCombo> list = new ArrayList<CardCombo>();
		for (int i = 0; i < cards.size() - 2; i++) {
			Byte b1 = cards.get(i);
			Byte b2 = (byte)(b1+1);
			Byte b3 = (byte)(b1+2);
			int ind2 = findIndex(cards, b2, i+1);
			if(ind2 == -1) {
				continue;
			}			
			int ind3 = findIndex(cards, b3, ind2+1);
			if(ind3 == -1) {
				continue;
			}
			list.add(new CardCombo(b1, b2, b3));
		}

		return list;
	}
	
	public static int findIndex(List<Byte> cards, byte card2Find, int fromInd) {
		if(fromInd >= cards.size()) {
			return -1;
		}
		int i = fromInd;
		while(i < cards.size()) {
			if(cards.get(i) == card2Find) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static List<CardCombo> getAllKezi(List<Byte> cards) {
		List<CardCombo> list = new ArrayList<CardCombo>();
		for (int i = 0; i < cards.size() - 2; i++) {
			Byte b1 = cards.get(i);
			Byte b2 = cards.get(i + 1);
			Byte b3 = cards.get(i + 2);
			if (b1 == b2 && b2 == b3) {
				list.add(new CardCombo(b1, b2, b3));
			}
		}

		return list;
	}

	public static int getCardCount(List<Byte> list, byte card) {
		int i = 0;
		for (byte c : list) {
			if (c == card) {
				i++;
			}
		}
		return i;
	}

	public static List<Byte> getAllDouble(List<Byte> list) {
		Map<Byte, Integer> map = new HashMap<Byte, Integer>();
		for (byte b : list) {
			if (map.containsKey(b)) {
				map.put(b, map.get(b) + 1);
			} else {
				map.put(b, 1);
			}
		}
		List<Byte> ret = new ArrayList<Byte>();
		for (Entry<Byte, Integer> e : map.entrySet()) {
			if (e.getValue() > 1) {
				ret.add(e.getKey());
			}
		}
		Collections.sort(ret);
		return ret;
	}

	/**
	 * 往 门前牌 里加牌
	 * @param point 最小的牌点数。
	 * @param chipoint 被吃的牌点数。
	 * @param type 1-碰 2-吃 3-杠
	 * @param list 门前牌堆
	 * @return
	 */
	public static int addCardDown(int point, int chipoint, int type, List<Integer> list) {
		int card = 0;
		if(type == 1) {//碰
			card = (point << 16) | (point << 8) | point;
		} else if(type == 2) {//吃
			card = (chipoint << 24) | ((point + 2) << 16) | ((point + 1) << 8) | point;
		} else if(type == 3) {//杠
			card = (MJConstants.MAHJONG_CODE_GANG_CARD << 16) | (0 << 8) | point;
			//card = (point << 24) | (point << 16) | (point << 8) | point;
		}

		list.add(card);
		return card;
	}
	
	public static int getCardNum(byte b){
		return b & MJConstants.MAHJONG_CODE_NUMBER_MASK;
	}
	
	public static int getCardColor(byte b){
		return b >> MJConstants.MAHJONG_CODE_COLOR_SHIFTS;
	}
	
	public static boolean isNormalCard(byte b){
		if(b == MJConstants.MAHJONG_CODE_GANG_CARD) {
			return false;
		}
		return getCardColor(b) < 3;
	}
}
