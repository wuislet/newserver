package com.buding.battle.logic.module.common;

public enum PlayerStatus {
	IN_HALL, //在大厅,默认状态
	ENROLL_OK, //报名成功
	ENTER_ROOM, //进入房间
	UNREADY, //坐下，处于未准备状态
	READY, //准备完成
	PRAPARE_DEAL, //可以发牌状态
	ORIGIN_CARD, //初始手牌状态
	GAMING, //游戏中
}