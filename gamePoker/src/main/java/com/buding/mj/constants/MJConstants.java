package com.buding.mj.constants;

public class MJConstants {

	
	public static final int MJPlayerCount = 4;
	
	
	/*
	 * 状态值定义
	 */
	public static final int MJStateReady = 1; //准备
	public static final int MJStatePrapareDeal = 2; //准备发牌
	public static final int MJStateDeal = 3; //洗牌、发牌、处理底分、台费、宝牌
	public static final int MJStateOriginCard = 4; //第一次看到手牌时的操作   比如换三张、丢牌、选缺门等
	public static final int MJStateRun = 5; //开始牌局
	public static final int MJStateFinish = 6; //结算

	/*
	 * 玩家状态
	 */
//	public static final int Player_Off = 1; ///玩家离线
//	public static final int Player_On = 2; ///玩家在线
//	public static final int Player_Auto = 3; ///玩家托管
	
	
	/**状态非法*/
	public static final int STATE_INVALID= 0;
	
	/**桌子状态非法*/
	public static final int TABLE_STATE_INVALID= 0;
	
	/**玩家的麻将操作*/
	public static final int MAHJONG_OPERTAION_CHI =0x01;//吃
	public static final int MAHJONG_OPERTAION_PENG =0x02;//碰
	public static final int MAHJONG_OPERTAION_AN_GANG =0x04;//暗杠
	public static final int MAHJONG_OPERTAION_BU_GANG =0x08;//补杠  自己碰了之后补杠
	public static final int MAHJONG_OPERTAION_ZHI_GANG =0x10;//直杠 别人打出的被我直接杠上
	public static final int MAHJONG_OPERTAION_CHU =0x20;//出牌
	public static final int MAHJONG_OPERTAION_HU =0x40;//胡牌
	public static final int MAHJONG_OPERTAION_TING =0x80;//听牌
	public static final int MAHJONG_OPERTAION_CANCEL=0x100;//给玩家提示操作，玩家点取消
	public static final int MAHJONG_OPERTAION_TING_CHU_LIANG =0x200;//听的时候出、亮牌

	public static final int MAHJONG_OPERTAION_CHI_TING=0x40000;//吃了直接听，吃听
	public static final int MAHJONG_OPERTAION_SHOUPAO=0x800000;//收炮
	public static final int MAHJONG_OPERTAION_SHUAIJIUYAO=0x1000000;//甩九幺
	public static final int MAHJONG_OPERTAION_PENG_TING =  0x2000000;//碰听
	public static final int MAHJONG_OPERTAION_ZD_TING =0x20000000;// 支对听
	public static final int MAHJONG_OPERTAION_MO =0x40000000;// 摸牌
	public static final int MAHJONG_OPERTAION_CHNAGE_BAO =0x80000000;// 换宝
	
	//花色编码
	public static final byte MAHJONG_CODE_HONG_ZHONG =0x39;//红中的编码
	public static final int MAHJONG_CODE_COLOR_SHIFTS =4;//花色部分的移位，花色，【0，1，2】
	public static final int MAHJONG_CODE_COLOR_MASK =0xf0;//花色部分的掩码
	public static final int MAHJONG_CODE_NUMBER_MASK =0x0f;//数字部分的掩码
	public static final int MAHJONG_CODE_GANG_CARD =0x0f;//杠的特殊数字标识！ //TODO 技术债务
	
	/**玩家牌局结果*/
	public static final int MAHJONG_HU_CODE_DIAN_PAO =0x0001;//点炮  //TODO wxd hutype 与胡型分开
	public static final int MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA=0x0002;//自己是不是庄家
	public static final int MAHJONG_HU_CODE_ZI_MO=0x0004;//自摸
	public static final int MAHJONG_HU_CODE_TING=0x010;//是否听牌
	public static final int MAHJONG_HU_CODE_TARGET_ZHUANG_JIA=0x020;//输赢的对方是庄家

	public static final int MAHJONG_HU_CODE_TIAN_HU=0x0040;//天胡
	public static final int MAHJONG_HU_CODE_MEN_QING =0x0080;//门清
	public static final int MAHJONG_HU_CODE_HAI_DI_LAO=0x100;//海底捞月
	public static final int MAHJONG_HU_CODE_GANG_HUA=0x0200;//杠花
	public static final int MAHJONG_HU_CODE_QING_YI_SE=0x400;//清一色
	public static final int MAHJONG_HU_CODE_QING_LONG=0x800;//清龙(清一色一条龙)
	public static final int MAHJONG_HU_CODE_QI_XIAO_DUI=0x1000;//七小对
	public static final int MAHJONG_HU_CODE_DUI_DUI_HU=0x2000;//对对胡
	public static final int MAHJONG_HU_CODE_JIA_HU=0x008;//夹胡
	public static final int MAHJONG_HU_CODE_DAN_DIAO=0x4000;//单吊(吊独将)
	public static final int MAHJONG_HU_CODE_JIA_BIAN=0x8000;//夹边
	//绝张

	public static final int MAHJONG_HU_CODE_YI_TIAO_LONG=0x10000;//一条龙
	public static final int MAHJONG_HU_CODE_SHUAI_JIU_ZHANG=0x20000;//甩九张
	public static final int MAHJONG_HU_CODE_KAIPAIZHA=0x40000;// 开炸牌
	
	public static final int MAHJONG_HU_CODE_WIN=0x100000;//赢 //TODO wxd hutype 与胡型分开
	public static final int MAHJONG_HU_CODE_LOSE=0x200000;//输
	public static final int MAHJONG_HU_CODE_LIUJU=0x400000;//流局
	public static final int MAHJONG_HU_CODE_SHOUPAO=0x800000;//收炮
	
	/**玩家已经满，准备开始**/
	public static final int GAME_TABLE_STATE_READY_GO=2;
	
	/**客户端收到起手牌，播放动画，给客户端一定时间**/
	public static final int GAME_TABLE_WAITING_CLIENT_SHOW_INIT_CARDS=3;
	
	/**玩家玩牌中**/
	public static final int GAME_TABLE_STATE_PLAYING=4;
	/**游戏结束，等待客户端显示game over界面**/
	public static final int GAME_TABLE_STATE_SHOW_GAME_OVER_SCREEN=5;
		
	/**有玩家离开桌子，暂停游戏*/
	public static final int GAME_TABLE_STATE_PAUSED=7;
	
	/**玩家吃碰之类的操作，服务器等客户端播个动画*/
	public static enum GAME_TABLE_SUB_STATE_ANIMATION{ //客户端正在播放的动画 //TODO WXD 将常数改用枚举。技术债务。
		idle,               //无任何操作
		chiPeng,            //吃碰牌动画
		hu,                 //胡牌动画
		chu,                //出牌动画
		ting,               //听牌动画
		initCard,           //发牌动画
		showChangeBao,      //换宝动画
		waitChangeBao,      //准备推送上宝/换宝动画
		gang,               //杠牌动画
	}
	public static final int GAME_TABLE_SUB_STATE_IDLE=0;//无任何操作
	public static final int GAME_TABLE_SUB_STATE_PLAYING_CHI_PENG_ANIMATION=1;//客户端在播吃碰牌动画
	public static final int GAME_TABLE_SUB_STATE_PLAYING_HU_ANIMATION=2;//客户端在播胡牌动画
	public static final int GAME_TABLE_SUB_STATE_PLAYING_CHU_ANIMATION=3;//客户端在播出牌动画
	public static final int GAME_TABLE_SUB_STATE_PLAYING_TING_ANIMATION=4;//客户端在播听牌动画
	public static final int GAME_TABLE_SUB_STATE_SHOW_INIT_CARDS=5;//客户端在播发牌动画
	public static final int GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO=6;//客户端在播换宝动画
	public static final int GAME_TABLE_SUB_STATE_WAIT_CHANGE_BAO=7;//准备推送上宝/换宝动画
	public static final int GAME_TABLE_SUB_STATE_PLAYING_GANG_ANIMATION=8;//客户端在播杠牌动画
	public static final int GAME_TABLE_SUB_STATE_PLAYING_CHU_TING_ANIMATION=9;//客户端在播出亮牌动画
		
	public static final int SEND_TYPE_SINGLE = 1;//发给单人
	public static final int SEND_TYPE_ALL = 2;//发给所有人
	public static final int SEND_TYPE_EXCEPT_ONE = 3;//发给除某人外其他人
	
	public static final int MAHJONG_CANCEL_OPER_OTHER_TURN = 0x01;	 //取消了其他人回合的操作。如碰，吃。
	public static final int MAHJONG_CANCEL_OPER_SELF_TURN = 0x02;    //取消了自己回合的操作。如暗杠，听
}
