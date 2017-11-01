package com.buding.mj.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * @Description:
 * 
 */
public class MjCheckResult {
	public List<Integer> shunzis = new ArrayList<Integer>();
	public List<Integer> kezis = new ArrayList<Integer>();
	public int special = 0; // 特殊胡型 1-七小对; 2-龙七对(有一个杠的七对); 3-双龙七对(有两个杠的七对); 4-三龙七对(有三个杠的七对); 10-十三幺
}
