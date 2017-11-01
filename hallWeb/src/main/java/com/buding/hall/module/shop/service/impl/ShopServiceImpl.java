package com.buding.hall.module.shop.service.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alipay.api.internal.util.AlipaySignature;
import com.buding.common.admin.component.BaseComponent;
import com.buding.common.result.Result;
import com.buding.db.model.UserOrder;
import com.buding.hall.config.ConfigManager;
import com.buding.hall.config.ItemPkg;
import com.buding.hall.config.ProductConfig;
import com.buding.hall.helper.AlipayConfig;
import com.buding.hall.helper.AlipayNotify;
import com.buding.hall.helper.HallPushHelper;
import com.buding.hall.helper.HttpUtils;
import com.buding.hall.helper.WXUtil;
import com.buding.hall.module.constants.Constants;
import com.buding.hall.module.item.service.ItemService;
import com.buding.hall.module.item.type.ItemChangeReason;
import com.buding.hall.module.order.dao.UserOrderDao;
import com.buding.hall.module.shop.OrderStatus;
import com.buding.hall.module.shop.service.ShopService;

/**
 * 商店服务处理类,处理购买请求
 * @author Administrator
 *
 */
@Component
public class ShopServiceImpl extends BaseComponent implements ShopService {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ConfigManager configManager;
	
	@Autowired
	ItemService itemService;

	@Autowired
	UserOrderDao userOrderDao;
	
	@Autowired
	HallPushHelper pushHelper;
	
	/**
	 * 购买道具
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@Override
	public Result buyItem(int userId, String id) throws Exception {
		ProductConfig itemConf = configManager.getItemConf(id);
		if (itemConf == null) {
			return Result.fail("购买失败,道具不存在");
		}

		// 生成流水号
		String no = System.currentTimeMillis() + "";

		logger.info("playerId={};id={};no={};act=tryBuyItem;", userId, id, no);
		// TODO 通过第三方支付平台进行购买

		return Result.success();
	}

	/**
	 * 购买成功回调
	 * 
	 * @param userId
	 * @param id
	 * @throws Exception
	 */
	@Override
	public Result finishOrder(String no, boolean result) throws Exception {
		logger.info("no={};result={};act=onBuyResultCallback;", no, result);
		
		if(!result) {
			logger.info("ignore invalid order {} ", no);
			return Result.success();
		}
		
		UserOrder order = userOrderDao.getByOrderId(no);
		if(order == null) {
			logger.error("act=finishOrderNotFound;no={};", no);
			return Result.fail("订单不存在");
		}
		
		if(order.getOrderStatus() != OrderStatus.WAITING) {
			logger.error("act=finishOrderStatusError;no={};status={}", no, order.getOrderStatus());
			return Result.fail("无效的订单");			
		}
		
		if(result) {
			int userId = order.getUserId();
			String id = order.getProductId();
			ProductConfig conf = configManager.getItemConf(id);
			
			itemService.addItem(userId, ItemChangeReason.UserBuyItem, "UserBuy:" + id, conf.items);
			for (ItemPkg d : conf.items) {
				logger.info("playerId={};id={};no={};itemType={};args={};count={};act=addItemOk;", userId, id, no, d.baseConf.itemType, d.baseConf.getArgument(), d.count);
			}
			logger.info("playerId={};id={};no={};result={};act=onAddBuyItemEnd;", userId, id, no, result);
			order.setOrderStatus(OrderStatus.END);
		} else {
			order.setOrderStatus(OrderStatus.FAIL);
		}
		
		userOrderDao.update(order);
		
		pushHelper.pushUserInfoSyn(order.getUserId());
		
		return Result.success();		
	}
	
	@Override
	public String genOrderId(String prefix) {
		int i = 0;
		while(i<100) {
			i++;
			String orderId = prefix + System.currentTimeMillis();
			UserOrder order = userOrderDao.getByOrderId(orderId);
			if(order == null) {
				return orderId;
			}
		}
		return null;
	}

	@Override
	public String getComponentName() {
		return "ShopService";
	}

	@Override
	public String processWxPayCallback(String strInfo) throws Exception {
		Map<String , String> returnMap = new HashMap<String, String>();
		//需要检验签名
		Map<String, String> requestMap = WXUtil.xml2Map(strInfo);
		
		String orderId = null;
		double totalFee = 0;
		
		if(WXUtil.CheckSign(requestMap, Constants.WX_MCH_SECRET))
		{
			if(requestMap.get("return_code").equals("SUCCESS"))
			{
				orderId = requestMap.get("out_trade_no");
				totalFee = Double.valueOf(requestMap.get("total_fee"));
				//签名校验成功.
				//收到支付回调，并且校验签名成功，需要根据订单id，去微信服务器查询订单.
				//我这边因为是模拟出来的数据，所以查询订单肯定是失败的。
				Map<String , String> searchOrderMap = new HashMap<String, String>();
				searchOrderMap.put("appid", requestMap.get("appid"));
				searchOrderMap.put("mch_id", Constants.WX_MCH_ID);
				searchOrderMap.put("transaction_id", requestMap.get("transaction_id"));
				searchOrderMap.put("out_trade_no",requestMap.get("out_trade_no"));
				searchOrderMap.put("nonce_str",requestMap.get("nonce_str"));
				searchOrderMap.put("sign", WXUtil.getSign(searchOrderMap, Constants.WX_MCH_SECRET));
				
				String restxml = HttpUtils.post(Constants.SEARCH_ORDER_PAY, WXUtil.map2xml(searchOrderMap));
				//校验签名
				System.out.println(restxml);
				Map<String , String> retMap = WXUtil.xml2Map(restxml);
				if(WXUtil.CheckSign(retMap, Constants.WX_MCH_SECRET))
				{
					//校验签名成功
					if(retMap.containsKey("return_code") && retMap.get("return_code").equals("SUCCESS"))
					{
						//取出这边的订单内的信息，然后处理添加道具之类的逻辑
						this.finishOrder(orderId, true);
						returnMap.put("return_code", "SUCCESS");
						returnMap.put("return_msg", "OK");
					}
					else
					{
						returnMap.put("return_code", retMap.get("return_code"));
						returnMap.put("return_msg", retMap.get("return_msg"));
					}
				}
				else
				{
					returnMap.put("return_code", "FAIL");
					returnMap.put("return_msg", "REQUEST ORDER WITH WRONG SIGN");
				}
			}
			else
			{
				//微信支付失败,通知微信服务器，接收到了.
				returnMap.put("return_code", "SUCCESS");
				returnMap.put("return_msg", "OK");
			}
		}
		else
		{
			//签名不通过
			returnMap.put("return_code", "FAIL");
			returnMap.put("return_msg", "WRONG SIGN");
		}
		
		return WXUtil.map2xml(returnMap);
	}

	@Override
	public String processAliPayCallback(String data) throws Exception {
		JSONObject req = JSONObject.fromObject(data);
		
		Map<String, String> params = new HashMap<String, String>();
		
		for (Iterator iter = req.keys(); iter.hasNext();) {			
			String name = (String) iter.next();
			params.put(name, req.getString(name));
			logger.info(name+"=" + req.getString(name));
		}		

		// 获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以下仅供参考)//
		// 商户订单号
		String out_trade_no = req.getString("out_trade_no");

		// 支付宝交易号
		String trade_no = req.getString("trade_no");

		// 交易状态
		String trade_status = req.getString("trade_status");

		// 异步通知ID
		String notify_id = req.getString("notify_id");

		// sign
		String sign = req.getString("sign");

		if (notify_id != "" && notify_id != null) {
			if (AlipayNotify.verifyResponse(notify_id).equals("true"))// 判断成功之后使用getResponse方法判断是否是支付宝发来的异步通知。
			{
				if (AlipaySignature.rsaCheckV1(params, AlipayConfig.alipay_public_key, "utf8", AlipayConfig.sign_type))// 使用支付宝公钥验签
				{
					if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")) {
						// 业务处理
						finishOrder(out_trade_no, true);
					}

					return "success";
				} else// 验证签名失败
				{
					return "sign fail";
				}
			} else// 验证是否来自支付宝的通知失败
			{
				return "response fail";
			}
		} else {
			return "no notify message";
		}
	}
	
}
