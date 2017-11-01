package com.buding.token.service;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.buding.common.admin.component.BaseComponent;
import com.buding.common.cache.CacheClient;
import com.buding.common.result.Result;
import com.buding.common.token.TokenServer;
import com.buding.common.util.DateUtil;
import com.buding.common.util.DesUtil;
import com.buding.db.model.User;
import com.buding.hall.module.user.dao.UserDao;

public class ClusterTokenServer extends BaseComponent implements TokenServer {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	UserDao userDao;
	
//	Map<Integer, String> tokenMap = new HashMap<Integer, String>();
//	Map<Integer, Long> tmpTimeMap = new HashMap<Integer, Long>();
	
	@Autowired
	CacheClient cacheClient;
	
	public String tmpTokenKey = "user_tmp_token";
	public String tokenKey = "user_token";
	
	@Override
	public Result verifyTmpToken(int userId, String token) {
		boolean t = verifyToken(userId, token);
		if(!t) {
			return Result.fail();
		}
		
		Long time = cacheClient.hget(tmpTokenKey, userId+"", Long.class);
		if(time == null) {
			return Result.fail("临时令牌不存在");
		}
		
		if(System.currentTimeMillis() - time >= 60*1000) {
			return Result.fail("登录会话过期,请重新进入页面");
		}
		cacheClient.hdel(tmpTokenKey, userId+"");
		return Result.success();
	}

	@Override
	public String updateToken(int userId, boolean tmp) {
		User user = userDao.getUser(userId);
		
		String token = UUID.randomUUID().toString().replace("-", "") + DateUtil.format(new Date(), "yyyyMMddhhmmss")+user.getId();
		
		if(logger.isInfoEnabled()) {
			logger.info("gen user {} token {}, tmp {}", user.getId(), token, tmp);
		}
		
		if(tmp) {
//			tmpTimeMap.put(userId, System.currentTimeMillis());
			cacheClient.hset(tmpTokenKey, userId+"", System.currentTimeMillis());
		} else {
			user.setToken(DesUtil.md5(token, 16));
			
			userDao.updateUser(user);
		}
		
//		tokenMap.put(userId, token);
		cacheClient.hset(tokenKey, userId+"", token);
				
		return token;
	}

	@Override
	public boolean verifyToken(int userId, String token) {
		logger.info("verify userid {} token {} instance {}", userId, token, this);
		String oldToken = cacheClient.hget(tokenKey, userId+"", String.class);
		if(oldToken != null) {
			if(token != null && token.equals(oldToken)) {
				return true;
			} else {
				logger.info("contain, but not equals, expect {} but found {}", oldToken, token);
				return false;
			}
		}
				
		User user = userDao.getUser(userId);
		if(user == null) {
			logger.info("user is null, userid {} ", userId);
			return false;
		}
		
		if(user.getToken() != null && user.getToken().equals(DesUtil.md5(token, 16))) {
			cacheClient.hset(tokenKey, userId+"", token);
			return true;
		}
		logger.error("not equals from db, userid {}", userId);
		return false;
	}

	@Override
	public String getComponentName() {
		return "tokenserver";
	}
}
