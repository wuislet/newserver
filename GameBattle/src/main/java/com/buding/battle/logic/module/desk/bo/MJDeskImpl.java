package com.buding.battle.logic.module.desk.bo;

import com.buding.api.desk.MJDesk;
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
	}
	
	@Override
	public int getCardType(){
		return 0x1;
	}
	
	@Override
	public boolean canQiXiaoDui() {
		return true;
	}
	
	@Override
	public int getGui(){
		return -1;
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
