package com.buding.test;

import packet.user.User.LoginResponse;

/**
 * @author vinceruan qq_404086388
 * @Description:
 * 
 */
public class PlayerData {
	public LoginResponse loginData;
//	public AuthResponse gameAuthData;
//	public AuthResponse msgAuthData;
	public boolean authGame;
	public boolean authMsg;
	public GamingData gamingData;
	
	public String gameServerAddr;
	public String msgServerAddr;
	public String hallServerAddr;
	
}
