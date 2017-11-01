package com.buding.hall.module.ws;

import com.buding.common.result.Result;
import com.buding.common.result.TResult;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public interface BattlePortalBroadcastService {
	// 设置回放数据
	public Result setDeskRelayData(String instanceId, int playerId, String data);

	// 停止服务
	public void stopService(String instanceId);

	// 恢复服务
	public void startService(String instanceId);

	// 剔除玩家
	public void kickPlayer(String instanceId, int playerId);

	// 获取桌子列表
	public String getDeskList(String instanceId);

	// 重新加载配置
	public void reloadMatchConf(String serverPattern);

	// 停服
	public void stopServer(String instanceId);

	// 清除卡桌
	public Result clearDesk(String instanceId, int userId);

	//结算桌子
	public Result dismissDesk(String instanceId, String gameId, String matchId, String deskId);
	
	public TResult<String> dump(String instanceId, String gameId, String matchId, String deskId);
	
	public String getStatus();
	
	public String searchDesk(int playerId);
}
