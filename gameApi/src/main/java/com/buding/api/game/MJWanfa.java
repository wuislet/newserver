package com.buding.api.game;

/**
 * @author wuislet
 * @Description:
 * 
 */
public interface MJWanfa {
	public int FEN_PAI = 0x1;
	public int BAO_TING = 0x2;
	public int CAN_CHI = 0x4;
	public int QI_XIAO_DUI = 0X8;
	public int HAS_GUI = 0x10;
	public int ZI_MO_TYPE = 0x20;
	public int QING_YI_SE_YI_TIAO_LONG = 0x1000;  //TODO wxd rule 非泛型。
	public int SHUAI_JIU_YAO = 0X2000;            //TODO wxd rule 非泛型。
	public int SHOU_PAO = 0X4000;                 //TODO wxd rule 非泛型。
	public int ALL = FEN_PAI | BAO_TING | CAN_CHI | QI_XIAO_DUI | HAS_GUI | ZI_MO_TYPE | QING_YI_SE_YI_TIAO_LONG | SHUAI_JIU_YAO | SHOU_PAO;
}
