package com.buding.mj;

import com.buding.api.desk.MJDesk;
import com.buding.game.GameLogiController;

/**
 * @author wuislet
 * @Description: 麻将
 * 
 */
public class MJServer extends GameLogiController {
	public MJServer() {
		this.m_dispatcher = new MJStateDispatcher<MJDesk>();
	}
}

