package com.buding.api.context;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class PlayFinalResult {
	public int playerId;
	public String playerName;
	public String headImg;
	public int pos;
	public int score;
	public int bankerCount = 0;
	public int totalCount = 0;
	public int huCount = 0;
	public int paoCount = 0;
	public int mobaoCount = 0;
	public int baoZhongBaoCount = 0;
	public int kaiPaiZhaCount = 0;
	public List<Integer> eachScore = new ArrayList<Integer>();
}
