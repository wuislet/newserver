package com.buding.common.monitor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Monitor {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public static List<Monitorable> objList = new ArrayList<Monitorable>();
	
	/**
	 * 加入监控列表
	 * @param obj
	 */
	public static void add2Monitor(Monitorable obj) {
		objList.add(obj);
	}
	
	/**
	 * 定时监控
	 */
	public void task() {
		for(Monitorable component : objList) {
			try {
				component.check();
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
