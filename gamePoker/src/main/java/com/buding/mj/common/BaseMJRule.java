package com.buding.mj.common;

import java.util.ArrayList;
import java.util.List;
import com.buding.mj.helper.MJHelper;
import com.google.gson.GsonBuilder;
public class BaseMJRule implements MJRule {
	public MjCheckResult canHuShiSanYao(MJContext ctx) { //常规特殊胡型：十三幺
		return null; //TODO WXD
		
		//MjCheckResult ret = new MjCheckResult();
		//ret.special = 10;
		//return ret;
	}
	
	public MjCheckResult canHuQiXiaoDui(MJContext ctx) { // 常规特殊胡型：七小对
		int gangCnt = 0;
		List<Byte> list = ctx.cardsInCard;
		if(ctx.cardsDown.size() != 0) { //必须门清
			return null;
		}

		for (byte card : list) {
			int count = MJHelper.getCardCount(list, card);
			if ((count & 1) == 1) { //奇数
				return null;
			}
			if(count == 4) {
				gangCnt += 1;
			}
		}
		
		MjCheckResult ret = new MjCheckResult();
		ret.special = gangCnt + 1;
		return ret;
	}
	
	@Override
	public MjCheckResult canHu(MJContext ctx) {
		List<Byte> list = ctx.cardsInCard;
		List<Byte> doubleList = MJHelper.getAllDouble(list);

		for (byte card : doubleList) {
			List<Byte> temp = new ArrayList<Byte>(list);
			temp.remove((Byte) card);
			temp.remove((Byte) card);
			MjCheckResult ret = new MjCheckResult();
			if (canChengPai(temp, ret)) {
				return ret;
			}
		}

		return null;
	}

	public boolean canChengPai(List<Byte> list, MjCheckResult result) {
		if (list.size() == 0) {
			return true;
		}
		if (list.size() < 3) {
			return false;
		}

		int count = MJHelper.getCardCount(list, list.get(0));
		if (count > 2) {
			List<Byte> temp = new ArrayList<Byte>(list);
			temp.remove((Byte) list.get(0));
			temp.remove((Byte) list.get(0));
			temp.remove((Byte) list.get(0));
			MjCheckResult ret = new MjCheckResult();
			if (canChengPai(temp, ret)) {
				result.kezis.addAll(ret.kezis);
				result.shunzis.addAll(ret.shunzis);
				return true;
			}
		}

		Byte card1 = (byte) (list.get(0) + 1);
		Byte card2 = (byte) (list.get(0) + 2);
		if (list.contains(card1) && list.contains(card2)) {
			List<Byte> temp = new ArrayList<Byte>(list);
			temp.remove((Byte) list.get(0));
			temp.remove((Byte) card1);
			temp.remove((Byte) card2);
			MjCheckResult ret = new MjCheckResult();
			if (canChengPai(temp, ret)) {
				result.kezis.addAll(ret.kezis);
				result.shunzis.addAll(ret.shunzis);
				return true;
			}
		}

		return false;
	}
	
	public void test(int ...bytes) {
		List<Byte> list = new ArrayList<Byte>();
		for(int b : bytes) {
			list.add((byte)(b & 0xFF));
		}
		MJContext contxt = new MJContext();
		contxt.cardsInCard.addAll(list);
		MjCheckResult ret = canHu(contxt);
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(ret));
	}
	
	public static void main(String[] args) {
		new BaseMJRule().test(1,1,2,2,2,3,4,5);
	}
}
