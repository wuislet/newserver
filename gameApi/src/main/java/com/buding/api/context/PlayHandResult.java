package com.buding.api.context;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class PlayHandResult {//TODO WXD check 清理掉这个类
	public static int GAME_RESULT_WIN = 1;
	public static int GAME_RESULT_LOSE = 2;
	public static int GAME_RESULT_EVEN = 3;
	
	public int pos;
	public int playerId;
	public String playerName;
	public int fanType = 0;// 当前这把牌的输赢牌型，庄，门清，放炮
	public int fanNum = 0;// 几番,倍数
	private int score = 0; // 本次输赢积分。//TODO wxd fantype 变成readonly，由setFanNum计算。
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	
	public String fanDesc = null; //番数描述
	public int result; // 1 win 2 lose 3 even
	public int tax;
	public String handcards = null;
	public String downcards = null;
}
