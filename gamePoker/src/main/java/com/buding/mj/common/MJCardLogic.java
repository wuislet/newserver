package com.buding.mj.common;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import packet.mj.MJ.GameOperHandCardSyn;
import packet.mj.MJ.GameOperPlayerActionNotify;
import packet.mj.MJ.GameOperPlayerActionSyn;
import packet.mj.MJ.GameOperPlayerHuSyn;
import packet.mj.MJ.GameOperStartSyn;
import packet.mj.MJBase.GameOperType;
import packet.mj.MJBase.GameOperation;
import com.buding.api.context.PlayFinalResult;
import com.buding.api.context.PlayHandResult;
import com.buding.api.desk.MJDesk;
import com.buding.api.player.PlayerInfo;
import com.buding.card.ICardLogic;
import com.buding.game.CardChangeReason;
import com.buding.game.GameCardDealer;
import com.buding.game.GameConstants;
import com.buding.game.GameData;
import com.buding.game.GamePacket.MyGame_Player_Ting_Cards;
import com.buding.game.GameRecorder;
import com.buding.mj.common.CardCombo;
import com.buding.mj.constants.MJConstants;
import com.buding.mj.helper.MJHelper;
import com.buding.mj.helper.PokerPushHelper;
import com.buding.mj.model.ActionWaitingModel;
import com.buding.mj.model.Card;
import com.buding.mj.model.ChuTingModel;
import com.buding.mj.model.GamingData;
import com.buding.mj.model.MjCheckContext;
import com.buding.mj.model.PlayerCard;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.protobuf.format.JsonFormat;

public class MJCardLogic implements ICardLogic<MJDesk> {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private MJProcessor mjProc = new MJProcessor();
	private GameData gameData; //TODO WXD 将函数传入的gameData跟私有变量gameData统一。
	private MJDesk desk;       //TODO WXD 将函数传入的desk跟私有变量desk统一。

	@Override
	public void init(GameData gameData, MJDesk desk) {
		this.gameData = gameData;
		this.desk = desk;
	}

	//流局
	private void liuju(GameData gameData, MJDesk desk) {
		//游戏结束
		gameData.setState(MJConstants.GAME_TABLE_STATE_SHOW_GAME_OVER_SCREEN);

		//记录
		gameData.recorder.recordPlayerAction(gameData.genSeq(), -1, MJConstants.MAHJONG_OPERTAION_HU, 0, 0, "流局", 1);

		// 结算番型和金币
		List<PlayerInfo> plist = desk.getPlayers();
		for (PlayerInfo px : plist) {
			gameData.mPlayerHandResult.playDetail[px.position].fanNum = 0;
			gameData.mPlayerHandResult.playDetail[px.position].fanType = MJConstants.MAHJONG_HU_CODE_LIUJU;
			gameData.mPlayerHandResult.playDetail[px.position].setScore(calScore(desk, 0));;
		}
		gameData.mGameHu.reset();
		//gameData.handEndTime = System.currentTimeMillis();
	}

	private void player_hu(GameData gameData, MJDesk desk, int fanType) {
		byte newCard = gameData.mGameHu.huCard;
		int position = gameData.mGameHu.position;
		PlayerInfo pl = desk.getDeskPlayer(position);
		int paoPosition = gameData.mGameHu.paoPosition;
		PlayerInfo pao_pl = desk.getDeskPlayer(paoPosition);
		System.out.println("    胡牌了   " + " newCard [" + newCard + "]" + " position [" + position + "]" + "Info [" + pl + "]" + " paoPosition [" + paoPosition + "]" + " paoInfo [" + pao_pl + "]");
		
		if(desk.canShouPao() && pao_pl != null && pl.playerId != pao_pl.playerId && gameData.mTingCards[position].tingCard) { //收炮 //TODO WXD 非泛用
			if(!gameData.mTingCards[paoPosition].tingCard){ //没有报听的人打出的牌才能触发收炮
				//游戏中的记录 //TODO wxd record 收炮
				gameData.recorder.recordPlayerAction(gameData.genSeq(), position, MJConstants.MAHJONG_OPERTAION_SHOUPAO, newCard, 0, "收炮", 1);
				
				// 结算番型和金币
				settle(gameData, desk, pl, pao_pl, fanType, newCard);
				
				//存储收炮数据
				int score = gameData.mPlayerHandResult.playDetail[pl.position].getScore(); //shouPaoData[]暂时用来存储每个人的收炮收支总结。本意是存储所有的单次收炮的数据。
				gameData.shouPaoData[position].addPaoScore(score);
				gameData.shouPaoData[paoPosition].addPaoScore(-score);
//				ShouPaoModel model = new ShouPaoModel();
//				model.setWinPosition(position);
//				model.setPaoPosition(paoPosition);
//				model.setScore(score);
//				model.setCard(newCard);
//				gameData.shouPaoData.add(model);
				
				//发送协议通知客户端
				GameOperPlayerHuSyn.Builder gb = GameOperPlayerHuSyn.newBuilder();
				gb.setCard(newCard);
				gb.setPosition(position);
				gb.setPaoPosition(paoPosition);
				gb.setSkipHuSettle(true);
				gb.setWinType(MJConstants.MAHJONG_HU_CODE_SHOUPAO);
				gb.setResultType(MJConstants.MAHJONG_HU_CODE_SHOUPAO);
				logger.info(" shouPaoMsg:"+JsonFormat.printToString(gb.build()));
				PokerPushHelper.pushPlayerHuMsg(desk, -100, gb, MJConstants.SEND_TYPE_ALL);
			}
			
			// 下个玩家进行摸牌动作
			player_mo(gameData, desk);
		} else {
			gameData.setState(MJConstants.GAME_TABLE_STATE_SHOW_GAME_OVER_SCREEN);
			//游戏中的记录
			gameData.recorder.recordPlayerAction(gameData.genSeq(), position, MJConstants.MAHJONG_OPERTAION_HU, newCard, 0, "胡牌", 1);
			
			// 结算番型和金币
			settle(gameData, desk, pl, pao_pl, fanType, newCard);
	
			//gameData.handEndTime = System.currentTimeMillis();
		}
	}
	
	@Override
	public int CalHuType(GameData gameData, MJDesk desk, PlayerInfo winner, byte newCard)
	{
		int position = winner.getTablePos();
		List<Byte> handCards = new ArrayList<Byte>();
		handCards.addAll(gameData.getCardsInHand(position));
		MJHelper.add2SortedList(newCard, handCards);
		
		List<Integer> downCards = gameData.getCardsDown(position); //门前牌
		
		int hutype = 0;
		
		//根据时机  //可能交由外部设置 
		//public static final int MAHJONG_HU_CODE_TIAN_HU=0x0040;//天胡  //TODO wxd hutype
		//public static final int MAHJONG_HU_CODE_GANG_HUA=0x0200;//杠花
//		if(gameData.isInFinalStage()){
//			hutype |= MJConstants.MAHJONG_HU_CODE_HAI_DI_LAO;
//		}
		
		//根据胡张
		if(mjProc.isJiaHu(handCards, newCard)) {
			hutype |= MJConstants.MAHJONG_HU_CODE_JIA_HU;
		}
		if(mjProc.isDanDiao(handCards, newCard)) {
			hutype |= MJConstants.MAHJONG_HU_CODE_DAN_DIAO;
		}
		if(mjProc.isJiaBian(handCards, newCard)) {
			hutype |= MJConstants.MAHJONG_HU_CODE_JIA_BIAN;
		}
		
		//根据牌型
//		if(mjProc.isMenQing(gameData.getCardsDown(position))){
//			hutype |= MJConstants.MAHJONG_HU_CODE_MEN_QING;
//		}
		if(mjProc.isQiXiaoDui(handCards, gameData.guiCards.get(0))){
			hutype |= MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI;
			hutype -= MJConstants.MAHJONG_HU_CODE_MEN_QING; //七小对不算门清
		}
//		if(mjProc.has1Color(handCards, downCards) && !mjProc.hasZi(handCards, downCards)) {
//			hutype |= MJConstants.MAHJONG_HU_CODE_QING_YI_SE;
//		}
//		if(mjProc.hasOne2Nine(handCards, downCards) && (hutype & MJConstants.MAHJONG_HU_CODE_QING_YI_SE) != 0) {
//			hutype |= MJConstants.MAHJONG_HU_CODE_QING_LONG;
//			hutype -= MJConstants.MAHJONG_HU_CODE_QING_YI_SE; //清龙不算清一色
//		}
//		if(!mjProc.hasShun(handCards, downCards)) {
//			hutype |= MJConstants.MAHJONG_HU_CODE_DUI_DUI_HU;
//		}
		
		//特殊规则
		if(desk.canShuaiJiuYao() && winner.shuaiCnt == 9) {
			hutype |= MJConstants.MAHJONG_HU_CODE_SHUAI_JIU_ZHANG;
		}
		return hutype;
	}
	
	@Override
	public int CalFanNum(int fanType)
	{
		int fanNum = 1;// 基础1翻
		if(desk.canShuaiJiuYao()){ //甩九幺玩法算分。
			if ((fanType & MJConstants.MAHJONG_HU_CODE_SHUAI_JIU_ZHANG) != 0
					|| (fanType & MJConstants.MAHJONG_HU_CODE_QI_XIAO_DUI) != 0) {
				fanNum *= 2;
			}
			if ((fanType & MJConstants.MAHJONG_HU_CODE_DAN_DIAO) != 0
					|| (fanType & MJConstants.MAHJONG_HU_CODE_JIA_HU) != 0
					|| (fanType & MJConstants.MAHJONG_HU_CODE_JIA_BIAN) != 0) {
				fanNum *= 2;
			}
			if ((fanType & MJConstants.MAHJONG_HU_CODE_ZI_MO) != 0) {
				fanNum *= 2;
			}
		} else { //普通规则
			fanNum = MJHelper.calNormalFanNum(fanType, fanNum);
		}
		return Math.min(fanNum, 8);
	}

	@Override
	public void gameTick(GameData gameData, MJDesk desk) {
		long ctt = System.currentTimeMillis();
		PlayerInfo currentPl = null;
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null) { // 如果有等待操作，就让对方操作
			currentPl = desk.getDeskPlayer(waiting.playerTableIndex);
		} else {
			currentPl = desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());
		}
		if (currentPl == null) {
			// TODO 设为非法状态，系统会自动清理
			// mGameData.setState(MJConstants.TABLE_STATE_INVALID);
			logger.error(">>>>>>>>>desk is invalid currentPl not found, deskId={};waitingPos={};curOperPos={};", waiting == null ? -1 : waiting.playerTableIndex,
					gameData.getCurrentOpertaionPlayerIndex());
			desk.setDeskInValid();
			return;
		}

		int substate = gameData.getPlaySubstate(); // 获取玩家的子原因状态

		if (substate == MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHI_PENG_ANIMATION || substate == MJConstants.GAME_TABLE_SUB_STATE_PLAYING_TING_ANIMATION) {
			if (ctt - gameData.getWaitingStartTime() > gameData.mGameParam.chiPengGangPlayMills)// 动画播完
			{
				// 清空子原因状态
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);
				// 通知玩家出牌
				this.player_chu_notify(gameData, desk);
			}
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_PLAYING_GANG_ANIMATION) {
			if (ctt - gameData.getWaitingStartTime() > gameData.mGameParam.chiPengGangPlayMills)// 动画播完
			{
				// 清空子原因状态
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);
				// 通知玩家出牌
				this.gang_mo(gameData, desk);
			}
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHU_ANIMATION) {
			if (ctt - gameData.getWaitingStartTime() > gameData.mGameParam.chuPlayMills)// 等待动画播完的时间
			{
				// 清空子原因状态
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);// 玩家吃碰之类的操作，服务器等客户端播个动画
				// 找找下个玩家
				this.next_player_operation_notify(gameData, desk);
			}
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_IDLE) {
			// 普通房间，如果玩家时托管状态，则直接帮他操作，否则等他超时
			boolean isAutoOper = 1 == gameData.mPlayerAction[currentPl.position].autoOperation;
			int playerActionTimeOut = gameData.mGameParam.operTimeOutSeconds; // 小于0则不允许超时自动出牌
			boolean isOperTimeout = playerActionTimeOut > 0 && (ctt - gameData.mPlayerAction[currentPl.position].opStartTime) > (playerActionTimeOut * 1000);
			boolean isNeedTrustGame = isOperTimeout && gameData.mGameParam.autoOperWhenTimeout;
			boolean isActionTime = (ctt - gameData.mPlayerAction[currentPl.position].opStartTime) > gameData.mGameParam.thinkMills4AutoOper;
			if (isNeedTrustGame || (isAutoOper && isActionTime)) { // 玩家超时或处于托管状态
				this.autoPlay(gameData, desk, currentPl, waiting);
				if (gameData.mPlayerAction[currentPl.position].autoOperation == 0) {
					gameData.mPlayerAction[currentPl.position].autoOperation = 1;
					desk.onPlayerHangup(currentPl.position);
				}
			}
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_SHOW_INIT_CARDS) {
			if (ctt - gameData.showInitCardTime > gameData.mGameParam.sendCardPlayMills) { // 3秒发牌动画
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);
				player_mo(gameData, desk);
			}
		}
		return;
	}

	@Override
	public boolean isFinishQuan(GameData gameData, MJDesk gt) {
		// 流局不换庄
		if (gameData.mGameHu.position == -1) {
			return false;
		}
		// 庄家赢不换庄
		if (gameData.mGameHu.position == gameData.mPublic.mbankerPos) {
			return false;
		}
		// 庄家是最后一个人(x风北局)，并且庄家输了，则一圈打完
		List<PlayerInfo> list = gt.loopGetPlayer(0, GameConstants.MyGame_Max_Players_Count, 0);
		boolean isNorthPlayerAsBanker = gameData.mPublic.mbankerPos == list.get(list.size() - 1).position;
		if (isNorthPlayerAsBanker && gameData.mPublic.mbankerPos != gameData.mGameHu.position) {
			return true;
		}
		return false;
	}

	@Override
	public void selectBanker(GameData data, MJDesk desk) {
		//没有庄家
		if (data.mPublic.mbankerPos == -1) {
			PlayerInfo playerInfo = desk.getDeskPlayer(0);
			data.mPublic.mbankerPos = playerInfo.position;
			data.mPublic.mBankerUserId = playerInfo.playerId;
			return;
		}
		
		if(true) {//金昌系列的麻将都按这个换庄规则。//TODO wxd 非泛用，需特殊化。
			int banker = data.mGameHu.position;
			if(banker != -1) { //谁胡谁庄。
				PlayerInfo playerInfo = desk.getDeskPlayer(banker);
				if (playerInfo == null) {
					logger.error("act=selectBanker;error=playerMiss;pos={};deskId={};", banker, desk.getDeskID());
					return;
				}
				data.mPublic.mbankerPos = banker;
				data.mPublic.mBankerUserId = playerInfo.playerId;
			}
			//没人胡不变庄
			return;
		}
		
		// 看看胡牌玩家是否就是庄家（连胡或者流局不换庄）
//		if ((data.mGameHu.position != -1) && (data.mPublic.mbankerPos != data.mGameHu.position)) {
//			int dealerPos = data.mPublic.mbankerPos + 1;
//			if (isFinishQuan(data, desk)) {
//				data.quanNum++;
//				dealerPos = 0;
//			}
//			PlayerInfo playerInfo = desk.getDeskPlayer(dealerPos);
//			if (playerInfo == null) {
//				logger.error("act=selectBanker;error=playerMiss;pos={};deskId={};", dealerPos, desk.getDeskID());
//				return;
//			}
//			data.mPublic.mbankerPos = dealerPos;
//			data.mPublic.mBankerUserId = playerInfo.playerId;
//		}
	}
	
	@Override
	public void setGuiCards(GameData gameData, MJDesk desk) {
		int guiCard = -1;
		if(desk.getGui() > 0) { //由desk里来决定鬼牌的点数。
			guiCard = desk.getGui();
		} else if(desk.getGui() == 0) { //由服务器算出鬼牌点数。
			guiCard = gameData.mDeskCard.cards.get(gameData.mDeskCard.cards.size() - 1);
		} else { //没有鬼牌。
			guiCard = -1;
		}
		gameData.guiCards.add(guiCard);
	}

	@Override
	public void sendCards(GameData gameData, MJDesk desk) {
		int totalCardNumber = 0;
		boolean loadReplay = false;
		String replayData = desk.getReplyData();
		if (StringUtils.isNotBlank(replayData)) {
			GameRecorder recorder = new Gson().fromJson(replayData, GameRecorder.class);
			if (recorder.playerInitCards.size() == desk.getPlayerCount()) { // 人数一致
				for (PlayerInfo pl : gameData.mPlayers) {
					if (pl == null)
						continue;
					List<Byte> cl = gameData.getCardsInHand(pl.position);
					cl.clear();
					List<Byte> src = new ArrayList<Byte>();
					src.addAll(MJHelper.getCardCodeList(recorder.playerInitCards.get(pl.position)));
					Collections.sort(src);
					cl.addAll(src);
					totalCardNumber += cl.size();
				}
				gameData.mPublic.mBaoCard = recorder.baoCard;
				gameData.mDeskCard.cards.clear();
				gameData.mDeskCard.cards.addAll(recorder.getInitCardCodeList());
				loadReplay = true;
				totalCardNumber += gameData.mDeskCard.cards.size();
			}
		}

		if (!loadReplay) {
			totalCardNumber += gameData.mDeskCard.cards.size();
			for (PlayerInfo pl : gameData.mPlayers) {
				if (pl == null)
					continue;
				List<Byte> cl = gameData.getCardsInHand(pl.position);
				cl.clear();
				List<Byte> src = new ArrayList<Byte>();

				List<Integer> initCards = desk.getDebugData(pl.position);
				for (int card : initCards) {
					Byte c = (byte) (card & 0xff);
					boolean ok = gameData.mDeskCard.cards.remove(c);
					if (ok) {
						src.add(c);
					}
					if (src.size() >= 13) {
						break;
					}
				}

				for (int j = src.size(); j < 13; j++) {
					Byte b = gameData.popCard();
					src.add(b);
				}
				// 排个序
				Collections.sort(src);
				cl.addAll(src);
			}
		}

		gameData.dice1 = (int) (System.nanoTime() % 6) + 1;
		gameData.dice2 = (int) (System.nanoTime() % 6) + 1;
		gameData.gameSeq = (int) (System.nanoTime() % 10000);
		setGuiCards(gameData, desk);
		
		for (PlayerInfo pl : gameData.mPlayers) {
			if (pl == null)
				continue;
			List<Byte> cl = gameData.getCardsInHand(pl.position);
			gameData.recorder.recordPlayerCard(pl.position, cl);
		}
		gameData.recorder.recordGameStart(gameData.mPlayers, gameData.mPublic.mBaoCard, gameData.mDeskCard.cards);
		
		// 把牌下发给客户端
		GameOperStartSyn.Builder msg = GameOperStartSyn.newBuilder();
		msg.setQuanNum(this.desk.getPlayerCount() == 2 ? gameData.handNum : gameData.quanNum);// 当前圈数(2人麻将显示局数)
		msg.setBankerPos(gameData.mPublic.mbankerPos);
		msg.setServiceGold((int) desk.getFee());// 本局服务费
		msg.setBankerContinue(gameData.mPublic.isContinueBanker); // 1:连庄，0：不是连庄
		msg.setDice1(gameData.dice1);
		msg.setDice2(gameData.dice2);
		msg.setSeq(gameData.gameSeq);
		msg.setCardLeft(totalCardNumber);
		msg.addAllGuiCards(gameData.guiCards);
		// msg.setTotalQuan(gameData.mGameParam.totalQuan);

		gameData.recorder.seq = msg.getSeq(); // 记录序列号

		for (PlayerInfo pl : gameData.mPlayers) {
			if (pl == null)
				continue;
			//
			List<Byte> cl = gameData.getCardsInHand(pl.position);
			logger.info("act=initcards;position={};cards={};", pl.position, new Gson().toJson(cl));
		}

		for (PlayerInfo pl : gameData.mPlayers) {
			if (pl == null)
				continue;
			msg.clearPlayerHandCards();
			for (PlayerInfo p : (List<PlayerInfo>) desk.getPlayers()) {
				boolean showHandCardVal = p.position == pl.position;
				GameOperHandCardSyn.Builder handCardBuilder = GameOperHandCardSyn.newBuilder();
				// 发给玩家的牌
				for (int card : gameData.getCardsInHand(pl.position)) {
					handCardBuilder.addHandCards(showHandCardVal ? card : -1);
				}
				handCardBuilder.setPosition(p.position);// 玩家的桌子位置
				msg.addPlayerHandCards(handCardBuilder);
			}

			GameOperation.Builder gb = GameOperation.newBuilder();
			gb.setOperType(GameOperType.GameOperStartSyn);
			gb.setContent(msg.build().toByteString());

			desk.sendMsg2Player(pl.position, gb.build().toByteArray());
		}
		
		gameData.setCurrentOpertaionPlayerIndex(gameData.mPublic.mbankerPos);
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_INIT_CARDS);
		gameData.showInitCardTime = System.currentTimeMillis();

		logger.info("act=onSendCard;seq={};players={}", msg.getSeq(), new Gson().toJson(gameData.mPlayers));
	}
	
	private int calScore(MJDesk desk, int fan) {
		return fan * desk.getBasePoint();
	}

	// 计算金币输赢
	private void settle(GameData gameData, MJDesk desk, PlayerInfo winner, PlayerInfo pao_pl, int fanType, byte newCard) {
		// 不能自己点自己
		Assert.isTrue(pao_pl == null || pao_pl.playerId != winner.playerId);
		
		fanType |= (pao_pl == null)?MJConstants.MAHJONG_HU_CODE_ZI_MO:0;
		fanType |= CalHuType(gameData, desk, winner, newCard);
		
		int dealer_pos = gameData.mPublic.mbankerPos; // 庄家位置
		// 先所有人计算下输赢，然后再看看是否放炮包三家
		List<PlayerInfo> plist = desk.getPlayers();

		for (PlayerInfo px : plist) {
			int selfFan = fanType;
			if (gameData.mTingCards[px.position].tingCard) {
				selfFan |= MJConstants.MAHJONG_HU_CODE_TING;
			}
			
			// 赢家不计算，他赢的等于其他输的人之和
			if (px.getPlayerID() == winner.getPlayerID()) {
				selfFan = fanType | MJConstants.MAHJONG_HU_CODE_WIN;
				if (px.position == dealer_pos)// 是庄家
				{
					logger.info("{} is banker;", px.position);
					gameData.mPlayerHandResult.playDetail[px.position].fanType = selfFan | MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA;
				} else {
					gameData.mPlayerHandResult.playDetail[px.position].fanType = selfFan;
				}
				gameData.mPlayerHandResult.playDetail[px.position].result = PlayHandResult.GAME_RESULT_WIN;
			} else { //输家
				selfFan = fanType | MJConstants.MAHJONG_HU_CODE_LOSE;
				if (px.position == dealer_pos) { // 我是庄家
					logger.info("{} is banker2;", px.position);
					selfFan |= MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA;
				} else if (winner.position == dealer_pos)// 对手是庄家
				{
					selfFan |= MJConstants.MAHJONG_HU_CODE_TARGET_ZHUANG_JIA;
				}
				
				int my_fan = CalFanNum(selfFan); //个人的翻计算。
				gameData.mPlayerHandResult.playDetail[px.position].fanType = selfFan;
				gameData.mPlayerHandResult.playDetail[px.position].fanNum = my_fan;
				gameData.mPlayerHandResult.playDetail[px.position].setScore(calScore(desk, my_fan));
			}
		}
		
		// 最后输赢处理
		int totalFan = 0;
		for (int i = 0; i < plist.size(); i++) {
			PlayerInfo px = plist.get(i);
			// 赢家不计算，他赢的等于其他输的人之和
			if (px.getPlayerID() == winner.getPlayerID())
				continue;
			totalFan += gameData.mPlayerHandResult.playDetail[px.position].fanNum;
			if (pao_pl != null && px.getPlayerID() != pao_pl.getPlayerID())// 有人点炮，则清空非点炮玩家的负分。
			{
				gameData.mPlayerHandResult.playDetail[px.position].fanNum = 0;
				gameData.mPlayerHandResult.playDetail[px.position].setScore(calScore(desk, 0));
			}
		}
		if (pao_pl != null) {//点炮者承担所有负分。
			gameData.mPlayerHandResult.playDetail[pao_pl.position].fanNum = totalFan;
			gameData.mPlayerHandResult.playDetail[pao_pl.position].setScore(calScore(desk, totalFan));
		}
		gameData.mPlayerHandResult.playDetail[winner.position].fanNum = totalFan;
		gameData.mPlayerHandResult.playDetail[winner.position].setScore(calScore(desk, totalFan));

		for (PlayerInfo p : plist) {
			int position = p.getTablePos();
			PlayFinalResult finalRes = gameData.mPlayerFinalResult.playDetail[position];
			PlayHandResult handRes = gameData.mPlayerHandResult.playDetail[position]; //引用变量，下面的设置操作会修改引用的对象。
			//如果玩家输了，将score和fanNum改为负数
			if ((handRes.fanType & MJConstants.MAHJONG_HU_CODE_LOSE) != 0) {
				handRes.result = PlayHandResult.GAME_RESULT_LOSE;
				handRes.fanNum = -handRes.fanNum;
				handRes.setScore(calScore(desk, handRes.fanNum));
			}
			//将每局结果记入到总结算中
			gameData.mergeHandResult(finalRes, handRes);
			//累加计算点炮总数
			if((pao_pl != null) && (p.playerId == pao_pl.playerId)) {
				finalRes.paoCount++;
			}
			
			String fanDesc = MJHelper.getFanDesc(handRes.fanType);
			logger.info("[" + p.position + ":" + handRes.fanType + "]" + fanDesc);
			logger.info("finalRes:" + new GsonBuilder().setPrettyPrinting().create().toJson(finalRes));

			//记录玩家的手牌,方便日后查
			handRes.downcards = new Gson().toJson(gameData.getCardsDown(position));
			handRes.handcards = new Gson().toJson(gameData.getCardsInHand(position));
			if (handRes.result == PlayHandResult.GAME_RESULT_WIN) {
				handRes.fanDesc = MJHelper.getResultTypeDesc(gameData.mPlayerHandResult.playDetail[p.position].fanType);
			}
		}
	}
	
	/**
	 * 摸牌之后的检测，会自动推送出牌。
	 * @param gameData
	 * @param desk
	 */
	public void player_check_mo(GameData gameData, MJDesk desk) {
		//当前操作玩家
		int position = gameData.getCurrentOpertaionPlayerIndex();
		PlayerInfo plx = desk.getDeskPlayer(position);
		byte newCard = gameData.mPlayerAction[position].cardGrab;// 摸到的一张牌

		if (plx == null) {
			logger.error("act=checkMo;error=playerMiss;newCard={};position={};deskId={};", newCard, gameData.getCurrentOpertaionPlayerIndex(), desk.getDeskID());
			return;
		}

		// 是否可以自摸胡
		if ((newCard > 0) && checkPlayHu(gameData, desk, position, -1, newCard)) {
			ActionWaitingModel result = new ActionWaitingModel();
			result.playerTableIndex = plx.position;
			result.opertaion = MJConstants.MAHJONG_OPERTAION_HU;
			gameData.setWaitingPlayerOperate(result);
			notifyPlayerWaitingOperation(gameData, desk, plx, result);
			return;
		}

		// 如果已经听牌，摸起来不能胡的牌，就自动打
		if (gameData.mTingCards[position].tingCard) {
			// 模拟玩家进行出牌操作
			GameOperPlayerActionSyn.Builder gb = GameOperPlayerActionSyn.newBuilder();
			gb.setPosition(position);
			gb.addCardValue(newCard & 0xff);
			gb.setAction(MJConstants.MAHJONG_OPERTAION_CHU);
			player_op_chu(gameData, desk, gb, plx);
			PokerPushHelper.pushActorSyn(desk, 0, position, 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
			return;
		}
		player_chu_notify(gameData, desk);
	}

	/**
	 * 提醒玩家出牌
	 * 
	 * @param gameData
	 * @param desk
	 */
	@Override
	public void player_chu_notify(GameData gameData, MJDesk desk) {
		//当前操作玩家
		int position = gameData.getCurrentOpertaionPlayerIndex();
		PlayerInfo plx = desk.getDeskPlayer(position);
		byte newCard = gameData.mPlayerAction[position].cardGrab;// 摸到的一张牌

		if (plx == null) {
			logger.error("act=chuNotify;error=playerMiss;position={};deskId={};", gameData.getCurrentOpertaionPlayerIndex(), desk.getDeskID());
			return;
		}

		// 游戏服务器通知客户端，轮到玩家操作了
		ActionWaitingModel result2 = new ActionWaitingModel();
		result2.opertaion = MJConstants.MAHJONG_OPERTAION_CHU;
		result2.playerTableIndex = position;
		result2.newCard = newCard;

		// 是不是取消了听
		boolean canOperSelfTurn = (gameData.mOpCancel[position].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN) != MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN;
		// 没有取消过听跟杠。
		if (canOperSelfTurn) {
			if(mjProc.canTing(new MjCheckContext(gameData, desk, newCard, position))) {
				// 自己摸的牌导致了可听牌，可以选择听牌，也可以选择不听而出牌
				result2.opertaion |= (MJConstants.MAHJONG_OPERTAION_TING | MJConstants.MAHJONG_OPERTAION_CHU);
				result2.playerTableIndex = position;
				result2.tingList.addAll(gameData.mTingCards[position].chuAndTingMap.keySet());
				result2.chuAndTingModel = new ChuTingModel();
				result2.chuAndTingModel.chuAndTingMap = gameData.mTingCards[position].chuAndTingMap;
				gameData.setWaitingPlayerOperate(result2);
			}
			
			ActionWaitingModel gangResult = mjProc.check_an_gang(gameData, newCard, plx.getTablePos());
			if(gangResult != null && gangResult.opertaion > 0){
				result2.opertaion |= (MJConstants.MAHJONG_OPERTAION_AN_GANG | MJConstants.MAHJONG_OPERTAION_CHU);
				result2.gangList = gangResult.gangList;
				result2.playerTableIndex = position;
				gameData.setWaitingPlayerOperate(result2);
			}
			
			gangResult = mjProc.check_bu_gang(gameData, newCard, plx.getTablePos());
			if(gangResult != null && gangResult.opertaion > 0){
				result2.opertaion |= (MJConstants.MAHJONG_OPERTAION_BU_GANG | MJConstants.MAHJONG_OPERTAION_CHU);
				result2.gangList = gangResult.gangList;
				result2.playerTableIndex = position;
				gameData.setWaitingPlayerOperate(result2);
			}
		}
		// 同步当前哪个玩家正在操作
		PokerPushHelper.pushActorSyn(desk, -100, position, 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
		//提醒当前玩家进行操作
		notifyPlayerWaitingOperation(gameData, desk, plx, result2);
		//同步手牌
		PokerPushHelper.pushHandCardSyn(gameData, desk, plx);

//		if (isEnd(gameData))// 流局了 //TODO WXD test del
//		{
//			gameData.mActor.gameState = MJConstants.MJStateFinish;
//			gameData.handEndTime = System.currentTimeMillis();
//
//			// 设置胡牌玩家为-1
//			gameData.mGameHu.position = -1;
//		}
	}

	private void player_op_chi(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		if (msg.getCardValueCount() != 2) {
			gt.sendErrorMsg(pl.position, "吃牌错误,需要提供两张牌");
			logger.error("act=chi;stage=gaming;error=invalidCardCount;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}
		// 玩家吃牌
		int v1 = msg.getCardValue(0) & 0xff;
		int v2 = msg.getCardValue(1) & 0xff;
		//
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.getTablePos(), "吃牌错误,当前牌型不能吃牌");
			logger.error("act=chi;stage=gaming;error=noSuchOperation;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}

		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.getTablePos(), "吃牌错误,没轮到你吃牌");
			logger.error("act=chi;stage=gaming;error=invalidPosition;position={};deskId={};desc=吃牌错误,没轮到你吃牌;", pl.getTablePos(), gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		// 吃
		if (((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) != MJConstants.MAHJONG_OPERTAION_CHI)) {
			gt.sendErrorMsg(pl.getTablePos(), "吃牌错误,命令码错误");
			logger.error("act=chi;stage=gaming;error=invalidOperation;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前不能吃；
		}

		CardCombo combo = waiting.chi_check(v1, v2);
		if (combo == null) {
			gt.sendErrorMsg(pl.position, "吃牌错误,找不到可吃的组合");
			logger.error("act=chi;stage=gaming;error=comboMiss;position=" + pl.getTablePos() + ";deskId="+gt.getDeskID()+";detail=v1:" + v1 + ",v2:" + v2 + ";isAuto:" + gameData.mPlayerAction[pl.position].autoOperation);
			return;// 吃的牌不对；
		}

		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.getTablePos(), msg.getAction(), v1, v2, MJHelper.getActionName(msg.getAction()) + ":" + MJHelper.getSingleCardName(combo.card1) + "_"
				+ MJHelper.getSingleCardName(combo.card2) + "_" + MJHelper.getSingleCardName(combo.targetCard), 0);

		// 有可能玩家吃听，这时候要把当前操作的索引改成此玩家
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());
		//
		gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.CHI);
		gameData.removeCardInHand(v2, pl.getTablePos(), CardChangeReason.CHI);
		gameData.add_Down_cards((byte) v1);
		gameData.add_Down_cards((byte) v2);
		//

		gameData.addCardDown(combo.targetCard, v1, v2, 2, pl.getTablePos());
		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushHandCardSyn(gameData, gt, pl);

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);

		// 等待客户端播动画
		gameData.setWaitingStartTime(System.currentTimeMillis());
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHI_PENG_ANIMATION);

		//清空刚才被吃的牌
		gameData.setCurrentCard((byte) 0);
		gameData.setCardOpPlayerIndex(-1);
	}

	private void player_op_peng(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		int v1 = msg.getCardValue(0) & 0xff;

		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, msg.getAction(), v1, 0,
				MJHelper.getActionName(msg.getAction()) + ":" + MJHelper.getSingleCardName(v1) + "_" + MJHelper.getSingleCardName(v1) + "_" + MJHelper.getSingleCardName(v1), 0);

		// 玩家出牌
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.position, "碰牌错误,当前牌型不能碰牌");
			logger.error("act=peng;stage=gaming;error=noSuchOperation;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}

		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.position, "碰牌错误,没轮到你碰牌");
			logger.error("act=peng;stage=gaming;error=positionInvalid;position={};deskId={};desc=碰牌错误,没轮到你碰牌;", pl.getTablePos(), gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) != MJConstants.MAHJONG_OPERTAION_PENG) {
			logger.error("act=peng;stage=gaming;error=operationInvalid;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前不能碰；
		}

		if (waiting.peng_card_value != (byte)v1) {
			gt.sendErrorMsg(pl.position, "碰牌错误,提交碰牌参数错误");
			logger.error("act=peng;stage=gaming;error=pengCardInvalid;expect={};actual={};position={};deskId={};", waiting.peng_card_value, v1, pl.getTablePos(), gt.getDeskID());
			return;// 碰的牌不对；
		}

		// 这时候要把当前操作的索引改成此玩家，这样他出牌的时候，当前操作玩家的索引才是正确的
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());

		gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.PENG);
		gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.PENG);
		gameData.add_Down_cards((byte) v1);
		gameData.add_Down_cards((byte) v1);
		
		PokerPushHelper.pushActorSyn(gt, -100, pl.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
		
		gameData.addCardDown(waiting.peng_card_value & 0xff, 0, 0, 1, pl.getTablePos());

		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushHandCardSyn(gameData, gt, pl);

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);
		// 碰完了轮到他操作，进行打牌
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());

		// 等待客户端播动画
		gameData.setWaitingStartTime(System.currentTimeMillis());
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHI_PENG_ANIMATION);

		gameData.setCurrentCard((byte) 0);
		gameData.setCardOpPlayerIndex(-1);
	}
	
	private void player_op_gang(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		int v1 = msg.getCardValue(0) & 0xff;

		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, msg.getAction(), v1, 0,
				MJHelper.getActionName(msg.getAction()) + ":" + MJHelper.getSingleCardName(v1) + "_" + MJHelper.getSingleCardName(v1) + "_" + MJHelper.getSingleCardName(v1), 0);

		// 玩家出牌
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.position, "杠牌错误,当前牌型不能杠牌");
			logger.error("act=gang;stage=gaming;error=noSuchOperation;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}

		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.position, "杠牌错误,没轮到你杠牌");
			logger.error("act=gang;stage=gaming;error=positionInvalid;position={};deskId={};desc=杠牌错误,没轮到你杠牌;", pl.getTablePos(), gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_AN_GANG) != MJConstants.MAHJONG_OPERTAION_AN_GANG
				&& (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_BU_GANG) != MJConstants.MAHJONG_OPERTAION_BU_GANG
				&& (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != MJConstants.MAHJONG_OPERTAION_ZHI_GANG) {
			logger.error("act=gang;stage=gaming;error=operationInvalid;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前不能杠；
		}

		if (!waiting.gangList.contains((Byte)(byte)v1)) {
			gt.sendErrorMsg(pl.position, "杠牌错误,提交杠牌参数错误");
			logger.error("act=gang;stage=gaming;error=gangCardInvalid;expect={};actual={};position={};deskId={};", waiting.gangList, v1, pl.getTablePos(), gt.getDeskID());
			return;// 杠的牌不对；
		}

		// 这时候要把当前操作的索引改成此玩家，这样他出牌的时候，当前操作玩家的索引才是正确的
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());
		
		//暗杠逻辑
		if((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_AN_GANG) != 0){
			gameData.removeCardInHandAndNewCard(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.removeCardInHandAndNewCard(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.removeCardInHandAndNewCard(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.removeCardInHandAndNewCard(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.add_Down_cards((byte) v1);
			gameData.add_Down_cards((byte) v1);
			gameData.add_Down_cards((byte) v1);
			gameData.add_Down_cards((byte) v1);
			gameData.addCardDown(v1, 0, 0, 3, pl.position);
		}
		//补杠逻辑
		if((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_BU_GANG) != 0){
			if(!gameData.tryBuGang(v1, pl.getTablePos())) {
				gt.sendErrorMsg(pl.position, "补杠错误,补杠提交杠牌参数错误");
				logger.error("act=gang;stage=gaming;error=gangCardInvalid;expect={};actual={};position={};deskId={};", waiting.gangList, v1, pl.getTablePos(), gt.getDeskID());
				return;
			}
			gameData.removeCardInHandAndNewCard(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.add_Down_cards((byte) v1);
			//gameData.addCardDown(v1, 0, 0, 3, pl.getTablePos());
		}
		//直杠逻辑
		if((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != 0){
			gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.removeCardInHand(v1, pl.getTablePos(), CardChangeReason.GANG);
			gameData.add_Down_cards((byte) v1);
			gameData.add_Down_cards((byte) v1);
			gameData.add_Down_cards((byte) v1);
			gameData.addCardDown(v1, 0, 0, 3, pl.position);
		}
		
		PokerPushHelper.pushActorSyn(gt, -100, pl.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushHandCardSyn(gameData, gt, pl);

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);
		
		// 碰完了轮到他操作，进行打牌
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());

		// 等待客户端播动画
		gameData.setWaitingStartTime(System.currentTimeMillis());
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_GANG_ANIMATION);

		// 清空刚才被吃的牌
		gameData.setCurrentCard((byte) 0);
		gameData.setCardOpPlayerIndex(-1);
	}
	
	private void player_op_hu(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		//TODO WXD 协议正确性检测。
		player_hu(gameData, gt, 1);
	}

	private void player_cancel(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, MJConstants.MAHJONG_OPERTAION_CANCEL, 0, 0, "取消", 0);

		// 获得玩家取消操作的列表
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.position, "不存在可取消的操作");
			logger.error("act=cancel;stage=gaming;error=noSuchOperation;position={};deskId={};", pl.getTablePos(), gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}
		// 等待处理人的座位号与当前玩家号不一致时
		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.position, "未轮到你操作，不能取消");
			logger.error("act=cancel;stage=gaming;error=positionInvalid;position={};deskId={};desc=未轮到你操作，不能取消;", pl.getTablePos(), gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_TING) != 0
				||(waiting.opertaion & MJConstants.MAHJONG_OPERTAION_AN_GANG) != 0
				||(waiting.opertaion & MJConstants.MAHJONG_OPERTAION_BU_GANG) != 0) {
			gameData.mOpCancel[pl.position].cancelOp |= MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN;
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) != 0
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) != 0
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != 0) {
			gameData.mOpCancel[pl.position].cancelOp |= MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN;
			List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
			gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);  // 如果是取消吃碰 直杠，则下一个操作人应该是出牌人的下家
		}
		
		//取消的是别人打出来牌胡的情况。//一定要额外判断card > 0，不然天胡会错。天胡错误原因是CardOpPlayerIndex默认是0，进入这个判断变成1了。
		if(gameData.getCurrentCard() > 0 && (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_HU) != 0) {
			gameData.mOpCancel[pl.position].cancelOp |= MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN;
			List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
			gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);  //下一个操作人应该是出牌人的下家
		}

		// 当前打出来的牌
		byte cd = gameData.getCurrentCard();
		gameData.setWaitingPlayerOperate(null);

		if (cd == 0) {
			player_chu_notify(gameData, gt);
		} else {
			next_player_operation_notify(gameData, gt);
		}
	}

	private void notifyPlayerWaitingOperation(GameData gameData, MJDesk desk, PlayerInfo pl, ActionWaitingModel waiting) {
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_HU) != 0) {
			//如果已经听牌了，则截获胡牌协议的发送，直接完成胡牌。
			if (gameData.mTingCards[pl.position].tingCard) { //已经听牌了直接胡
				player_hu(gameData, desk, 1);
				return;
			}
		}

		gameData.mPlayerAction[pl.position].opStartTime = System.currentTimeMillis(); //重置操作计时
		
		GameOperPlayerActionNotify.Builder msg = GameOperPlayerActionNotify.newBuilder();
		msg.setPosition(pl.getTablePos());
		msg.setActions(waiting.opertaion);
		msg.setLastActionCard(gameData.getCurrentCard());
		if (gameData.getCurrentCard() <= 0) {
			msg.setLastActionPosition(gameData.getCurrentOpertaionPlayerIndex());
		} else {
			msg.setLastActionPosition(gameData.getCardOpPlayerIndex());
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) != 0) {
			for (byte a : waiting.tingList) {
				msg.addTingList(a); // 告诉客户端，你只能出这些牌
			}
		}
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_AN_GANG) != 0) {
			for (byte a : waiting.gangList) {
				msg.addGangList(a); // 告诉客户端，你只能杠这些牌
			}
		}
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_BU_GANG) != 0) {
			for (byte a : waiting.gangList) {
				msg.addGangList(a); // 告诉客户端，你只能杠这些牌
			}
		}
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != 0) {
			for (byte a : waiting.gangList) {
				msg.addGangList(a); // 告诉客户端，你只能杠这些牌
			}
		}
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) != 0) {
			MJHelper.copyPengArg(waiting.peng_card_value, msg);
		}
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) != 0) {
			MJHelper.copyChiArg(waiting.chiCombos, msg);
		}

		msg.setNewCard(waiting.newCard);

		PokerPushHelper.pushActorSyn(desk, pl.position, pl.position, 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_SINGLE);
		PokerPushHelper.pushActionNofity(gameData, desk, pl.position, msg, MJConstants.SEND_TYPE_SINGLE);
	}

	private void player_op_ting(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, MJConstants.MAHJONG_OPERTAION_TING, 0, 0, "听牌", 0);
		// 玩家听牌
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.position, "当前牌型不能听牌");
			logger.error("act=playerting;error=waitingmiss;position={};deskId={};", pl.position, gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}
		//
		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.position, "未轮到你听牌");
			logger.error("act=playerting;error=notcurrentoperation;position={};deskId={};desc=未轮到你听牌;", pl.position, gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		waiting.newCard = 0; // 清掉刚摸到的牌

		// 发给其他玩家，让他们知道当前轮到谁操作
		PokerPushHelper.pushActorSyn(gt, -100, pl.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);

		gameData.addTingPl(pl);
		// 把听牌消息广播给桌子上所有成员
		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		waiting.opertaion = MJConstants.MAHJONG_OPERTAION_CHU; // 已听牌，下一步出牌
		notifyPlayerWaitingOperation(gameData, gt, pl, waiting); //TODO WXD 听牌尽量调用出牌提示函数player_chu_notify()。
	}

	private void player_op_chu(GameData gameData, MJDesk desk, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		byte card_value = (byte) (msg.getCardValue(0) & 0Xff);
		int idx = gameData.getCurrentOpertaionPlayerIndex(); // 当前打牌的玩家索引

		// 看看之前刚摸的牌有没有，摸的牌，先放cardgrab变量里面，等他出牌的时候再放进去，这样断线重连的时候，他摸的还是原来那张 //TODO WXD gang 杠会直接加，按照注释，可能重启会有问题。
		byte moCard = gameData.mPlayerAction[pl.position].cardGrab;
		if (moCard != 0) {
			gameData.addCardInHand(moCard, pl.position, CardChangeReason.MO);
			gameData.mPlayerAction[pl.position].cardGrab = 0;
		}

		if (idx != pl.getTablePos()) {
			// desk.sendErrorMsg(pl.position, "出牌错误,未轮到你操作");
			logger.error("act=playerChu;error=notcurrentoperation;expect={};actual={};position={};deskId={};desc=出牌错误,未轮到你操作;", idx, pl.position, pl.position, desk.getDeskID());
			return;
		}

		// 玩家出牌
		int pl_pos = msg.getPosition();
		byte card_v = gameData.findCardInHand(card_value, pl.position);

		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl_pos, msg.getAction(), card_v, 0,
				MJHelper.getActionName(msg.getAction()) + ":" + MJHelper.getSingleCardName(card_v) + ",带听:"
						+ ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_TING) != 0), 0);

		if (card_v != card_value || pl_pos != pl.getTablePos()) {
			desk.sendErrorMsg(pl.position, "出牌错误,不存在的牌");
			logger.error("act=playerChu;error=notcurrentoperation2;position={};deskId={};", pl.position, desk.getDeskID());
			return;
		}

		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null && (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) == 0) {
			desk.sendErrorMsg(pl.position, "出牌错误,当前不能进行出牌操作");
			logger.error("act=playerChu;error=notallowchu;position={};deskId={};", pl.position, desk.getDeskID());
			return;
		}

		MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.position];

		// 1把牌从玩家手里拿走
		gameData.removeCardInHand(card_value, pl.position, CardChangeReason.CHU);

		// 记录打下的牌
		gameData.add_Down_cards(card_value);

		// 2把牌放在桌子中间，如果没有吃碰胡之类，牌就放在这个玩家面前
		gameData.setCurrentCard(card_v);

		// 设置当前操作的玩家座位号
		gameData.setCardOpPlayerIndex(pl.getTablePos());

		// 推送给其他人
		PokerPushHelper.pushActionSyn(desk, 0, msg, MJConstants.SEND_TYPE_ALL);

		PokerPushHelper.pushHandCardSyn(gameData, desk, pl);

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);

		// 玩家第一次听牌，设置要听的牌
		if (tingModel.tingCard && tingModel.cards.size() == 0) {
			tingModel.cards = waiting.chuAndTingModel.chuAndTingMap.get(card_v);
		}
		reset4NextPlayerOperation(gameData, desk);
	}

	private void player_op_shuaijiuyao(GameData gameData, MJDesk desk, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		//TODO 验证九幺牌的点数。
		pl.shuaiCnt = msg.getCardValueList().size();
		for(int i = 0; i < pl.shuaiCnt; i++) {
			int cardpoint = msg.getCardValue(i);
			gameData.removeCardInHand(cardpoint, pl.getTablePos(), CardChangeReason.SHUAI_JIU_YAO);
		}
		PokerPushHelper.pushActionSyn(desk, -100, msg, MJConstants.SEND_TYPE_ALL);
	}

	private void reset4NextPlayerOperation(GameData gameData, MJDesk desk) {
		// 等待客户端播动画
		gameData.setWaitingStartTime(System.currentTimeMillis());
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHU_ANIMATION);// 设置玩家的子原因为客户端在播出牌动画

		// 顺序，轮到下一个玩家行动
		List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
		gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);
	}

	// 出牌结束，没有吃碰胡
	private void chu_end(GameData gameData, MJDesk desk) {
		byte card = gameData.getCurrentCard();
		if (card == 0)
			return;// 没有出的牌

		int idx = gameData.getCardOpPlayerIndex();
		PlayerInfo pl = desk.getDeskPlayer(idx);

		gameData.addCardBefore(card, pl.position);

		gameData.setCurrentCard((byte) 0);
		gameData.setCardOpPlayerIndex(-1);
	}

	/**
	 * 最后分张阶段判断玩家是否能自摸
	 * 
	 * @param gameData
	 **/
	private boolean game_only_zimo(GameData gameData, MJDesk desk) {
		for (PlayerInfo pl : gameData.mPlayers) {
			if (pl == null)
				continue;

			PlayerInfo plx = desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());

			if (plx == null) {
				continue;
			}

			// 给玩家摸一张
			byte newCard = gameData.popCard();
			gameData.mPlayerAction[plx.position].cardGrab = newCard;

			pushPlayerMoMsg(gameData, desk, plx, newCard);

			// 3看看自摸胡没
			if (checkPlayHu(gameData, desk, plx.position, -1, newCard)) {
				player_hu(gameData, desk, 1);
				return true;
			}

			// 顺序，轮到下一个玩家行动
			gameData.move2NextPlayer(desk);
		}

		// 如果分张阶段，没有人胡，那么流局了
		liuju(gameData, desk);

		return true;
	}

	public void gang_mo(GameData gameData, MJDesk desk) {
		PlayerInfo plx = desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());
		if (plx == null) {
			logger.error("act=mo;stage=gaming;error=noSuchPlayer;position={};deskId={};", gameData.getCurrentOpertaionPlayerIndex(), desk.getDeskID());
			return;
		}
		
		//加入上一次摸的牌。
		byte moCard = gameData.mPlayerAction[plx.getTablePos()].cardGrab;
		if (moCard != 0) {
			gameData.addCardInHand(moCard, plx.getTablePos(), CardChangeReason.MO);
			gameData.mPlayerAction[plx.getTablePos()].cardGrab = 0;
		}

		// 重置取消操作记录
		gameData.resetOpCancel();

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);

		if(gameData.getCardLeftNum() == 0) {
			liuju(gameData, desk);
			return;
		}

		if (gameData.getCardNumInHand(plx.getTablePos()) % 3 == 1) {
			// 给玩家摸一张
			byte b = gameData.popGangCard();
			gameData.mPlayerAction[plx.getTablePos()].cardGrab = b;
			gameData.recorder.recordPlayerAction(gameData.genSeq(), plx.getTablePos(), MJConstants.MAHJONG_OPERTAION_MO, b, 0, "摸牌:" + MJHelper.getSingleCardName(b), 1);
			pushPlayerMoMsg(gameData, desk, plx, b);

			player_check_mo(gameData, desk);
			return;
		} else {
			throw new RuntimeException("大件事了,杠摸错牌啦!!!!!!!!!!!!!!!!!;position="+plx.getTablePos()+";deskId="+desk.getDeskID()+";num="+gameData.getCardNumInHand(plx.getTablePos()));
		}
	}
	
	public void player_mo(GameData gameData, MJDesk desk) {
		PlayerInfo plx = desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());
		if (plx == null) {
			logger.error("act=mo;stage=gaming;error=noSuchPlayer;position={};deskId={};", gameData.getCurrentOpertaionPlayerIndex(), desk.getDeskID());
			return;
		}

		// 在玩家摸牌之前，把上家的打出来的牌，放到玩家面前（因为没有吃碰胡）
		chu_end(gameData, desk);

		// 重置取消操作记录
		gameData.resetOpCancel();

		// 服务器清除等待玩家操作的数据
		gameData.setWaitingPlayerOperate(null);

		if(gameData.getCardLeftNum() == 0) {
			liuju(gameData, desk);
			return;
		}

		if (gameData.getCardNumInHand(plx.position) % 3 == 1) {
			// 给玩家摸一张
			byte b = gameData.popCard();
			gameData.mPlayerAction[plx.position].cardGrab = b;
			gameData.recorder.recordPlayerAction(gameData.genSeq(), plx.position, MJConstants.MAHJONG_OPERTAION_MO, b, 0, "摸牌:" + MJHelper.getSingleCardName(b), 1);
			pushPlayerMoMsg(gameData, desk, plx, b);

			player_check_mo(gameData, desk);
			return;
		} else {
			throw new RuntimeException("大件事了,摸错牌啦!!!!!!!!!!!!!!!!!;position="+plx.position+";deskId="+desk.getDeskID()+";numinhand="+gameData.getCardNumInHand(plx.position));
		}
	}

	private void pushPlayerMoMsg(GameData gameData, MJDesk desk, PlayerInfo plx, byte b) {
		GameOperPlayerActionSyn.Builder moMsg = GameOperPlayerActionSyn.newBuilder();
		moMsg.setAction(MJConstants.MAHJONG_OPERTAION_MO);
		moMsg.setPosition(plx.position);
		moMsg.addCardValue(-1);
		PokerPushHelper.pushActionSyn(desk, plx.position, moMsg, MJConstants.SEND_TYPE_EXCEPT_ONE);
		moMsg.clearCardValue();
		moMsg.addCardValue(b);
		PokerPushHelper.pushActionSyn(desk, plx.position, moMsg, MJConstants.SEND_TYPE_SINGLE);
	}

	public ActionWaitingModel check_chi(GameData gameData, byte card, int position) {
		if (!MJHelper.isNormalCard(card)){
			return null;
		}
		ActionWaitingModel code = new ActionWaitingModel();
		code.playerTableIndex = position;

		List<Byte> cards = gameData.getCardsInHand(position);
		{
			// 3,4,5
			if (MJHelper.findInSortList(cards, (byte) (card + 1), (byte) (card + 2))) {
				code.chiCombos.add(new CardCombo((byte) (card + 1), (byte) (card + 2), card));
				code.opertaion = MJConstants.MAHJONG_OPERTAION_CHI;
			}

			// 2,3,4
			if (MJHelper.findInSortList(cards, (byte) (card - 1), (byte) (card + 1))) {
				code.chiCombos.add(new CardCombo((byte) (card - 1), (byte) (card + 1), (byte) (card)));
				code.opertaion = MJConstants.MAHJONG_OPERTAION_CHI;
			}

			// 1,2,3
			if (MJHelper.findInSortList(cards, (byte) (card - 2), (byte) (card - 1))) {
				code.chiCombos.add(new CardCombo((byte) (card - 2), (byte) (card - 1), (byte) (card)));
				code.opertaion = MJConstants.MAHJONG_OPERTAION_CHI;
			}
		}
		if (code.opertaion > 0) {
			return code;
		}
		return null;
	}

	//检测是不是要同时提示其他操作。
	private void checkAllAction(GameData gameData, byte card, ActionWaitingModel result, int nextPosition) {
		int position = result.playerTableIndex;
		logger.info("act=checkAllAction;stage=gaming;position={};action={};", position, result.opertaion);
		// 同时提示直杠
		ActionWaitingModel tmp = mjProc.check_zhi_gang(gameData, card, position);
		if (tmp != null && tmp.opertaion > 0) {
			result.gangList.addAll(tmp.gangList);
			result.opertaion |= MJConstants.MAHJONG_OPERTAION_ZHI_GANG;
		}
		
		// 同时提示碰
		tmp = mjProc.check_peng(gameData, card, position);
		if (tmp != null && tmp.opertaion > 0) {
			result.peng_card_value = tmp.peng_card_value;
			result.opertaion |= MJConstants.MAHJONG_OPERTAION_PENG;
		}
		
		// 同时提示吃
		if (desk.canChi() && position == nextPosition) {
			tmp = check_chi(gameData, card, position);
			if (tmp != null) {
				result.chiCombos = tmp.chiCombos;
				result.opertaion |= MJConstants.MAHJONG_OPERTAION_CHI;
			}
		}
	}
	
	/**
	 * 有人打了一张牌看下别的玩家有没有操作
	 * @param gameData
	 * @param desk
	 * @return
	 */
	public boolean next_player_operation_notify(GameData gameData, MJDesk desk) {
		byte card = gameData.getCurrentCard();// 当前打出的牌

		int opPosition = gameData.getCardOpPlayerIndex();

		// 顺序，轮到下一个玩家行动
		List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
		gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);

		// 获取当前的玩家
		PlayerInfo plx = desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());

		if (plx == null) {
			logger.error("act=nextNotify;stage=gaming;error=noSuchPlayer;position={};deskId={};", gameData.getCurrentOpertaionPlayerIndex(), desk.getDeskID());
			return false;
		}

		ActionWaitingModel result = null;

		do {// 按优先级
			// 1先看看有没有胡的玩家
			if(!desk.havetoZiMo()) { //只能自摸的情况下，不去循环找胡。
				result = loopCheckHu(gameData, desk, card, opPosition);
				System.out.println("   loop result  " + result);
				if (result != null) {
					checkAllAction(gameData, card, result, plx.getTablePos());
					break;
				}
			}

			// 2看看有没有杠的玩家
			result = loopCheckZhiGang(gameData, desk, card, opPosition);
			if (result != null) {
				checkAllAction(gameData, card, result, plx.getTablePos());
				break;
			}

			// 3看看有没有碰的玩家
			result = loopCheckPeng(gameData, desk, card, opPosition);
			if (result != null) {
				checkAllAction(gameData, card, result, plx.getTablePos());
				break;
			}

			// 4看看有没有吃的玩家
			boolean cancelChi = (gameData.mOpCancel[plx.position].cancelOp 
							& MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) != 0;
			if (desk.canChi() && gameData.mTingCards[plx.position].tingCard == false && !cancelChi) {
				result = check_chi(gameData, card, plx.getTablePos());
				if (result != null) {
					break;
				}
			}
		} while (false);

		if (result != null) {
			// 设置当前桌子的等待玩家操作，等玩家操作的时候，再核查一下
			gameData.setWaitingPlayerOperate(result);
			PlayerInfo pl = desk.getDeskPlayer(result.playerTableIndex);
			notifyPlayerWaitingOperation(gameData, desk, pl, result);
		} else {
			// 如果什么操作都没有，下个玩家进行摸牌动作
			player_mo(gameData, desk);
			return true;
		}
		return true;
	}
	
	private boolean checkPlayHu(GameData gameData, MJDesk desk, int position, int paoPosition, byte card){
		boolean flag = false;
		MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[position];
		if (tingModel.tingCard) {
			if((desk.getTingType() & 0x2) != 0) {
				flag = tingModel.cards.contains(card);
			}
		} else {
			if((desk.getTingType() & 0x1) != 0) {
				flag = mjProc.canHu(new MjCheckContext(gameData, desk, card, position));
			}
		}
		
		if(paoPosition != -1 && gameData.guiCards.contains((Integer)(int)card)) { //特殊判断：别人打出的万能牌不能点炮 //TODO wxd 想办法去掉特判？
			flag = false;
		}
		if(flag) {
			gameData.mGameHu.position = position;
			gameData.mGameHu.paoPosition = paoPosition;
			gameData.mGameHu.huCard = card;
		}
		
		return flag;
	}

	// 别人打的牌，自己能不能胡
	private ActionWaitingModel loopCheckHu(GameData gameData, MJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			if ((gameData.mOpCancel[pl.getTablePos()].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) != 0) {
				continue;
			}
			if(checkPlayHu(gameData, desk, pl.getTablePos(), cardOpPosition, card)) {
				ActionWaitingModel code = new ActionWaitingModel();
				code.playerTableIndex = pl.getTablePos();
				code.opertaion = MJConstants.MAHJONG_OPERTAION_HU;
				return code;
			}
		}
		return null;
	}

	private ActionWaitingModel loopCheckZhiGang(GameData gameData, MJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.getTablePos()];
			if (tingModel.tingCard) {
				continue;
			}
			if ((gameData.mOpCancel[pl.getTablePos()].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) != 0) {
				continue;
			}
			ActionWaitingModel code = mjProc.check_zhi_gang(gameData, card, pl.getTablePos());
			if (code != null && code.opertaion > 0) {
				return code;
			}
		}
		return null;
	}

	private ActionWaitingModel loopCheckPeng(GameData gameData, MJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.getTablePos()];
			if (tingModel.tingCard) {
				continue;
			}
			if ((gameData.mOpCancel[pl.getTablePos()].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) != 0) {
				continue;
			}
			ActionWaitingModel code = mjProc.check_peng(gameData, card, pl.getTablePos());
			if (code != null && code.opertaion > 0) {
				return code;
			}
		}
		return null;
	}

	@Override
	public void playerOperation(GameData gameData, MJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		if (msg == null || pl == null)
			return;
		
		logger.info("act=playerOperation;code={};position={};deskId={};", msg.getAction(), pl.position, gt.getDeskID());

		// 玩家出牌
		if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CHU) != 0) {
			player_op_chu(gameData, gt, msg, pl);
		}

		// 玩家吃牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CHI) != 0) {
			player_op_chi(gameData, gt, msg, pl);
		}

		// 玩家碰牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_PENG) != 0) {
			player_op_peng(gameData, gt, msg, pl);
		}

		// 玩家暗杠牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_AN_GANG) != 0) {
			player_op_gang(gameData, gt, msg, pl);
		}

		// 玩家补杠牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_BU_GANG) != 0) {
			player_op_gang(gameData, gt, msg, pl);
		}

		// 玩家直杠牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != 0) {
			player_op_gang(gameData, gt, msg, pl);
		}

		// 玩家胡牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_HU) != 0) {
			player_op_hu(gameData, gt, msg, pl);
		}

		// 玩家取消操作牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CANCEL) != 0) {
			player_cancel(gameData, gt, msg, pl);
		}

		// 玩家听牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_TING) != 0) {
			player_op_ting(gameData, gt, msg, pl);
		}
		
		// 玩家甩九幺
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_SHUAIJIUYAO) != 0) {
			player_op_shuaijiuyao(gameData, gt, msg, pl);
		}

		else
			throw new RuntimeException("UnKnowOperation;");
	}

	@Override
	public void re_notify_current_operation_player(GameData gameData, MJDesk desk, int position) {
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null) {
			PlayerInfo plx = desk.getDeskPlayer(waiting.playerTableIndex);
			if (plx != null && plx.position == position) {
				notifyPlayerWaitingOperation(gameData, desk, plx, waiting);
			}
		} else if (gameData.getCurrentOpertaionPlayerIndex() == position) {
			this.player_chu_notify(gameData, desk);
		}

		if (gameData.isInFinalStage())// 流局了
		{
			liuju(gameData, desk);
		}
	}

	public void autoPlay(GameData gt, MJDesk desk, PlayerInfo pl, ActionWaitingModel waiting) {
		GameOperPlayerActionSyn.Builder msg = GameOperPlayerActionSyn.newBuilder();
		msg.setPosition(pl.position);
		// 自动吃碰听
		if (waiting != null) {
			// 杠
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_AN_GANG) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_AN_GANG);
				msg.addCardValue(waiting.gangList.get(0) & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_BU_GANG) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_BU_GANG);
				msg.addCardValue(waiting.gangList.get(0) & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZHI_GANG) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_ZHI_GANG);
				msg.addCardValue(waiting.gangList.get(0) & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}
			
			// 碰
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_PENG);
				msg.addCardValue(waiting.peng_card_value);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 吃
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_CHI);
				CardCombo combo = waiting.chiCombos.get(0);
				msg.addCardValue(combo.card1 & 0xFF);
				msg.addCardValue(combo.card2 & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 听
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_TING) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_TING); //TODO WXD gang list 为什么不用设牌？
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 出
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) != 0) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_CHU);
				if (waiting.tingList.size() > 0) { //TODO WXD gang list ？？
					msg.addCardValue(waiting.tingList.get(0));
				} else if (gt.mPlayerAction[pl.position].cardGrab != 0) {
					msg.addCardValue(gt.mPlayerAction[pl.position].cardGrab & 0xff);
				} else {
					msg.addCardValue(gt.getCard(gt.getCardNumInHand(pl.position) - 1, pl.position) & 0xff);
				}
				this.playerOperation(gt, desk, msg, pl);
				return;
			}
		}

		// 没有waiting,则出
		msg.setAction(MJConstants.MAHJONG_OPERTAION_CHU);
		if (gt.mPlayerAction[pl.position].cardGrab != 0) {
			msg.addCardValue(gt.mPlayerAction[pl.position].cardGrab & 0xff);
		} else {
			msg.addCardValue(gt.getCard(gt.getCardNumInHand(pl.position) - 1, pl.position) & 0xff);
		}
		this.playerOperation(gt, desk, msg, pl);
	}

	@Override
	public void handleSetGamingData(GameCardDealer mCardDealer, GameData gameData, MJDesk desk, String json) {
		logger.info(json);
		GamingData gd = new Gson().fromJson(json, GamingData.class);
		gameData.Reset();
		mCardDealer.dealCard();

		gameData.setCurrentOpertaionPlayerIndex(gd.curOperPosition);
		gameData.mPublic.mbankerPos = 0;
		gameData.mPublic.mBaoCard = findCard(gameData, gd.baopai);
		gameData.mPublic.mBankerUserId = gameData.mPlayers[0].playerId;
		gameData.debugMode = gd.debugMode;
		if (gd.newCard != null) {
			gameData.mPlayerAction[gd.curOperPosition].cardGrab = findCard(gameData, gd.newCard);
		}
		setPlayerCard(gameData, 0, gd.player1);
		setPlayerCard(gameData, 1, gd.player2);

		if (gameData.mPlayers.length > 2 && gameData.mPlayers[2] != null) {
			setPlayerCard(gameData, 2, gd.player3);
		}

		if (gameData.mPlayers.length > 3 && gameData.mPlayers[3] != null) {
			setPlayerCard(gameData, 3, gd.player4);
		}

		List<Byte> list = new ArrayList<Byte>();
		if (gd.preSetRemainCard != null && gd.preSetRemainCard.isEmpty() == false) {			
			for (Card card : gd.preSetRemainCard) {
				byte code = findCard(gameData, card);
				list.add(code);
			}
		}
		
		fillOtherCard(gameData, 0);
		fillOtherCard(gameData, 1);

		if (gameData.mPlayers.length > 2 && gameData.mPlayers[2] != null) {
			fillOtherCard(gameData, 2);
		}

		if (gameData.mPlayers.length > 3 && gameData.mPlayers[3] != null) {
			fillOtherCard(gameData, 3);
		}
		if(list.isEmpty() == false) {
			gameData.mDeskCard.cards.addAll(0, list);
		}

		gameData.setState(MJConstants.GAME_TABLE_STATE_PLAYING);

		for (PlayerInfo player : gameData.mPlayers) {
			if (player != null) {
				this.repushGameData(gameData, desk, player.position);
			}
		}

		logger.info("推送完毕");
		logger.info("玩家1手牌:" + MJHelper.getSingleCardListName(gameData.getCardsInHand(0)));
		logger.info("玩家1吃碰杠:" + MJHelper.getCompositeCardListName(gameData.getCardsDown(0)));
		logger.info("玩家1打出的牌:" + MJHelper.getSingleCardListName(gameData.getCardsBefore(0)));

		logger.info("玩家2手牌:" + MJHelper.getSingleCardListName(gameData.getCardsInHand(1)));
		logger.info("玩家2吃碰杠:" + MJHelper.getCompositeCardListName(gameData.getCardsDown(1)));
		logger.info("玩家2打出的牌:" + MJHelper.getSingleCardListName(gameData.getCardsBefore(1)));

		logger.info("玩家3手牌:" + MJHelper.getSingleCardListName(gameData.getCardsInHand(2)));
		logger.info("玩家3吃碰杠:" + MJHelper.getCompositeCardListName(gameData.getCardsDown(2)));
		logger.info("玩家3打出的牌:" + MJHelper.getSingleCardListName(gameData.getCardsBefore(2)));

		logger.info("玩家4手牌:" + MJHelper.getSingleCardListName(gameData.getCardsInHand(3)));
		logger.info("玩家4吃碰杠:" + MJHelper.getCompositeCardListName(gameData.getCardsDown(3)));
		logger.info("玩家4打出的牌:" + MJHelper.getSingleCardListName(gameData.getCardsBefore(3)));
	}

	private void fillOtherCard(GameData gameData, int pos) {
		List<Byte> cardsInHand = gameData.getCardsInHand(pos);
		int cardsdownSize = gameData.getCardsDown(pos).size() * 3;
		while (cardsInHand.size() < 13 - cardsdownSize) {
			byte card = gameData.popCard();
			if (card == 0) {
				break;
			}
			gameData.addCardInHand(card, pos, CardChangeReason.MO);
		}
	}

	private byte findCard(GameData gameData, Card card) {
		Byte code = (byte) card.code;
		boolean ok = gameData.mDeskCard.cards.remove(code);
		if (ok) {
			return code;
		} else {
			throw new RuntimeException("找不到牌:" + card);
		}
	}

	private void setPlayerCard(GameData gameData, int pos, PlayerCard playerCard) {
		if (playerCard != null && playerCard.cardsInHand != null) {
			for (Card card : playerCard.cardsInHand) {
				byte code = findCard(gameData, card);
				gameData.addCardInHand(code, pos, CardChangeReason.MO);
				gameData.add_Down_cards(code);
			}
		}
		if (playerCard != null && playerCard.cardsDown != null) {
			for (int i = 0; i < playerCard.cardsDown.size(); i += 3) {
				byte c1 = findCard(gameData, playerCard.cardsDown.get(i));
				byte c2 = findCard(gameData, playerCard.cardsDown.get(i + 1));
				byte c3 = findCard(gameData, playerCard.cardsDown.get(i + 2));
				gameData.add_Down_cards(c1);
				gameData.add_Down_cards(c2);
				gameData.add_Down_cards(c3);
				if(c3 == MJConstants.MAHJONG_CODE_GANG_CARD) {
					gameData.addCardDown(c1, 0, 0, 3, pos);
				} else if (c1 == c2) {
					gameData.addCardDown(c1, 0, 0, 1, pos);
				} else {
					gameData.addCardDown(c1, c2, c3, 2, pos);
				}
			}
		}

		if (playerCard != null && playerCard.cardsBefore != null) {
			for (Card card : playerCard.cardsBefore) {
				byte code = findCard(gameData, card);
				gameData.add_Down_cards((byte) code);
				gameData.addCardBefore((byte) code, pos);
			}
		}
	}

	@Override
	public void repushGameData(GameData gameData, MJDesk desk, int position) {
		logger.info("act=repushGameData; position={};deskId={};", position, desk.getDeskID());
		// 把牌下发给客户端
		GameOperStartSyn.Builder msg = GameOperStartSyn.newBuilder();
		msg.setQuanNum(desk.getPlayerCount() == 2 ? gameData.handNum : gameData.quanNum);// 当前圈数
		msg.setBankerPos(gameData.mPublic.mbankerPos);
		msg.setServiceGold((int) desk.getFee());// 本局服务费
		msg.setBankerContinue(gameData.mPublic.isContinueBanker); // 1:连庄，0：不是连庄
		msg.setDice1(gameData.dice1);
		msg.setDice2(gameData.dice2);
		msg.setSeq(gameData.gameSeq);
		msg.setCardLeft(gameData.getCardLeftNum());
		msg.addAllGuiCards(gameData.guiCards);
		msg.setReconnect(true);

		gameData.recorder.seq = msg.getSeq(); // 记录序列号

		byte moCard = gameData.mPlayerAction[position].cardGrab;
		if (moCard > 0) { // 加到手中,防止前端丢牌
			gameData.addCardInHand(moCard, position, CardChangeReason.MO);
			gameData.mPlayerAction[position].cardGrab = 0;
		}

		PlayerInfo pl = desk.getDeskPlayer(position);
		for (PlayerInfo p : (List<PlayerInfo>) desk.getPlayers()) {
			boolean showHandCardVal = p.position == pl.position;
			GameOperHandCardSyn.Builder handCardBuilder = GameOperHandCardSyn.newBuilder();
			// 发给玩家的牌
			for (int card : gameData.getCardsInHand(p.position)) {
				handCardBuilder.addHandCards(showHandCardVal ? card : -1);
			}
			for (int card : gameData.getCardsBefore(p.position)) {
				handCardBuilder.addCardsBefore(card);
			}
			byte currentCard = gameData.getCurrentCard();
			if (currentCard > 0 && gameData.getCardOpPlayerIndex() == p.position) {
				handCardBuilder.addCardsBefore(currentCard);
			}
			for (int card : gameData.getCardsDown(p.position)) {
				handCardBuilder.addDownCards(card);
			}
			handCardBuilder.setPosition(p.position);// 玩家的桌子位置
			msg.addPlayerHandCards(handCardBuilder);
		}

		GameOperation.Builder gb = GameOperation.newBuilder();
		gb.setOperType(GameOperType.GameOperStartSyn);
		gb.setContent(msg.build().toByteString());

		desk.sendMsg2Player(pl.position, gb.build().toByteArray());

		// 发送当前操作人
		PokerPushHelper.pushActorSyn(desk, position, gameData.getCurrentOpertaionPlayerIndex(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_SINGLE);

		// 如果是当前人操作，重新通知他进行操作
		this.re_notify_current_operation_player(gameData, desk, position);
	}
}
