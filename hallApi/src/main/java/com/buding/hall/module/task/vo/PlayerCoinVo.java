package com.buding.hall.module.task.vo;

public class PlayerCoinVo {
	private int userId;
	private int coin;

	public PlayerCoinVo(int userId, int coin) {
		this.userId = userId;
		this.coin = coin;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getCoin() {
		return coin;
	}

	public void setCoin(int coin) {
		this.coin = coin;
	}

	

}
