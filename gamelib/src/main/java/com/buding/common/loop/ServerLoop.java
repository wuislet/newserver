package com.buding.common.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class ServerLoop implements InitializingBean, Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());

	ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	List<Looper> loopList = new ArrayList<Looper>();

	public int intervalMills = 10000;

	public void register(Looper loop) {
		this.loopList.add(loop);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		executor.scheduleWithFixedDelay(this, 1000, intervalMills, TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		for (Looper l : loopList) {
			try {
				logger.info("Loop tick " + l.getClass());
				l.loop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getIntervalMills() {
		return intervalMills;
	}

	public void setIntervalMills(int intervalMills) {
		this.intervalMills = intervalMills;
	}

}
