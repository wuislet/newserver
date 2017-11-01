package com.buding.api.desk;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public interface DQMJDesk<T> extends MJDesk<T> {
	boolean canKaiPaiZha(); // 是否可以开牌炸
	
	boolean canHZMTF(); //是否可以红中满天飞
	
	boolean canDanDiaoJia(); //是否单调夹
	
	boolean can37Jia(); //37夹
	
	boolean canZhiDuiHU(); //支对
	
	boolean canGuaDaFeng(); //刮大风
	
	boolean canWuJiaBuHu(); //无夹不胡
	
	boolean canDaiLouDe(); //带漏的

	boolean canDuidao();//对倒

	boolean canGunBao();//滚宝
}
