package com.buding.battle.logic.module.desk.bo;

import com.buding.api.desk.DQMJDesk;
import com.buding.api.game.DQMJWanfa;
import com.buding.battle.logic.module.desk.listener.DeskListener;
import com.buding.battle.logic.module.room.bo.Room;
import com.buding.hall.config.DeskConfig;
import com.buding.mj.constants.MJConstants;

/**
 * @author tiny qq_381360993
 * @Description: 大庆麻将桌
 * 
 */
public class DQMJDeskImpl extends MJDeskImpl implements DQMJDesk<byte[]> {
	public DQMJDeskImpl(DeskListener listener, Room room, DeskConfig deskConf, String deskId) {
		super(listener, room, deskConf, deskId);
		wanfa = DQMJWanfa.ALL - DQMJWanfa.WUJIA_BUHU;
	}
	
	@Override
	public int getCardType() {
		return 0x1;
	}

	@Override
	public int getGui(){
		return -1;
		//return (int)MJConstants.MAHJONG_CODE_HONG_ZHONG;
	}

	@Override
	public boolean canKaiPaiZha() {
		return (wanfa & DQMJWanfa.KAI_PAI_ZHA) == DQMJWanfa.KAI_PAI_ZHA;
	}

	@Override
	public boolean canHZMTF() {
		return (wanfa & DQMJWanfa.HONGZHONG_MTF) == DQMJWanfa.HONGZHONG_MTF;
	}

	@Override
	public boolean canDanDiaoJia() {
//		return (wanfa & DQMJWanfa.DANDIAO) == DQMJWanfa.DANDIAO;
		return true;
	}

	@Override
	public boolean can37Jia() {
//		return (wanfa & DQMJWanfa.JIA37) == DQMJWanfa.JIA37;
		return true;
	}

	@Override
	public boolean canZhiDuiHU() {
		return (wanfa & DQMJWanfa.ZHIDUI) == DQMJWanfa.ZHIDUI;
	}

	@Override
	public boolean canGuaDaFeng() {
		return (wanfa & DQMJWanfa.GUA_DA_FENG) == DQMJWanfa.GUA_DA_FENG;
	}

	@Override
	public boolean canWuJiaBuHu() {
		return (wanfa & DQMJWanfa.WUJIA_BUHU) == DQMJWanfa.WUJIA_BUHU;
	}

	@Override
	public boolean canDaiLouDe() {
		return (wanfa & DQMJWanfa.DAI_LOU) == DQMJWanfa.DAI_LOU;
	}
	@Override
	public boolean canDuidao() {
		return (wanfa & DQMJWanfa.DUI_DAO) == DQMJWanfa.DUI_DAO;
	}
	@Override
	public boolean canGunBao() {
		return (wanfa & DQMJWanfa.GUN_BAO) == DQMJWanfa.GUN_BAO;
	}
}
