package com.buding.hall.network.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import packet.game.Hall.MallProductModel;
import packet.game.Hall.MallProductResponse;
import packet.msgbase.MsgBase.PacketType;

import com.buding.common.token.TokenClient;
import com.buding.hall.config.ConfigManager;
import com.buding.hall.config.ProductConfig;
import com.buding.hall.helper.HallPushHelper;
import com.buding.hall.module.shop.channel.ChannelRepostory;
import com.buding.hall.module.user.helper.UserSecurityHelper;

@Component
public class MallProductCmd extends HallCmd {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected ChannelRepostory channelRepostory;
	
	@Autowired
	protected TokenClient tokenClient;
	
	@Autowired
	protected UserSecurityHelper userSecurityHelper;
	
	@Autowired
	protected HallPushHelper pushHelper;
	
	@Autowired
	ConfigManager configManager;
	
	@Override
	public void execute(CmdData data) throws Exception {
		int userId = data.session.userId;
		if(userId == 0) {
			pushHelper.pushErrorMsg(data.session, PacketType.GenOrderResponse, "用户未登录");
			return;
		}
		
		
		MallProductResponse.Builder mb = MallProductResponse.newBuilder();
		//排序
		Set<Entry<String, ProductConfig>> entry = configManager.shopItemConfMap.entrySet();
		List<Entry<String, ProductConfig>> list = new ArrayList<Entry<String, ProductConfig>>(entry);
		Collections.sort(list, new Comparator<Entry<String, ProductConfig>>(){
					public int compare(Entry<String, ProductConfig> o1, Entry<String, ProductConfig> o2){
						return o1.getKey().compareTo(o2.getKey());
					}
				});
		for(Entry<String, ProductConfig> mapping : list) {
			ProductConfig prd = mapping.getValue();
			if(prd.status == 1) {
				MallProductModel.Builder m = MallProductModel.newBuilder();
				m.setCategory(prd.category);
				m.setId(prd.id);
				m.setImage(prd.img);
				m.setItemCount(prd.cItemCount);
				m.setName(prd.name);
				m.setPrice(prd.price.currenceCount);
				mb.addProducts(m);
			}
			System.out.println("  for  shopitem  " + prd.name + " , " + prd.price.currenceCount + " , " + prd.cItemCount);
		}
		
		pushHelper.pushPBMsg(data.session, PacketType.MallProductResponse, mb.build().toByteString());
	}

	@Override
	public PacketType getKey() {
		return PacketType.MallProductRequest;
	}	
}
