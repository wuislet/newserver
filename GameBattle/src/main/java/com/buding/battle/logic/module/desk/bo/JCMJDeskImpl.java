package com.buding.battle.logic.module.desk.bo;

import com.buding.api.desk.JCMJDesk;
import com.buding.battle.logic.module.desk.listener.DeskListener;
import com.buding.battle.logic.module.room.bo.Room;
import com.buding.hall.config.DeskConfig;

/**
 * @author tiny qq_381360993
 * @Description: 金昌麻将桌
 * 
 */
public class JCMJDeskImpl extends MJDeskImpl implements JCMJDesk<byte[]> {
	public JCMJDeskImpl(DeskListener listener, Room room, DeskConfig deskConf, String deskId) {
		super(listener, room, deskConf, deskId);
	}

	@Override
	public boolean canShuaiJiuYao() {
		return true;
	}

	@Override
	public boolean canShouPao() {
		return true;
	}
}
