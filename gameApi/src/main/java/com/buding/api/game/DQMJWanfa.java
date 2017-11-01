package com.buding.api.game;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public interface DQMJWanfa {
	public int KAI_PAI_ZHA = 0X1;
	public int ZHIDUI = 0X2;
	public int JIA37 = 0x4;
	public int DANDIAO = 0x8;
	public int GUA_DA_FENG = 0X10;
	public int HONGZHONG_MTF = 0X20;
	public int DAI_LOU = 0x40;
	public int WUJIA_BUHU = 0X80;
	public int DUI_DAO = 0X100;
	public int GUN_BAO = 0X200;
	public int ALL = KAI_PAI_ZHA | JIA37 | ZHIDUI | DANDIAO | GUA_DA_FENG | HONGZHONG_MTF | DAI_LOU | WUJIA_BUHU | DUI_DAO | GUN_BAO;
}
