package com.buding.api.game;

/**
 * @author wuislet
 * @Description:
 * 
 */
public interface MJWanfa {
	public int FEN_PAI = 0x1;
	public int BAO_TING = 0x2;
	public int QI_XIAO_DUI = 0X4;
	public int HAS_GUI = 0x8;
	public int ZI_MO_TYPE = 0x10;
	public int QING_YI_SE_YI_TIAO_LONG = 0x20;  //TODO wxd rule 非泛型。
	public int SHUAI_JIU_YAO = 0X40;            //TODO wxd rule 非泛型。
	public int SHOU_PAO = 0X80;                 //TODO wxd rule 非泛型。
	public int ALL = FEN_PAI | BAO_TING | QI_XIAO_DUI | HAS_GUI | ZI_MO_TYPE | QING_YI_SE_YI_TIAO_LONG | SHUAI_JIU_YAO | SHOU_PAO;
}
