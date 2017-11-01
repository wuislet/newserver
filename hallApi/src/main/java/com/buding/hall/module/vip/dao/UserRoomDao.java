package com.buding.hall.module.vip.dao;

import java.util.List;

import com.buding.db.model.UserRoom;
import com.buding.db.model.UserRoomGameTrack;
import com.buding.db.model.UserRoomResult;
import com.buding.db.model.UserRoomResultDetail;

public interface UserRoomDao {
	public UserRoom getUserRoom(int userId, String matchId);
	public void updateUserRoom(UserRoom room);
	public long addUserRoom(UserRoom room);
	public UserRoom get(long roomId);
	public UserRoom getByCode(String roomCode);
	public boolean isRoomExists(String roomCode);
	public List<UserRoom> getMyRoomList(int playerId);
	public int getMyRoomListCount(int playerId);
	public void updateLastActiveTime(String roomCode);
	
	public void insertUserRoomResultDetail(UserRoomResultDetail detail);
	
	public void insertUserRoomResult(UserRoomResult detail);
	
	public List<UserRoomResult> getUserRoomResultList(long userId);
	
	public List<UserRoomResultDetail> getUserRoomResultDetailList(long roomId);
	
	public void insertUserRoomGameTrack(UserRoomGameTrack track);
}
