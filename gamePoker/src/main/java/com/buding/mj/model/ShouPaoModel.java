package com.buding.mj.model;

import com.buding.api.player.PlayerInfo;

/**
 * @author wuislet
 * @Description:
 * 
 */
public class ShouPaoModel {
	private int winPosition = -1;
	private int paoPosition = -1;
	private int score = 0;
	private byte card = 0;
	private int cnt = 0;
	private int paoCnt = 0;
	public int getWinPosition() {
		return winPosition;
	}
	public void setWinPosition(int winPosition) {
		this.winPosition = winPosition;
	}
	public int getPaoPosition() {
		return paoPosition;
	}
	public void setPaoPosition(int paoPosition) {
		this.paoPosition = paoPosition;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public byte getCard() {
		return card;
	}
	public void setCard(byte card) {
		this.card = card;
	}
	public int getCnt() {
		return cnt;
	}
	public void setCnt(int cnt) {
		this.cnt = cnt;
	}
	public int getPaoCnt() {
		return paoCnt;
	}
	public void setPaoCnt(int paoCnt) {
		this.paoCnt = paoCnt;
	}
	public void addPaoScore(int score) { //这个类本意不是用做总统计的，所以这个方法应该要去掉。
		this.score += score;
		if(score > 0) {
			this.cnt++;
		} else {
			this.paoCnt++;
		}
	}
}
