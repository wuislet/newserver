package com.buding.hall.network.cmd;

import java.text.SimpleDateFormat;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import packet.game.Hall.PlayerScoreModel;
import packet.game.Hall.RoomResultModel;
import packet.game.Hall.RoomResultRequest;
import packet.game.Hall.RoomResultResponse;
import packet.msgbase.MsgBase.PacketBase;
import packet.msgbase.MsgBase.PacketType;

import com.buding.db.model.UserRoomResult;
import com.buding.db.model.UserRoomResultDetail;
import com.buding.hall.helper.HallPushHelper;
import com.buding.hall.module.user.service.UserService;
import com.buding.hall.module.vip.dao.UserRoomDao;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
@Component
public class UserRoomResultCmd extends HallCmd {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	UserService userService;

	@Autowired
	HallPushHelper pushHelper;

	@Autowired
	UserRoomDao userRoomDao;

	@Override
	public void execute(CmdData data) throws Exception {
		PacketBase packet = data.packet;
		RoomResultRequest ur = RoomResultRequest.parseFrom(packet.getData());
		long roomId = ur.getRoomId();
		long userId = data.session.userId;
		if(roomId == 0) { //查看总战绩
			List<UserRoomResult> list = userRoomDao.getUserRoomResultList(userId);
			RoomResultResponse.Builder rb = RoomResultResponse.newBuilder();
			for(UserRoomResult model : list) {
				RoomResultModel.Builder bb  = RoomResultModel.newBuilder();
				bb.setPlayerTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(model.getEndTime()));
				bb.setRoomCode(model.getRoomName());
				bb.setRoomName(model.getRoomName());
				bb.setRoomId(model.getRoomId());
				JSONArray ja = JSONArray.fromObject(model.getDetail());
				for(int i = 0; i < ja.size(); i++) {
					JSONObject obj = ja.getJSONObject(i);
					PlayerScoreModel.Builder score = PlayerScoreModel.newBuilder();
					score.setPlayerId(obj.getLong("playerId"));
					score.setPlayerName(obj.getString("playerName"));
					score.setScore(obj.getInt("score"));
					bb.addPlayerScore(score);
				}
				rb.addList(bb);
			}
			
			pushHelper.pushRoomResultResponse(data.session, rb.build());
		} else { //查看某个房间的
			List<UserRoomResultDetail> list = userRoomDao.getUserRoomResultDetailList(roomId);
			RoomResultResponse.Builder rb = RoomResultResponse.newBuilder();
			for(UserRoomResultDetail model : list) {
				RoomResultModel.Builder bb  = RoomResultModel.newBuilder();
				bb.setPlayerTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(model.getEndTime()));
				bb.setRoomCode(model.getRoomName());
				bb.setRoomName(model.getRoomName());
				bb.setRoomId(model.getRoomId());
				JSONArray ja = JSONArray.fromObject(model.getDetail());
				for(int i = 0; i < ja.size(); i++) {
					JSONObject obj = ja.getJSONObject(i);
					PlayerScoreModel.Builder score = PlayerScoreModel.newBuilder();
					score.setPlayerId(obj.getLong("playerId"));
					score.setPlayerName(obj.getString("playerName"));
					score.setScore(obj.getInt("score"));
					bb.addPlayerScore(score);
				}
				rb.addList(bb);
			}
			pushHelper.pushRoomResultResponse(data.session, rb.build());
		}
	}

	@Override
	public PacketType getKey() {
		return PacketType.RoomResultRequest;
	}

}
