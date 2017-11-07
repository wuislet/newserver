package com.buding.api.desk;

/**
 * @author wuislet
 * @Description:
 * 
 */
public interface MJDesk<T> extends Desk<T> {
	int getCardType(); //所有可用牌类。0x1-风牌标记; 0x2-花牌标记; 0x4-鬼牌不在可用牌内 
	
	int getTingType(); //0x1-有直接胡，0x2-有听。 e.p. 3-无限制, 2-必须听了才能胡, 1-没有听，直接胡, 0-不能胡 
	
	int getGui(); //获取万能牌 -1表示没有
	
	int getWanfa(); //获取玩法
	
	boolean canQiXiaoDui(); //能否七小对
	
	boolean canShuaiJiuYao(); //能否甩九幺 //TODO 非通用，须特殊化
	
	boolean canShouPao(); //能否收炮 //TODO 非通用，须特殊化
	
	int getRoomType(); // 1:2人麻将 2:4人麻将
	
	int getTotalQuan();

}
