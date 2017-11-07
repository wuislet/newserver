package com.buding.api.desk;

/**
 * @author wuislet
 * @Description:
 * 
 */
public interface JCMJDesk<T> extends MJDesk<T> {
	boolean canShuaiJiuYao(); //能否甩九幺
	
	boolean canShouPao(); //能否收炮
}
