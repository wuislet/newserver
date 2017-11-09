package com.buding.mj.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.buding.mj.helper.MJHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
public class BaseMJRule implements MJRule {
	public boolean canHuShiSanYao(List<Byte> list) { //唯一对外接口！
		return canHuShiSanYao(list, new MjCheckResult());
	}
	
	public boolean canHuShiSanYao(List<Byte> list, MjCheckResult result) { //常规特殊胡型：十三幺
		if(list.size() != 14) { //手牌必须14张。
			return false;
		}
		
		//result.special = 10;
		return false; //TODO WXD
	}

	public boolean canHuQiXiaoDui(List<Byte> list) { //唯一对外接口！
		return canHuQiXiaoDui(list, new MjCheckResult());
	}
	
	public boolean canHuQiXiaoDui(List<Byte> list, MjCheckResult result) { // 常规特殊胡型：七小对
		if(list.size() != 14) { //手牌必须14张。
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
	public boolean canHu(List<Byte> list, int guiCard) { //唯一对外接口！
		if(guiCard == -1) {
			return canHu(list, new MjCheckResult());
		} else {
			return testHuPai(list, guiCard);
		}
	}

	@Override
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

	@Override
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
	
	// ================ 鬼牌，万能牌 ================
	// 收集有问题的数据：
	// 1,1,1,6,6,9,9,9,17,17,17,19,57; gui=57; canting=[19,18,21,20,6,57]; 17本该能听。
    private static HashMap<Integer,List<CheckObjectVO>> shengCard = new HashMap<>(); //剩余牌数:[当时的牌型数据结构, ...]

    public static int changeCardPoint(Byte cardpoint) { //牌点数转化器
    	int color = MJHelper.getCardColor(cardpoint);
    	int point = MJHelper.getCardNum(cardpoint);
    	if(color < 3) {
    		return color * 9 + point - 1;
    	} else {
    		return 27 + point / 2;
    	}
    }
    
    public static boolean testHuPai(List<Byte> list, int guiPai){ //入口
        int guiCount = 0;
    	int [] paiList = new int[34];
    	for(Byte card : list) {
    		if(card == guiPai) {
    			guiCount += 1;
    		} else {
    			paiList[changeCardPoint(card)] += 1;
    		}
    	}
		//System.out.println("  [[[>>> testHuPai    - list: " + new Gson().toJson(paiList) + " gui " + guiPai + " cnt " + guiCount);
    	return getNeedHunNum(paiList, guiCount);
    }

    /**
     * 得到胡牌需要的赖子数
     * type 将在哪里
     * @return
     */
    private static boolean getNeedHunNum(int[] paiList, int guiCount){
        int[] wan_arr = new int[9];
        int[] tiao_arr = new int[9];
        int[] tong_arr = new int[9];
        int[] feng_arr = new int[7];
        for(int i = 0; i < 9; i++) {
        	wan_arr[i] = paiList[i];
        }
        for(int i = 9; i < 18; i++) {
        	tiao_arr[i - 9] = paiList[i];
        }
        for(int i = 18; i < 27; i++) {
        	tong_arr[i - 18] = paiList[i];
        }
        for(int i = 27; i < 34; i++) {
        	feng_arr[i - 27] = paiList[i];
        }

        int needNum = 0;
        // 分开不同花色的牌放入每个数组。 牌数字:个数
        needNum = getNumWithJiang(wan_arr.clone())+ getNorNumber(tiao_arr.clone()) + getNorNumber(tong_arr.clone()) + getNorNumber_feng(feng_arr.clone());
        if(needNum <= guiCount){
        	return true;
        }
        else {
        	needNum = getNorNumber(wan_arr.clone()) +getNumWithJiang(tiao_arr.clone()) + getNorNumber(tong_arr.clone())+ getNorNumber_feng(feng_arr.clone());
        	if(needNum <= guiCount){
        		return true;
        	}
        	else{
        		needNum = getNorNumber(wan_arr.clone()) + getNorNumber(tiao_arr.clone())+getNumWithJiang(tong_arr.clone()) + getNorNumber_feng(feng_arr.clone());
        		if(needNum <= guiCount){
        			return true;
        		}
        		else{
        			needNum = getNorNumber(wan_arr.clone()) + getNorNumber(tiao_arr.clone())+getNorNumber(tong_arr.clone()) + getNumWithJiang_feng(feng_arr.clone());
            		if(needNum <= guiCount){
            			return true;
            		}
            		else{
            			return false;
            		}
        		}
        	}
        }
    }

    static int Jiang = 0;

    private static int  getNumWithJiang(int[] temp_arr){
        int result = 9999;
        Jiang = 0;
        shengCard.clear();
        if(checkCanbeGroup(temp_arr.clone())){
                result = 0;
        }else{
            Set<Integer> ints =  shengCard.keySet();
            ints.size();
            if(ints.iterator().hasNext()){
                int key = ints.iterator().next();
                List<CheckObjectVO> objectVOList = shengCard.get(key);
                objectVOList = deleteSameItemForList(objectVOList);
                for(int k = 0;k<objectVOList.size();k++) {
                    CheckObjectVO objectVO = objectVOList.get(k);
                    if (objectVO.isJiang == 0) {
                        for (int i = 0; i < 9; i++) {
                            if (objectVO.paiArray[i] > 0) {
                                int tempInt = getJiangNumber(objectVO.paiArray.clone(), i);
                                if (tempInt < result) {
                                    result = tempInt;
                                }
                            }
                        }
                    } else {
                        int tempInt = getNumber(objectVO.paiArray.clone());
                        if (tempInt < result) {
                            result = tempInt;
                        }
                    }
                }
            }
        }
        return  result;
    }

    private static int getJiangNumber(int[] temp_arr,int index){
        int result = 0;
        if(temp_arr[index] >= 2){
            temp_arr[index] -= 2;
        }else if(temp_arr[index] == 1){
            temp_arr[index] = 0;
            result++;
        }
        return result + getNumber(temp_arr);
    }


    private static int getNorNumber(int[] temp_arr){
        int result = 9999;
        shengCard.clear();
        if(checkNormalGroup(temp_arr.clone())){
            result = 0;
        }else{
            Set<Integer> ints =  shengCard.keySet();
            ints.size();
            if(ints.iterator().hasNext()) {
                int key = ints.iterator().next();
                List<CheckObjectVO> objectVOList = shengCard.get(key);
                objectVOList = deleteSameItemForList(objectVOList);
                for (int k = 0; k < objectVOList.size(); k++) {
                    CheckObjectVO objectVO = objectVOList.get(k);
                    int tempInt = getNumber(objectVO.paiArray.clone());
                    if (tempInt < result) {
                        result = tempInt;
                    }
                }
            }
        }
        return result;
    }

    private static boolean checkCanbeGroup(int[] temp_arr){
        int resultNum = Remain(temp_arr);
        if (resultNum == 0) {
            return true;           //   递归退出条件：如果没有剩牌，则胡牌返回。
        }else{
            CheckObjectVO objectVO = new CheckObjectVO();
            objectVO.isJiang = Jiang;
            objectVO.paiArray = temp_arr.clone();
            List<CheckObjectVO> tempList = shengCard.get(resultNum);
            if(tempList == null){
                tempList = new ArrayList<>();
                tempList.add(objectVO);
                shengCard.put(resultNum,tempList);
            }else{
                tempList.add(objectVO);
            }
        }
        for (int i = 0;  i < temp_arr.length; i++) {//   找到有牌的地方，i就是当前牌,   PAI[i]是个数
            //   跟踪信息
            //   4张组合(杠子)
            if(temp_arr[i] != 0){
                if (temp_arr[i] == 4)                               //   如果当前牌数等于4张
                {
                    temp_arr[i] = 0;                                     //   除开全部4张牌
                    if (checkCanbeGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，和牌
                    }
                    temp_arr[i] = 4;                                     //   否则，取消4张组合
                }
                //   3张组合(大对)
                if (temp_arr[i] >= 3)                               //   如果当前牌不少于3张
                {
                    temp_arr[i] -= 3;                                   //   减去3张牌
                    if (checkCanbeGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，胡牌
                    }
                    temp_arr[i] += 3;                                   //   取消3张组合
                }
                //   2张组合(将牌)
                if (Jiang ==0 && temp_arr[i] >= 2)           //   如果之前没有将牌，且当前牌不少于2张
                {
                    Jiang = 1;                                       //   设置将牌标志
                    temp_arr[i] -= 2;                                   //   减去2张牌
                    if (checkCanbeGroup(temp_arr)) return true;             //   如果剩余的牌组合成功，胡牌
                    temp_arr[i] += 2;                                   //   取消2张组合
                    Jiang = 0;                                       //   清除将牌标志
                }
                if   ( i> 27){
                    return   false;               //   “东南西北中发白”没有顺牌组合，不胡
                }
                //   顺牌组合，注意是从前往后组合！
                //   排除数值为8和9的牌
                if (i<7 && temp_arr[i+1]!=0 && temp_arr[i+2]!=0)             //   如果后面有连续两张牌
                {
                    temp_arr[i]--;
                    temp_arr[i + 1]--;
                    temp_arr[i + 2]--;                                     //   各牌数减1
                    if (checkCanbeGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，胡牌
                    }
                    temp_arr[i]++;
                    temp_arr[i + 1]++;
                    temp_arr[i + 2]++;                                     //   恢复各牌数
                }
            }
        }
        return false;
    }

    private static boolean checkNormalGroup(int[] temp_arr){
        int resultNum = Remain(temp_arr);
        if (resultNum == 0) {
            return true;           //   递归退出条件：如果没有剩牌，则胡牌返回。
        }else{
            CheckObjectVO objectVO = new CheckObjectVO();
            objectVO.isJiang = Jiang;
            objectVO.paiArray = temp_arr.clone();
            List<CheckObjectVO> tempList = shengCard.get(resultNum);
            if(tempList == null){
                tempList = new ArrayList<>();
                tempList.add(objectVO);
                shengCard.put(resultNum,tempList);
            }else{
                tempList.add(objectVO);
            }
        }
        for (int i = 0;  i < temp_arr.length; i++) {//   找到有牌的地方，i就是当前牌,   PAI[i]是个数
            //   跟踪信息
            //   4张组合(杠子)
            if(temp_arr[i] != 0){
                if (temp_arr[i] == 4)                               //   如果当前牌数等于4张
                {
                    temp_arr[i] = 0;                                     //   除开全部4张牌
                    if (checkNormalGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，和牌
                    }
                    temp_arr[i] = 4;                                     //   否则，取消4张组合
                }
                //   3张组合(大对)
                if (temp_arr[i] >= 3)                               //   如果当前牌不少于3张
                {
                    temp_arr[i] -= 3;                                   //   减去3张牌
                    if (checkNormalGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，胡牌
                    }
                    temp_arr[i] += 3;                                   //   取消3张组合
                }
                if   ( i> 27){
                    return   false;               //   “东南西北中发白”没有顺牌组合，不胡
                }
                //   顺牌组合，注意是从前往后组合！
                //   排除数值为8和9的牌
                if (i<7 && temp_arr[i+1]!=0 && temp_arr[i+2]!=0)             //   如果后面有连续两张牌
                {
                    temp_arr[i]--;
                    temp_arr[i + 1]--;
                    temp_arr[i + 2]--;                                     //   各牌数减1
                    if (checkNormalGroup(temp_arr)) {
                        return true;             //   如果剩余的牌组合成功，胡牌
                    }
                    temp_arr[i]++;
                    temp_arr[i + 1]++;
                    temp_arr[i + 2]++;                                     //   恢复各牌数
                }
            }
        }
        return false;
    }

    //   检查剩余牌数
    static int Remain(int[] paiList) {
        int sum = 0;
        for (int i = 0; i < paiList.length; i++) {
            sum += paiList[i];
        }
        return sum;
    }

    private static int getNumber(int[] temp_arr){
        int result = 0;
        for(int i=0;i<9;i++) {
            if(temp_arr[i] <= 0) {
            	continue;
            }
            
            if(temp_arr[i] >= 3){ //刻子消除  从这里就看出不准确。比如 123 3x 345 777 999, 123 3x 345 678 888 
                temp_arr[i] -= 3;
                i--;
            } else {
                if(temp_arr[i] == 2){ //两张 默认补成刻子。
                    temp_arr[i] = 0;
                    result++;
                }else { //顺子 只往后补。
                    if (i < 7) {
                        if (temp_arr[i + 1] > 0 && temp_arr[i + 2] > 0) {
                            temp_arr[i]--;
                            temp_arr[i + 1]--;
                            temp_arr[i + 2]--;
                            i--;
                        } else if (temp_arr[i + 1] > 0 && temp_arr[i + 2] == 0) {
                            temp_arr[i]--;
                            temp_arr[i + 1]--;
                            result++;
                            i--;
                        } else if (temp_arr[i + 1] == 0 && temp_arr[i + 2] > 0) {
                            temp_arr[i]--;
                            temp_arr[i + 2]--;
                            result++;
                            i--;
                        } else if (temp_arr[i + 1] == 0 && temp_arr[i + 2] == 0) {
                            temp_arr[i] = 0;
                            result += 2;
                        }
                    } else {
                        if (i == 7) {
                            if (temp_arr[i] > 0 && temp_arr[i + 1] > 0) {
                                temp_arr[i]--;
                                temp_arr[i + 1]--;
                                result++;
                                i--;
                            } else if (temp_arr[i] > 0 && temp_arr[i + 1] == 0) {
                                result = result + 3 - temp_arr[i];
                                temp_arr[i] = 0;
                            }
                        } else {
                            result = result + 3 - temp_arr[i];
                            temp_arr[i] = 0;
                        }
                    }
                }
            }
        }
        return result;
    }

    //判断杠、顺子和三同需要多少个癞子
    private static int getNorNumber_feng(int[] temp_arr){
    	int result = 0;
		for (int i = 0;  i < temp_arr.length; i++) {
			if(temp_arr[i] == 2) {
				result ++;
			}
			if(temp_arr[i] == 1) {
				result += 2;
			}
        }
    	return result;
    }

    //带将时，判断杠、顺子和三同需要多少个癞子
    private static int getNumWithJiang_feng(int[] temp_arr){
    	int result = 0;
		int intShuang=0; //双牌个数
		int intSignal=0; //单牌个数
		for (int i = 0;  i < temp_arr.length; i++) {//   找到有牌的地方，i就是当前牌,   PAI[i]是个数
            if (temp_arr[i] == 2)
            {
            	intShuang++;
            }
            if (temp_arr[i] == 1)
            {
            	intSignal++;
            }
        }

		//分析还要多少个癞子才能胡牌
		if(intShuang == 0 && intSignal == 0){
			result = 2;
		}else if(intShuang > 0 && intSignal == 0){
			result = intShuang-1;
		}else if(intShuang == 0 && intSignal > 0){
			result = (intSignal-1)*2+1;
		} else {
			result = intShuang - 1 + (intSignal) * 2;
		}
		//分析还要多少个癞子才能胡牌==》结束

    	return result;
    }

    private static List<CheckObjectVO> deleteSameItemForList(List<CheckObjectVO> tempList){
        HashMap<String,CheckObjectVO> tempMap = new HashMap<>();
        List<CheckObjectVO> result = new ArrayList<>();
        for(int i=0;i<tempList.size();i++){
           tempMap.put(tempList.get(i).ToString(),tempList.get(i));
        }
        Object[] temp =  tempMap.values().toArray();
        for (int i=0;i<temp.length;i++){
            result.add((CheckObjectVO)temp[i]);
        }
        return result;
    }

	// ================ end ================
	
	public void test(int ...bytes) {
		List<Byte> list = new ArrayList<Byte>();
		for(int b : bytes) {
			list.add((byte)(b & 0xFF));
		}
		boolean flag = canHu(list, -1);
		System.out.println("   flag  " + flag);
	}
	
	public static void main(String[] args) {
		new BaseMJRule().test(1,1,2,2,2,3,4,5);
	}
}

class CheckObjectVO {
    public int[] paiArray;
    public int isJiang;
    public String ToString(){
        String result = "";
        if(paiArray != null){
            for(int i=0;i<paiArray.length;i++){
                result += paiArray[i];
            }
        }
        return  result;
    }
}