package com.buding.battle.logic.module.desk.bo;

import com.buding.api.desk.MJDesk;
import com.buding.api.game.MJWanfa;
import com.buding.battle.logic.module.desk.listener.DeskListener;
import com.buding.battle.logic.module.room.bo.Room;
import com.buding.hall.config.DeskConfig;

/**
 * @author wuislet
 * @Description: 麻将桌
 * 
 */
public class MJDeskImpl extends RobotSupportDeskImpl implements MJDesk<byte[]> {
	protected int wanfa = 0;
	
	public MJDeskImpl(DeskListener listener, Room room, DeskConfig deskConf, String deskId) {
		super(listener, room, deskConf, deskId);
		wanfa = MJWanfa.ALL;
	}
	
	@Override
	public int getCardType(){
		return (wanfa & MJWanfa.FEN_PAI) != 0 ? 0x1 : 0x0;
	}
	
	@Override
	public int getTingType() {
		return (wanfa & MJWanfa.BAO_TING) != 0 ? 2 : 3;
	}
	
	@Override
	public int getGui(){
		return (wanfa & MJWanfa.HAS_GUI) != 0 ? 10 : -1;
	}
	
	@Override
	public boolean canQiXiaoDui() {
		return (wanfa & MJWanfa.QI_XIAO_DUI) != 0;
	}

	@Override
	public boolean canShuaiJiuYao() {
		return (wanfa & MJWanfa.SHUAI_JIU_YAO) != 0;
	}

	@Override
	public boolean canShouPao() {
		return (wanfa & MJWanfa.SHOU_PAO) != 0;
	}
	
	@Override
	public boolean havetoZiMo() {
		return (wanfa & MJWanfa.ZI_MO_TYPE) != 0;
	}

	@Override
	public boolean qinYiSeYiTiaoLong() {
		return (wanfa & MJWanfa.QING_YI_SE_YI_TIAO_LONG) != 0;
	}

	@Override
	public int getWanfa() {
		return wanfa;
	}

	@Override
	public int getRoomType() {
		return 2;
	}

	@Override
	public int getTotalQuan() {
		return 1;
	}
}
