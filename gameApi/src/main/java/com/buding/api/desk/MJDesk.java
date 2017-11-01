package com.buding.api.desk;

/**
 * @author wuislet
 * @Description:
 * 
 */
public interface MJDesk<T> extends Desk<T> {
	boolean isQiXiaoDui(); //能否七小对
	
	int getCardType(); //所有可用牌类。0x1-风牌标记; 0x2-花牌标记; 0x4-鬼牌不在可用牌内 
	
	int getGui(); //获取万能牌 -1表示没有
	
	int getWanfa(); //获取玩法
	
	int getRoomType(); // 1:2人麻将 2:4人麻将
	
	int getTotalQuan();

}
