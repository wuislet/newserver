package com.buding.mj.common;

import java.util.ArrayList;
import java.util.List;
import com.buding.mj.helper.MJHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
public class BaseMJRule implements MJRule {
	public boolean canHuShiSanYao(List<Byte> list) {
		return canHuShiSanYao(list, new MjCheckResult());
	}
	
	public boolean canHuShiSanYao(List<Byte> list, MjCheckResult result) { //常规特殊胡型：十三幺
		if(list.size() != 14) { //必须门清 //TODO WXD 判断理由不准确
			return false;
		}
		
		//result.special = 10;
		return false; //TODO WXD
	}

	public boolean canHuQiXiaoDui(List<Byte> list) {
		return canHuQiXiaoDui(list, new MjCheckResult());
	}
	
	public boolean canHuQiXiaoDui(List<Byte> list, MjCheckResult result) { // 常规特殊胡型：七小对
		if(list.size() != 14) { //必须门清 //TODO WXD 判断理由不准确
			return false;
		}
		
		int gangCnt = 0;
		for (byte card : list) {
			int count = MJHelper.getCardCount(list, card);
			if ((count & 1) == 1) { //奇数
				return false;
			}
			if(count == 4) {
				gangCnt += 1;
			}
		}
		
		result.special = gangCnt + 1;
		return true;
	}
	
	@Override
	public boolean canHu(List<Byte> list) {
		return canHu(list, new MjCheckResult());
	}

	public boolean canChengPai(List<Byte> list) {
		return canChengPai(list, new MjCheckResult());
	}
	
	@Override
	public boolean canHu(List<Byte> list, MjCheckResult result) {
		List<Byte> doubleList = MJHelper.getAllDouble(list);

		for (byte card : doubleList) {
			List<Byte> temp = new ArrayList<Byte>(list);
			temp.remove((Byte) card);
			temp.remove((Byte) card);
			if (canChengPai(temp, result)) {
				return true;
			}
		}

		return false;
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
		MjCheckResult ret = new MjCheckResult(); 
		canHu(list, ret);
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(ret));
	}
	
	public static void main(String[] args) {
		new BaseMJRule().test(1,1,2,2,2,3,4,5);
	}
}
