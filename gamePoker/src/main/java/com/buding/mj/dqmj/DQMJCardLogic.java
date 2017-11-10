package com.buding.mj.dqmj;

import java.util.*;
import com.buding.mj.common.BaseMJRule;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import packet.mj.MJ.GameOperBaoChangeSyn;
import packet.mj.MJ.GameOperHandCardSyn;
import packet.mj.MJ.GameOperPlayerActionNotify;
import packet.mj.MJ.GameOperPlayerActionSyn;
import packet.mj.MJ.GameOperPlayerHuSyn;
import packet.mj.MJ.GameOperStartSyn;
import packet.mj.MJBase.GameOperType;
import packet.mj.MJBase.GameOperation;
import com.buding.api.context.PlayFinalResult;
import com.buding.api.context.PlayHandResult;
import com.buding.api.desk.DQMJDesk;
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
import com.buding.mj.model.DQMjCheckContext;
import com.buding.mj.model.PlayerCard;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.protobuf.format.JsonFormat;

public class DQMJCardLogic implements ICardLogic<DQMJDesk> {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private DQMJProcessor dqmjProc = new DQMJProcessor();
	private GameData gameData;
	private DQMJDesk desk;

	@Override
	public void init(GameData gameData, DQMJDesk desk) {
		this.gameData = gameData;
		this.desk = desk;
	}

	// 核心思想就是3张牌不能被占用(只有支对的情况才算占用)
	public boolean isGuaDaFeng(GameData gameData, int position, byte card) {
		// 已经是碰牌了
		if (gameData.isPengCard(card, position)) {
			return true;
		}

		// 没有3张
		if (gameData.getXCardNumInHand(card, position) < 3) {
			return false;
		}
		// 支对了，算做占用
		if (gameData.mTingCards[position].zhiduiTing == card) {
			return false;
		}
		return true;
	}

	// 检测是否满足下雨规则(手里有3个牌，或差牌3个，和宝一样，算宝中宝)
	public boolean tryXiaYu(GameData gameData, int position, byte c, PlayerInfo player) {
		byte baoCard = gameData.mPublic.mBaoCard;
		List<Byte> cards = new ArrayList<Byte>();
		cards.addAll(gameData.mPlayerCards[position].cardsInHand);
		if (c != 0) {
			cards.addAll(cards);
		}
		byte zhiduiTing = gameData.mTingCards[position].zhiduiTing;
		List<Byte> baocardList = new ArrayList<Byte>();
		baocardList.add(baoCard);
		if (desk.canGunBao()) {
			baocardList.add(desk.getGunBaoCard(baoCard));
		}
		return false;
	}

	/**
	 * 玩家上听后每次摸牌时，根据本方法判断是否自摸
	 */
	public boolean checkPlayerHuAfterMo(GameData gameData, DQMJDesk desk, PlayerInfo player) {
		int position = player.position;
		//未听牌直接返回
		if (!gameData.mTingCards[position].tingCard) {
			return false;
		}

		byte card = gameData.mPlayerAction[position].cardGrab;

		// 如果是自摸
		if (gameData.mTingCards[position].cards.contains(card)) {
			List<Byte> cards = gameData.mPlayerCards[position].cardsInHand;
			byte zhiduiTing = gameData.mTingCards[position].zhiduiTing;
			if (dqmjProc.isJiaHu(cards, desk, card, zhiduiTing, position)) {
				// 自摸夹胡
				player_hu(gameData, desk, player, card, null, MJConstants.MAHJONG_HU_CODE_ZI_MO | MJConstants.MAHJONG_HU_CODE_JIA_HU);
				return true;
			}

			// 自摸平胡
			player_hu(gameData, desk, player, card, null, MJConstants.MAHJONG_HU_CODE_ZI_MO);
			return true;
		}

		return false;
	}

	/**
	 * 当玩家上听时，检测是否能胡牌(比如带漏、红中满天飞等)
	 * 当换宝时，也要按玩家上听顺序先后调用本方法检测是否能胡牌
	 */
	public boolean checkPlayerHuAfterTing(GameData gameData, DQMJDesk desk, PlayerInfo player) {
		//未上听玩家忽略
		if (gameData.mTingCards[player.position].tingCard == false || gameData.mTingCards[player.position].showBao == false) {
			return false;
		}

		List<Byte> cards = gameData.mPlayerCards[player.position].cardsInHand;
		byte zhiduiTing = gameData.mTingCards[player.position].zhiduiTing;
		Byte baoCard = gameData.mPublic.mBaoCard;
		List<Byte> baocardList = new ArrayList<Byte>();
		baocardList.add(baoCard);
		if (desk.canGunBao()) {
			baocardList.add(desk.getGunBaoCard(baoCard));
		}
		return false;
	}

	//流局
	private void liuju(GameData gameData, DQMJDesk desk) {
		//游戏结束
		gameData.setState(MJConstants.GAME_TABLE_STATE_SHOW_GAME_OVER_SCREEN);

		//记录
		gameData.recorder.recordPlayerAction(gameData.genSeq(), -1, MJConstants.MAHJONG_OPERTAION_HU, 0, 0, "流局", 1);

		// 结算番型和金币
		List<PlayerInfo> plist = desk.getPlayers();
		for (PlayerInfo px : plist) {
			gameData.mPlayerHandResult.playDetail[px.position].fanNum = 0;
			gameData.mPlayerHandResult.playDetail[px.position].fanType = MJConstants.MAHJONG_HU_CODE_LIUJU;
			//gameData.mPlayerHandResult.playDetail[px.position].score = 0;
		}
		gameData.mGameHu.reset();
		gameData.handEndTime = System.currentTimeMillis();
	}

	private void player_hu(GameData gameData, DQMJDesk desk, PlayerInfo pl, byte newCard, PlayerInfo pao_pl, int fanType) {
		Assert.isTrue(pl != null);
		//设置一局结束的状态,循环获取状态后结束这局游戏
		System.out.println("    收炮  playerInfo [" + pao_pl + "]");

		if(pao_pl != null && pl.playerId != pao_pl.playerId) { //收炮 //TODO WXD 非泛用
			if(!gameData.mTingCards[pao_pl.position].tingCard){ //没有报听的人打出的牌才能触发收炮
				//游戏中的记录
				gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, MJConstants.MAHJONG_OPERTAION_SHOUPAO, newCard, 0, "收炮", 1);
		
				// 结算番型和金币
				settle(gameData, desk, pl, pao_pl, fanType);
	
				GameOperPlayerHuSyn.Builder gb = GameOperPlayerHuSyn.newBuilder();
				gb.setCard(newCard);
				gb.setPosition(pl.position);
				gb.setPaoPosition(pao_pl.position);
				gb.setSkipHuSettle(false);
				gb.setWinType(MJHelper.getHuType(gameData.mPlayerHandResult.playDetail[pl.position].fanType) | MJConstants.MAHJONG_HU_CODE_SHOUPAO);
				
				for (PlayerInfo p : (List<PlayerInfo>) desk.getPlayers()) {
					int resultType = MJHelper.getResultType(gameData.mPlayerHandResult.playDetail[p.position].fanType);
					gb.setResultType(resultType);
					
					logger.info("huMsg:"+JsonFormat.printToString(gb.build()));
	
					PokerPushHelper.pushPlayerHuMsg(desk, p.position, gb, MJConstants.SEND_TYPE_SINGLE);	
				}
			}
			
			// 下个玩家进行摸牌动作
			player_mo(gameData, desk);
		} else { // 自摸
			gameData.setState(MJConstants.GAME_TABLE_STATE_SHOW_GAME_OVER_SCREEN);
			//游戏中的记录
			gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, MJConstants.MAHJONG_OPERTAION_HU, newCard, 0, "胡牌", 1);
	
			// 结算番型和金币
			settle(gameData, desk, pl, pao_pl, fanType);
	
			gameData.mGameHu.position = pl.position;
			gameData.mGameHu.huCard = newCard;
	
			gameData.handEndTime = System.currentTimeMillis();
		} 
	}
	
	@Override
	public int CalHuType(GameData gameData, DQMJDesk desk, PlayerInfo pl, byte newCard)
	{
		return 0;
	}
	
	@Override
	public int CalFanNum(int fanType)
	{
		return 1;
	}

	@Override
	public void gameTick(GameData gameData, DQMJDesk desk) {
		long ctt = System.currentTimeMillis();
		PlayerInfo currentPl = null;
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null)// 如果有等待操作，就让对方操作
		{
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
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);
				// 通知玩家出牌
				this.player_chu_notify(gameData, desk);
				System.out.println("   player_chu_notify  " + desk.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex()).playerId);
			}
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHU_ANIMATION) {
			if (ctt - gameData.getWaitingStartTime() > gameData.mGameParam.chuPlayMills)// 等待动画播完的时间
			{
				// 判断是否需要播放上宝动画
				if (gameData.playerChangeBao != -1 && gameData.playerChangeBao < 0) {
					gameData.playerChangeBao += 100;
					pushBaoChangeSyn(gameData, desk, -1, gameData.mPublic.mBaoCard, -1);
					gameData.setWaitingStartTime(System.currentTimeMillis());
					gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO);
				}
				// 是否需要换宝
				check_bao_change(gameData, desk);
				// 换宝成功，则等待前端换宝动画
				if (gameData.getPlaySubstate() == MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO) {
					return;
				}

				// 下一个玩家操作
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
			if (isNeedTrustGame || (isAutoOper && isActionTime))// 玩家超时或处于托管状态
			{
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
		} else if (substate == MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO) {
			if (ctt - gameData.getWaitingStartTime() > gameData.mGameParam.changeBaoMills) {
				// 下一个玩家操作
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_IDLE);// 玩家吃碰之类的操作，服务器等客户端播个动画

				// 找找下个玩家
				this.next_player_operation_notify(gameData, desk);
			}
		}
		return;
	}

	@Override
	public boolean isFinishQuan(GameData gameData, DQMJDesk gt) {
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
	public void selectBanker(GameData data, DQMJDesk desk) {
		// 看看胡牌玩家是否就是庄家（连胡或者流局不换庄）
		if ((data.mGameHu.position != -1) && (data.mPublic.mbankerPos != data.mGameHu.position)) {
			int dealerPos = data.mPublic.mbankerPos + 1;
			if (isFinishQuan(data, desk)) {
				data.quanNum++;
				dealerPos = 0;
			}
			PlayerInfo playerInfo = desk.getDeskPlayer(dealerPos);
			if (playerInfo == null) {
				logger.error("act=selectBanker;error=playerMiss;pos={};deskId={};", dealerPos, desk.getDeskID());
				return;
			}
			data.mPublic.mbankerPos = dealerPos;
			data.mPublic.mBankerUserId = playerInfo.playerId;
		}

		if (data.mPublic.mbankerPos == -1) {
			PlayerInfo playerInfo = desk.getDeskPlayer(0);
			data.mPublic.mbankerPos = playerInfo.position;
			data.mPublic.mBankerUserId = playerInfo.playerId;
		}
	}
	
	@Override
	public void setGuiCards(GameData gameData, DQMJDesk desk) {
		gameData.guiCards.add(-1);
	}

	@Override
	public void sendCards(GameData gameData, DQMJDesk desk) {
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
				}
				gameData.mPublic.mBaoCard = recorder.baoCard;
				gameData.mDeskCard.cards.clear();
				gameData.mDeskCard.cards.addAll(recorder.getInitCardCodeList());
				loadReplay = true;
			}
		}

		if (!loadReplay) {
			for (PlayerInfo pl : gameData.mPlayers) {
				if (pl == null)
					continue;
				List<Byte> cl = gameData.getCardsInHand(pl.position);
				cl.clear();
				List<Byte> src = new ArrayList<Byte>();

				boolean isBanker = pl.playerId == gameData.mPublic.mBankerUserId;

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
			// 默认第一张是宝牌
			gameData.mPublic.mBaoCard = gameData.popCard();
		}

		gameData.dice1 = (int) (System.nanoTime() % 6) + 1;
		gameData.dice2 = (int) (System.nanoTime() % 6) + 1;
		gameData.gameSeq = (int) (System.nanoTime() % 10000);

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
		msg.setCardLeft(gameData.getCardLeftNum());
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

		//
		gameData.setCurrentOpertaionPlayerIndex(gameData.mPublic.mbankerPos);
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_INIT_CARDS);
		gameData.showInitCardTime = System.currentTimeMillis();

		logger.info("act=onSendCard;seq={};players={}", msg.getSeq(), new Gson().toJson(gameData.mPlayers));
	}

	public byte findKaiPaiZhaVal(GameData gt, PlayerInfo player) {
		int findNum = 0;
		byte findVal = 0;
		for (Byte card : gt.getCardsInHand(player.position)) {
			if (card == findVal) {
				if (++findNum >= 4) {
					return card;
				}
				continue;
			}
			findNum = 1;
			findVal = card;
		}
		return 0;
	}

	// 计算金币输赢
	private void settle(GameData gameData, DQMJDesk desk, PlayerInfo winner, PlayerInfo pao_pl, int fanType) {
		// 不能自己点自己
		Assert.isTrue(pao_pl == null || pao_pl.playerId != winner.playerId);
		int fan = 1;// 基础1翻
		if ((fanType & MJConstants.MAHJONG_HU_CODE_JIA_HU) == MJConstants.MAHJONG_HU_CODE_JIA_HU) {
			fan *= 2;// 卡胡加一翻
		}
		if (pao_pl == null) {
			fan *= 2;// 自摸加一翻
			Assert.isTrue((fanType & MJConstants.MAHJONG_HU_CODE_ZI_MO) == MJConstants.MAHJONG_HU_CODE_ZI_MO);
		}
		// 庄家位置
		int dealer_pos = gameData.mPublic.mbankerPos;

		int dizhu = desk.getBasePoint();
		// 先所有人计算下输赢，然后再看看是否放炮包三家
		List<PlayerInfo> plist = desk.getPlayers();

		for (PlayerInfo px : plist) {
			int position = px.getTablePos();
			// 赢家不计算，他赢的等于其他输的人之和
			if (px.getPlayerID() == winner.getPlayerID()) {
				int win_fan = fanType | MJConstants.MAHJONG_HU_CODE_WIN | MJConstants.MAHJONG_HU_CODE_TING;
				if (position == dealer_pos)// 是庄家
				{
					logger.info("{} is banker;", position);
					gameData.mPlayerHandResult.playDetail[position].fanType = win_fan | MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA;
				} else {
					gameData.mPlayerHandResult.playDetail[position].fanType = win_fan;
				}
				gameData.mPlayerHandResult.playDetail[position].result = PlayHandResult.GAME_RESULT_WIN;
				continue;
			}
			// 输
			int pl_fan_type = fanType | MJConstants.MAHJONG_HU_CODE_LOSE;
			if (gameData.mTingCards[position].tingCard)
				pl_fan_type |= MJConstants.MAHJONG_HU_CODE_TING;
			int my_fan = fan;

			// 未听点炮加一番
			if (pao_pl != null && px.getPlayerID() == pao_pl.getPlayerID()) {
				pl_fan_type |= MJConstants.MAHJONG_HU_CODE_DIAN_PAO; // 点炮之人
				if (!gameData.mTingCards[position].tingCard) {
					my_fan *= 2; // 未听点牌翻一番
				}
			}

			if (dqmjProc.isMenQing(gameData.getCardsDown(position))) {
				my_fan *= 2; // 门清加一番
				pl_fan_type |= MJConstants.MAHJONG_HU_CODE_MEN_QING;
			}
			// 我是庄家
			if (position == dealer_pos) {
				logger.info("{} is banker2;", position);
				my_fan *= 2; // 庄家输翻番
				pl_fan_type |= MJConstants.MAHJONG_HU_CODE_MYSELF_ZHUANG_JIA;
			} else if (winner.position == dealer_pos)// 对手是庄家
			{
				my_fan *= 2; // 庄家赢, 闲家翻番
				pl_fan_type |= MJConstants.MAHJONG_HU_CODE_TARGET_ZHUANG_JIA;
			}
			int lose = my_fan * dizhu;
			//gameData.mPlayerHandResult.playDetail[position].score = lose;
			gameData.mPlayerHandResult.playDetail[position].fanType = pl_fan_type;
			gameData.mPlayerHandResult.playDetail[position].fanNum = my_fan;
		}
		boolean baoSanJia = false;
		// 点炮胡,如果没听包三家
		if (pao_pl != null) {
			// 没听，包三家
			if (gameData.mTingCards[pao_pl.position].tingCard == false) {
				baoSanJia = true;
			}
		}
		// 最后输赢处理
		int total_gold = 0;
		for (int i = 0; i < plist.size(); i++) {
			PlayerInfo px = plist.get(i);
			// 赢家不计算，他赢的等于其他输的人之和
			if (px.getPlayerID() == winner.getPlayerID())
				continue;
			//total_gold += gameData.mPlayerHandResult.playDetail[px.position].score;
			if (baoSanJia == false)// 包三家的情况最后处理
			{

			} else if (px.getPlayerID() != pao_pl.getPlayerID())// 包三家，且不是放炮的人
			{
				//gameData.mPlayerHandResult.playDetail[px.position].score = 0;
				gameData.mPlayerHandResult.playDetail[px.position].fanNum = 0;
			}
		}
		if (baoSanJia) {
			//gameData.mPlayerHandResult.playDetail[pao_pl.position].score = total_gold;
			gameData.mPlayerHandResult.playDetail[pao_pl.position].fanNum = total_gold / dizhu;
		}
		//gameData.mPlayerHandResult.playDetail[winner.position].score = total_gold;
		gameData.mPlayerHandResult.playDetail[winner.position].fanNum = (total_gold / dizhu);

		for (PlayerInfo p : plist) {
			int position = p.getTablePos();
			PlayFinalResult finalRes = gameData.mPlayerFinalResult.playDetail[position];
			PlayHandResult handRes = gameData.mPlayerHandResult.playDetail[position];
			//如果玩家输了，将score和fanNum改为负数
			if ((handRes.fanType & MJConstants.MAHJONG_HU_CODE_LOSE) == MJConstants.MAHJONG_HU_CODE_LOSE) {
				handRes.result = PlayHandResult.GAME_RESULT_LOSE;
				//handRes.score *= -1;
				handRes.fanNum *= -1;
			}
			//将每局结果记入到总结算中
			gameData.mergeHandResult(finalRes, handRes);
			String fanDesc = MJHelper.getFanDesc(handRes.fanType);
			logger.info("[" + p.position + ":" + handRes.fanType + "]" + fanDesc);
			logger.info("finalRes:" + new GsonBuilder().setPrettyPrinting().create().toJson(finalRes));

			//记录玩家的手牌,方便日后查
			gameData.mPlayerHandResult.playDetail[position].downcards = new Gson().toJson(gameData.mPlayerCards[position].cardsDown);
			gameData.mPlayerHandResult.playDetail[position].handcards = new Gson().toJson(gameData.mPlayerCards[position].cardsInHand);
			if (handRes.result == PlayHandResult.GAME_RESULT_WIN) {
				handRes.fanDesc = MJHelper.getResultTypeDesc(gameData.mPlayerHandResult.playDetail[p.position].fanType);
			}
		}
	}

	/**
	 * 吃碰、听、摸后都会调用这个方法提醒玩家出牌
	 * 
	 * @param gameData
	 * @param gt
	 */
	@Override
	public void player_chu_notify(GameData gameData, DQMJDesk gt) {
		//当前操作玩家
		PlayerInfo plx = gt.getDeskPlayer(gameData.getCurrentOpertaionPlayerIndex());

		if (plx == null) {
			logger.error("act=chuNotify;error=playerMiss;position={};deskId={};", gameData.getCurrentOpertaionPlayerIndex(), gt.getDeskID());
			return;
		}

		byte newCard = gameData.mPlayerAction[plx.position].cardGrab;// 摸到的一张牌

		// 是否可以自摸胡
		if (gameData.mTingCards[plx.position].tingCard && newCard > 0) {
			boolean hu = this.checkPlayerHuAfterMo(gameData, gt, plx);
			if (hu) {
				return;
			}
		}

		// 如果已经听牌，摸起来不能胡的牌，就自动打
		if (gameData.mTingCards[plx.position].tingCard) {
			// 模拟玩家进行出牌操作
			GameOperPlayerActionSyn.Builder gb = GameOperPlayerActionSyn.newBuilder();
			gb.setPosition(plx.getTablePos());
			gb.addCardValue(newCard & 0xff);
			gb.setAction(MJConstants.MAHJONG_OPERTAION_CHU);
			player_op_chu(gameData, gt, gb, plx);
			PokerPushHelper.pushActorSyn(gt, 0, plx.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
			return;
		}

		// 游戏服务器通知客户端，轮到玩家操作了
		ActionWaitingModel result2 = new ActionWaitingModel();
		result2.opertaion = MJConstants.MAHJONG_OPERTAION_CHU;
		result2.playerTableIndex = plx.position;
		result2.newCard = newCard;

		// 是不是取消了听
		boolean canTing = (gameData.mOpCancel[plx.position].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN) != MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN;
		//如果没有取消听，并且可以听，则提醒用户听牌
		canTing = canTing && dqmjProc.canTing(new DQMjCheckContext(gameData, gt, newCard, plx.position, false));

		// 可以听
		if (canTing) {
			// 自己摸的牌导致了可听牌，可以选择听牌，也可以选择不听而出牌
			result2.opertaion |= (MJConstants.MAHJONG_OPERTAION_TING | MJConstants.MAHJONG_OPERTAION_CHU);
			result2.playerTableIndex = plx.position;
			result2.tingList.addAll(gameData.mTingCards[plx.position].chuAndTingMap.keySet());
			result2.chuAndTingModel = new ChuTingModel();
			result2.chuAndTingModel.chuAndTingMap = gameData.mTingCards[plx.position].chuAndTingMap;
			gameData.setWaitingPlayerOperate(result2);
		}
		gameData.mPlayerAction[plx.position].opStartTime = System.currentTimeMillis();

		// 同步当前哪个玩家正在操作
		PokerPushHelper.pushActorSyn(gt, -100, plx.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);
		//提醒当前玩家进行操作
		notifyPlayerWaitingOperation(gameData, gt, plx, result2);
		//同步手牌
		PokerPushHelper.pushHandCardSyn(gameData, gt, plx);

		if (isEnd(gameData))// 流局了
		{
			gameData.mActor.gameState = MJConstants.MJStateFinish;
			gameData.handEndTime = System.currentTimeMillis();

			// 设置胡牌玩家为-1
			gameData.mGameHu.position = -1;
		}

		return;
	}

	private void player_op_chi(GameData gameData, DQMJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		if (msg.getCardValueCount() != 2) {
			gt.sendErrorMsg(pl.position, "吃牌错误,需要提供两张牌");
			logger.error("act=chi;stage=gaming;error=invalidCardCount;position={};deskId={};", pl.position, gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}
		// 玩家吃牌
		int v1 = msg.getCardValue(0) & 0xff;
		int v2 = msg.getCardValue(1) & 0xff;
		//
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			gt.sendErrorMsg(pl.position, "吃牌错误,当前牌型不能吃牌");
			logger.error("act=chi;stage=gaming;error=noSuchOperation;position={};deskId={};", pl.position, gt.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}

		if (waiting.playerTableIndex != pl.getTablePos()) {
			gt.sendErrorMsg(pl.position, "吃牌错误,没轮到你吃牌");
			logger.error("act=chi;stage=gaming;error=invalidPosition;position={};deskId={};desc=吃牌错误,没轮到你吃牌;", pl.position, gt.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		// 吃，或者吃听
		if (((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) != MJConstants.MAHJONG_OPERTAION_CHI)
				&& ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI_TING) != MJConstants.MAHJONG_OPERTAION_CHI_TING)) {
			gt.sendErrorMsg(pl.position, "吃牌错误,命令码错误");
			logger.error("act=chi;stage=gaming;error=invalidOperation;position={};deskId={};", pl.position, gt.getDeskID());
			return;// 当前不能吃；
		}

		CardCombo combo = waiting.chi_check(v1, v2);
		if (combo == null) {
			gt.sendErrorMsg(pl.position, "吃牌错误,找不到可吃的组合");
			logger.error("act=chi;stage=gaming;error=comboMiss;position=" + pl.position + ";deskId="+desk.getDeskID()+";detail=v1:" + v1 + ",v2:" + v2 + ";isAuto:" + gameData.mPlayerAction[pl.position].autoOperation);
			return;// 吃的牌不对；
		}

		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, msg.getAction(), v1, v2, MJHelper.getActionName(msg.getAction()) + ":" + MJHelper.getSingleCardName(combo.card1) + "_"
				+ MJHelper.getSingleCardName(combo.card2) + "_" + MJHelper.getSingleCardName(combo.targetCard), 0);

		// 有可能玩家吃听，这时候要把当前操作的索引改成此玩家
		gameData.setCurrentOpertaionPlayerIndex(pl.getTablePos());
		//
		gameData.removeCardInHand(v1, pl.position, CardChangeReason.CHI);
		gameData.removeCardInHand(v2, pl.position, CardChangeReason.CHI);
		gameData.add_Down_cards((byte) v1);
		gameData.add_Down_cards((byte) v2);
		//
		
		gameData.addCardDown(combo.targetCard, v1, v2, 2, pl.position);
		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushHandCardSyn(gameData, gt, pl);

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI_TING) == MJConstants.MAHJONG_OPERTAION_CHI_TING) {
			ActionWaitingModel result2 = new ActionWaitingModel();
			result2.chuAndTingModel = waiting.chiChuAndTingModelMap.get(combo);
			result2.tingList.addAll(result2.chuAndTingModel.chuAndTingMap.keySet());
			result2.playerTableIndex = pl.position;

			if (result2 != null && result2.chuAndTingModel.canTing()) {
				// 先出牌，再支对
				result2.opertaion = MJConstants.MAHJONG_OPERTAION_CHU; // 已听牌，下一步出牌
				gameData.addTingPl(pl);
				gameData.setWaitingPlayerOperate(result2);
				
				// 设置操作开始时间
				PlayerInfo result_pl = gt.getDeskPlayer(result2.playerTableIndex);
				gameData.mPlayerAction[result_pl.position].opStartTime = System.currentTimeMillis();
				
				// 提示玩家出牌
				notifyPlayerWaitingOperation(gameData, gt, result_pl, result2);

				// 清空刚才被吃的牌
				gameData.setCurrentCard((byte) 0);
				gameData.setCardOpPlayerIndex(-1);
			} else {
				gt.sendErrorMsg(pl.position, "吃牌后听牌失败");
				logger.error("act=chi;stage=gaming;error=tingFailAfterChi;position={};deskId={};", pl.position, gt.getDeskID());
			}
		} else {
			// 服务器清除等待玩家操作的数据
			gameData.setWaitingPlayerOperate(null);

			// 等待客户端播动画
			gameData.setWaitingStartTime(System.currentTimeMillis());
			gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHI_PENG_ANIMATION);

			//清空刚才被吃的牌
			gameData.setCurrentCard((byte) 0);
			gameData.setCardOpPlayerIndex(-1);
		}
	}

	private void player_op_peng(GameData gameData, DQMJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
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

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) != MJConstants.MAHJONG_OPERTAION_PENG
				&& ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG_TING) != MJConstants.MAHJONG_OPERTAION_PENG_TING)) {
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
		//

		gameData.removeCardInHand(v1, pl.position, CardChangeReason.PENG);
		gameData.removeCardInHand(v1, pl.position, CardChangeReason.PENG);
		gameData.add_Down_cards((byte) v1);
		gameData.add_Down_cards((byte) v1);
		//
		PokerPushHelper.pushActorSyn(gt, -100, pl.position, 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);

		gameData.addCardDown(waiting.targetCard & 0xff, 0, 0, 1, pl.position);

		PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
		PokerPushHelper.pushHandCardSyn(gameData, gt, pl);

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG_TING) == MJConstants.MAHJONG_OPERTAION_PENG_TING) {
			ActionWaitingModel result2 = new ActionWaitingModel();
			result2.chuAndTingModel = waiting.pengChuAndTingModel;
			result2.tingList.addAll(result2.chuAndTingModel.chuAndTingMap.keySet());
			result2.playerTableIndex = pl.position;

			if (result2 != null && result2.chuAndTingModel.canTing()) {
				gameData.addTingPl(pl);
				gameData.setWaitingPlayerOperate(result2);
				// 设置操作开始时间
				result2.opertaion = MJConstants.MAHJONG_OPERTAION_CHU; // 已听牌，下一步出牌
				PlayerInfo result_pl = gt.getDeskPlayer(result2.playerTableIndex);
				gameData.mPlayerAction[result_pl.position].opStartTime = System.currentTimeMillis();

				notifyPlayerWaitingOperation(gameData, gt, pl, result2);

				// 清空刚才被吃的牌
				gameData.setCurrentCard((byte) 0);
				gameData.setCardOpPlayerIndex(-1);
			} else {
				gt.sendErrorMsg(pl.position, "碰牌后听牌失败");
				logger.error("act=ting;error=tingFail;position={};deskId={};", pl.position, gt.getDeskID());
			}
		} else {
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
	}

	private void player_cancel(GameData gameData, DQMJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
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

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG_TING) == MJConstants.MAHJONG_OPERTAION_PENG_TING
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI_TING) == MJConstants.MAHJONG_OPERTAION_CHI_TING
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_TING) == MJConstants.MAHJONG_OPERTAION_TING) {
			gameData.mOpCancel[pl.position].cancelOp |= MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN;
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) == MJConstants.MAHJONG_OPERTAION_PENG
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) == MJConstants.MAHJONG_OPERTAION_CHI) {
			gameData.mOpCancel[pl.position].cancelOp |= MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN;
		}

		// 如果是取消吃碰，则下一个操作人应该是出牌人的下家
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) == MJConstants.MAHJONG_OPERTAION_PENG
				|| (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) == MJConstants.MAHJONG_OPERTAION_CHI) {
			List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
			gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);
		}

		// 如果是取消支对，则下一个操作人应该是出牌人的下家
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZD_TING) == MJConstants.MAHJONG_OPERTAION_ZD_TING) {
			boolean showBao = setPlayerSawBao(gameData, desk, pl.position);

			boolean ret = checkPlayerHuAfterTing(gameData, desk, pl);
			if (ret) {
				return;
			}
			// 直接播放上宝动画
			if (showBao) {
				gameData.playerChangeBao += 100;
				pushBaoChangeSyn(gameData, desk, -1, gameData.mPublic.mBaoCard, -1);
				gameData.setWaitingStartTime(System.currentTimeMillis());
				gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO);
				return;
			}
			// reset4NextPlayerOperation(gameData, gt);
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

	private void notifyPlayerWaitingOperation(GameData gameData, DQMJDesk gt, PlayerInfo pl, ActionWaitingModel waiting) {
		GameOperPlayerActionNotify.Builder msg = GameOperPlayerActionNotify.newBuilder();
		msg.setPosition(pl.getTablePos());
		msg.setActions(waiting.opertaion);
		msg.setLastActionCard(gameData.getCurrentCard());
		if (gameData.getCurrentCard() <= 0) {
			msg.setLastActionPosition(gameData.getCurrentOpertaionPlayerIndex());
		} else {
			msg.setLastActionPosition(gameData.getCardOpPlayerIndex());
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZD_TING) == MJConstants.MAHJONG_OPERTAION_ZD_TING) {
			msg.addAllTingDzs(waiting.zhiDuiTingList);
			msg.setMustZhiDui(waiting.mustZhiDui);
		}

		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) == MJConstants.MAHJONG_OPERTAION_CHU) {
			for (byte a : waiting.tingList) {
				msg.addTingList(a); // 告诉客户端，你只能出这些牌
			}
		}

		boolean pengTing = (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG_TING) == MJConstants.MAHJONG_OPERTAION_PENG_TING;
		boolean chiTing = (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI_TING) == MJConstants.MAHJONG_OPERTAION_CHI_TING;
		if (pengTing) {
			MJHelper.copyPengArg(waiting.peng_card_value, msg);
		}
		if (chiTing) {
			MJHelper.copyChiArg(waiting.chiCombos, msg);
		}

		boolean peng = (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) == MJConstants.MAHJONG_OPERTAION_PENG;
		boolean chi = (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) == MJConstants.MAHJONG_OPERTAION_CHI;
		if (peng) {
			MJHelper.copyPengArg(waiting.peng_card_value, msg);
		}
		if (chi) {
			MJHelper.copyChiArg(waiting.chiCombos, msg);
		}

		msg.setNewCard(waiting.newCard);

		PokerPushHelper.pushActorSyn(gt, pl.position, pl.position, 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_SINGLE);
		PokerPushHelper.pushActionNofity(gameData, gt, pl.position, msg, MJConstants.SEND_TYPE_SINGLE);
	}

	private void player_op_ting(GameData gameData, DQMJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
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

		// 玩家不碰（被提示玩家只有碰的操作），那就看看当前玩家是否可以吃，不能吃就摸牌
		if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_TING) == MJConstants.MAHJONG_OPERTAION_TING) {
			gameData.addTingPl(pl);
			// 把听牌消息广播给桌子上所有成员
			PokerPushHelper.pushActionSyn(gt, -100, msg, MJConstants.SEND_TYPE_ALL);
			waiting.opertaion = MJConstants.MAHJONG_OPERTAION_CHU; // 已听牌，下一步出牌
			notifyPlayerWaitingOperation(gameData, gt, pl, waiting);
		}
	}

	private void player_op_danzhidui(GameData gameData, DQMJDesk desk, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		int v1 = (msg.getCardValue(0) & 0xFF);
		gameData.recorder.recordPlayerAction(gameData.genSeq(), pl.position, MJConstants.MAHJONG_OPERTAION_ZD_TING, v1, v1,
				"单只对:" + MJHelper.getSingleCardName(v1 & 0xFF) + "_" + MJHelper.getSingleCardName(v1 & 0xFF), 0);

		// 玩家听牌-单只对
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting == null) {
			desk.sendErrorMsg(pl.position, "当前牌型不能支对");
			logger.error("act=playerZhidui;error=operationMiss;position={};deskId={};", pl.position, desk.getDeskID());
			return;// 当前没有在等待某个玩家操作；
		}
		//
		if (waiting.playerTableIndex != pl.getTablePos()) {
			desk.sendErrorMsg(pl.position, "支对错误,未轮到你操作");
			logger.error("act=playerZhidui;error=notPlayerOperationIndex;position={};deskId={};desc=支对错误,未轮到你操作;", pl.position, desk.getDeskID());
			return;// 当前不是在等这个玩家操作
		}

		// 发给其他玩家，让他们知道当前轮到谁操作
		PokerPushHelper.pushActorSyn(desk, 0, pl.getTablePos(), 12, gameData.getCardLeftNum(), MJConstants.SEND_TYPE_ALL);

		// 单只对选择
		if (gameData.mTingCards[pl.position].tingCard && waiting.opertaion == MJConstants.MAHJONG_OPERTAION_ZD_TING) {
			if (waiting.zhiDuiTingList.contains(v1)) {
				// 把单只对听牌消息广播给桌子上所有成员
				PokerPushHelper.pushActionSyn(desk, -100, msg, MJConstants.SEND_TYPE_ALL);
				gameData.mTingCards[pl.position].zhiduiTing = (byte) v1;
				gameData.mTingCards[pl.position].cards.clear();
				gameData.mTingCards[pl.position].cards.add((byte) v1);
				boolean showBao = setPlayerSawBao(gameData, desk, pl.position);
				if (checkPlayerHuAfterTing(gameData, desk, pl)) {
					return;
				}
				gameData.setWaitingPlayerOperate(null);
				// next_player_operation_notify(gameData, gt);
				if (showBao) {
					gameData.playerChangeBao += 100;
					pushBaoChangeSyn(gameData, desk, -1, gameData.mPublic.mBaoCard, -1);
					gameData.setWaitingStartTime(System.currentTimeMillis());
					gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO);
					return;
				}
				reset4NextPlayerOperation(gameData, desk);
			} else {
				desk.sendErrorMsg(pl.position, "对子不在允许的范围列表内");
				logger.error("act=playerZhidui;error=duiziNotInList;position={};deskId={};", pl.position, desk.getDeskID());
			}
		} else {
			desk.sendErrorMsg(pl.position, "当前不是支对状态");
			logger.error("act=playerZhidui;error=operationPermit;position={};deskId={};", pl.position, desk.getDeskID());
		}
	}

	private void player_op_chu(GameData gameData, DQMJDesk desk, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		byte card_value = (byte) (msg.getCardValue(0) & 0Xff);
		int idx = gameData.getCurrentOpertaionPlayerIndex(); // 当前打牌的玩家索引

		// 看看之前刚摸的牌有没有，摸的牌，先放cardgrab变量里面，等他出牌的时候再放进去，这样断线重连的时候，他摸的还是原来那张
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
						+ ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_TING) == MJConstants.MAHJONG_OPERTAION_TING), 0);

		if (card_v != card_value || pl_pos != pl.getTablePos()) {
			desk.sendErrorMsg(pl.position, "出牌错误,不存在的牌");
			logger.error("act=playerChu;error=notcurrentoperation2;position={};deskId={};", pl.position, desk.getDeskID());
			return;
		}

		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null && (waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) != MJConstants.MAHJONG_OPERTAION_CHU) {
			desk.sendErrorMsg(pl.position, "出牌错误,当前不能进行出牌操作");
			logger.error("act=playerChu;error=notallowchu;position={};deskId={};", pl.position, desk.getDeskID());
			return;
		}

		MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.position];
		
		// 玩家听牌了，当他打出了牌，就可以看宝了
		if (tingModel.tingCard && !tingModel.showBao) {
			// 这里表示：
			if (waiting == null) {
				desk.sendErrorMsg(pl.position, "出牌错误,不是听牌状态");
				logger.error("act=playerChu;error=waitingisnull;position={};deskId={};", pl.position, desk.getDeskID());
				return;
			}

			if (!waiting.tingList.contains(card_value)) {
				desk.sendErrorMsg(pl.position, "出牌错误,不在指定牌列表中的牌不能出");
				logger.error("act=chi;stage=gaming;error=chuCardNotInList;position={};deskId={};", pl.position, desk.getDeskID());
				return;
			}
		}

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

		// 玩家听牌了，当他打出了牌，就可以看宝了
		if (tingModel.tingCard && !tingModel.showBao) {
			// gameData.mTingCards[pl.position].showBao = true;
			tingModel.cards = waiting.chuAndTingModel.chuAndTingMap.get(card_v);

			// 提醒玩家支对
			Set<Integer> zhiduiCards = new HashSet<Integer>();
			for (byte card2Ting : tingModel.cards) {
				int count = MJHelper.getCardCount(gameData.mPlayerCards[pl.position].cardsInHand, card2Ting);
				if (count >= 2) {
					zhiduiCards.add((int) card2Ting);
				}
			}
			if (desk.canZhiDuiHU()&&zhiduiCards.size() >= 2) { // 可以提醒用户支对听
				waiting.opertaion = MJConstants.MAHJONG_OPERTAION_ZD_TING;
				waiting.tingList.clear();
				Set<Integer> zhiDuiTingCards = checkZhiDui(zhiduiCards,gameData.getCardsInHand(pl.position));
				waiting.zhiDuiTingList.addAll(zhiDuiTingCards);
				waiting.mustZhiDui = zhiDuiTingCards.size()==tingModel.cards.size() && desk.canWuJiaBuHu()?1:0;
				gameData.setWaitingPlayerOperate(waiting);
				notifyPlayerWaitingOperation(gameData, desk, pl, waiting);
				return;
			} else { // 不要提醒用户支对听
				setPlayerSawBao(gameData, desk, pl.position);
				if (checkPlayerHuAfterTing(gameData, desk, pl)) {
					return;
				}
			}
		}

		reset4NextPlayerOperation(gameData, desk);
	}

	private void player_op_shuaijiuyao(GameData gameData, DQMJDesk desk, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		System.out.println("  =====================  shuaijiuyao  " + msg.getCardValueList().size());
		if(msg.getCardValueList().size() == 9) {
			System.out.println("  ===================== 九张加倍！！！  ");//TODO WXD 计算
		}
		for(int i = 0; i < msg.getCardValueList().size(); i++) {
			int cardpoint = msg.getCardValue(i);
			gameData.removeCardInHand(cardpoint, pl.getTablePos(), CardChangeReason.SHUAI_JIU_YAO);
		}
	}

	/**
	 * 在支队时再次校验可以支队的逻辑
	 * 1.找出之前认可可以上听的牌,依次组队形成支队的基本条件,2个对子
	 * 2.将这两个对子从手牌移除,如果能胡牌,说明这两个可以支队
	 * @param zhiduiCards
	 * @param cardsInHand
	 * @return
	 */
	private Set<Integer> checkZhiDui(Set<Integer> zhiduiCards, List<Byte> cardsInHand) {
		Set<Integer> result = new HashSet();
		for(Integer c1:zhiduiCards){
			Set<Integer> zhiDuiCardsTemp = new HashSet<Integer>(zhiduiCards);
			zhiDuiCardsTemp.remove(Integer.valueOf(c1));
			for(Integer c2:zhiDuiCardsTemp) {
				List<Byte> shouPaiTemp = new ArrayList<Byte>(cardsInHand);
				shouPaiTemp.remove(Byte.valueOf(c1+""));
				shouPaiTemp.remove(Byte.valueOf(c1+""));
				shouPaiTemp.remove(Byte.valueOf(c2+""));
				shouPaiTemp.remove(Byte.valueOf(c2+""));
			}
		}
		return result;
	}

	private void reset4NextPlayerOperation(GameData gameData, DQMJDesk desk) {
		// 等待客户端播动画
		gameData.setWaitingStartTime(System.currentTimeMillis());
		gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_PLAYING_CHU_ANIMATION);// 设置玩家的子原因为3，客户端在播出牌动画

		// check_bao_change(gameData, desk);

		// 顺序，轮到下一个玩家行动
		List<PlayerInfo> nextPlayer = desk.loopGetPlayer(gameData.getCardOpPlayerIndex(), 1, 0);
		gameData.setCurrentOpertaionPlayerIndex(nextPlayer.get(0).position);
	}

	public boolean isEnd(GameData gameData) {
		// 最后要剩8，9张牌
		if (gameData.mDeskCard.cards.size() < 8)
			return true;
		//
		return false;
	}

	// 出牌结束，没有吃碰胡
	private void chu_end(GameData gameData, DQMJDesk desk) {
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
	private boolean game_only_zimo(GameData gameData, DQMJDesk desk) {
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
			if (gameData.mTingCards[plx.position].tingCard && checkPlayerHuAfterMo(gameData, desk, plx)) {
				return true;
			}

			// 顺序，轮到下一个玩家行动
			gameData.move2NextPlayer(desk);
		}

		// 如果分张阶段，没有人胡，那么流局了
		liuju(gameData, desk);

		return true;
	}

	public boolean setPlayerSawBao(GameData gameData, DQMJDesk desk, int position) {
		gameData.mTingCards[position].showBao = true;
		if (gameData.playerChangeBao == -1) { // 第一次上宝
			gameData.playerChangeBao = position - 100;
			return true;
		}
		return false;
	}

	public void player_mo(GameData gameData, DQMJDesk desk) {
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

		// 如果是最后分张阶段了，就只能自摸了
		if (gameData.isInFinalStage()) {
			game_only_zimo(gameData, desk);
			return;
		} else {
			int cardTotal = gameData.getCardNumInHand(plx.position) + gameData.mPlayerCards[plx.position].cardsDown.size() * 3;
			if (cardTotal != 13) {
				logger.error("act=playerMo;error=errorCardCount;position={};deskId={};msg=大件事了,摸错牌啦!!!!!!!!!!!!!!!!!;", plx.position, desk.getDeskID());
				throw new RuntimeException("大件事了,摸错牌啦2.0!!!!!!!!!!!!!!!!!" + cardTotal);
			}
			if (gameData.getCardNumInHand(plx.position) % 3 == 1) {
				// 给玩家摸一张
				byte b = gameData.popCard();
				gameData.mPlayerAction[plx.position].cardGrab = b;
				gameData.recorder.recordPlayerAction(gameData.genSeq(), plx.position, MJConstants.MAHJONG_OPERTAION_MO, b, 0, "摸牌:" + MJHelper.getSingleCardName(b), 1);
				pushPlayerMoMsg(gameData, desk, plx, b);

				player_chu_notify(gameData, desk);
				return;
			} else {
				throw new RuntimeException("大件事了,摸错牌啦!!!!!!!!!!!!!!!!!;position="+plx.position+";deskId="+desk.getDeskID());
			}
		}

	}

	private void pushPlayerMoMsg(GameData gameData, DQMJDesk desk, PlayerInfo plx, byte b) {
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
		if (gameData.mPlayerCards[position].cardsInHand.size() <= 4) {
			return null; // 不能手把一
		}
		ActionWaitingModel code = new ActionWaitingModel();
		code.playerTableIndex = position;

		List<Byte> cards = gameData.mPlayerCards[position].cardsInHand;
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

	/**
	 * 换宝看上听玩家的操作或者有人打了一张牌看下别的玩家有没有操作
	 * @param gameData
	 * @param desk
	 * @return
	 */
	public boolean next_player_operation_notify(GameData gameData, DQMJDesk desk) {
		byte card = gameData.getCurrentCard();// 当前打出的牌

		PlayerInfo opPlayer = desk.getDeskPlayer(gameData.getCardOpPlayerIndex());

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

		// 如果进入最后摸牌阶段，不能吃碰放炮，只能自摸
		if (gameData.isInFinalStage()) {
			player_mo(gameData, desk);
			return false;
		}

		// 1先看看有没有胡的玩家
		do {
			// 按优先级，先胡
			result = loopCheckHu(gameData, desk, card, opPlayer.position);
			if (result != null) {
				break;
			}

			// 按优先级，再抢听(碰听/吃听)
			result = loopCheckTing(gameData, desk, card, opPlayer.position);
			if (result != null) {
				break;
			}

			// 2看看有没有玩家可以碰
			result = loopCheckPeng(gameData, desk, card, opPlayer.position);
			if (result != null) {
				// 如果是下一个玩家刚好碰，再看看是否能吃，一起提示了
				if (result.playerTableIndex == plx.getTablePos()) {
					// 3看看有没有玩家可以吃
					ActionWaitingModel chi = check_chi(gameData, card, plx.getTablePos());
					if (chi != null) {
						result.chiCombos = chi.chiCombos;
						result.opertaion |= MJConstants.MAHJONG_OPERTAION_CHI;
					}
				}
				break;
			}

			// 3看看有没有玩家可以吃
			boolean cancelChi = (gameData.mOpCancel[plx.position].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) == MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN;
			if (gameData.mTingCards[plx.position].tingCard == false && !cancelChi) {
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
			gameData.mPlayerAction[pl.position].opStartTime = System.currentTimeMillis();
			if (result.opertaion == MJConstants.MAHJONG_OPERTAION_HU) {
				int fanType = result.isKaHu ? MJConstants.MAHJONG_HU_CODE_JIA_HU : 0;
				player_hu(gameData, desk, pl, card, opPlayer, fanType);
				return false;
			} else {
				// 其他操作发给自己
				notifyPlayerWaitingOperation(gameData, desk, pl, result);
			}
		} else {
			// 4如果什么操作都没有，下个玩家进行摸牌动作
			player_mo(gameData, desk);
			return true;
		}
		return true;
	}

	public boolean isTingJia(GameData gt, DQMJDesk desk, int position) {
		for (byte card : gt.mTingCards[position].cards) {
			List<Byte> cards = gt.mPlayerCards[position].cardsInHand;
			byte zhiduiTing = gt.mTingCards[position].zhiduiTing;
			if (dqmjProc.isJiaHu(cards, desk, card, zhiduiTing, position)) {
				return true;
			}
		}
		return false;
	}

	// 别人打的牌，自己能不能胡
	private ActionWaitingModel loopCheckHu(GameData gt, DQMJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			MyGame_Player_Ting_Cards tingModel = gt.mTingCards[pl.position];
			if (!tingModel.tingCard) {
				continue;
			}
			if (gt.mTingCards[pl.position].zhiduiTing > 0 && gt.mTingCards[pl.position].zhiduiTing != card) {
				continue;
			}
			if (!tingModel.cards.contains(card)) {
				continue;
			}
			// 可以胡
			ActionWaitingModel code = new ActionWaitingModel();
			code.playerTableIndex = pl.position;

			List<Byte> cards = gameData.mPlayerCards[pl.position].cardsInHand;
			byte zhiduiTing = gameData.mTingCards[pl.position].zhiduiTing;
			if (dqmjProc.isJiaHu(cards, desk, card, zhiduiTing, pl.position)) {
				code.isKaHu = true;
			}
			if (desk.canWuJiaBuHu() && !code.isKaHu) {
				return null;
			}
			code.opertaion = MJConstants.MAHJONG_OPERTAION_HU;
			return code;
		}
		return null;
	}

	private ActionWaitingModel loopCheckPeng(GameData gameData, DQMJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.position];
			if (tingModel.tingCard) {
				continue;
			}
			if ((gameData.mOpCancel[pl.position].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) == MJConstants.MAHJONG_CANCEL_OPER_OTHER_TURN) {
				continue;
			}
			if (gameData.mPlayerCards[pl.position].cardsInHand.size() <= 4) {
				continue; // 不能手把一
			}
			ActionWaitingModel code = dqmjProc.check_peng(gameData, card, pl);
			if (code != null && code.opertaion > 0) {
				return code;
			}
		}
		return null;
	}

	private ActionWaitingModel loopCheckTing(GameData gameData, DQMJDesk desk, byte card, int cardOpPosition) {
		List<PlayerInfo> list = desk.loopGetPlayer(cardOpPosition, -1, 0);
		for (PlayerInfo pl : list) {
			MyGame_Player_Ting_Cards tingModel = gameData.mTingCards[pl.getTablePos()];
			if (tingModel.tingCard) {
				continue;
			}
			if ((gameData.mOpCancel[pl.getTablePos()].cancelOp & MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN) == MJConstants.MAHJONG_CANCEL_OPER_SELF_TURN) {
				continue;
			}
			if (gameData.mPlayerCards[pl.getTablePos()].cardsInHand.size() <= 4) {
				continue; // 不能手把一
			}
			DQMjCheckContext ctx = new DQMjCheckContext(gameData, desk, card, pl.position, true);
			boolean canTing = dqmjProc.canTing(ctx);

			if (!canTing) {
				continue;
			}

			ActionWaitingModel code = new ActionWaitingModel();
			{
				// 检查是否可以 碰听
				List<Byte> handCards = new ArrayList<Byte>();
				List<Integer> cardsDown = new ArrayList<Integer>();
				cardsDown.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsDown);
				handCards.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsInHand);
				MJHelper.add2SortedList(card, handCards);

				if (handCards.remove((Byte) card) && handCards.remove((Byte) card) && handCards.remove((Byte) card)) {
					MJHelper.addCardDown(card, 0, 1, cardsDown);
					Map<Byte, Set<Byte>> chuAndTingMap = dqmjProc.canTingInternal(handCards, cardsDown, ctx).chuAndTingMap;
					if (chuAndTingMap.isEmpty() == false) {
						code.opertaion |= MJConstants.MAHJONG_OPERTAION_PENG_TING;
						code.pengChuAndTingModel = new ChuTingModel();
						code.pengChuAndTingModel.chuAndTingMap = chuAndTingMap;
						code.peng_card_value = card;
						code.playerTableIndex = pl.position;
					}
				}
			}
			{
				// 检查是否可以吃:3,4,5
				List<Byte> handCards = new ArrayList<Byte>();
				List<Integer> cardsDown = new ArrayList<Integer>();
				cardsDown.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsDown);
				handCards.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsInHand);
				MJHelper.add2SortedList(card, handCards);
				Byte card1 = (byte) (card + 1);
				Byte card2 = (byte) (card + 2);
				if (handCards.remove((Byte) card) && handCards.remove(card1) && handCards.remove(card2)) {
					MJHelper.addCardDown(card, card, 2, cardsDown);
					Map<Byte, Set<Byte>> chuAndTingMap = dqmjProc.canTingInternal(handCards, cardsDown, ctx).chuAndTingMap;
					if (chuAndTingMap.isEmpty() == false) {
						code.opertaion |= MJConstants.MAHJONG_OPERTAION_CHI_TING;
						CardCombo combo = new CardCombo(card1, card2, card);
						code.chiCombos.add(combo);
						ChuTingModel model = new ChuTingModel();
						model.chuAndTingMap = chuAndTingMap;
						code.chiChuAndTingModelMap.put(combo, model);
						code.playerTableIndex = pl.getTablePos();
					}
				}
			}
			{
				// 检查是否可以吃:2,3,4
				List<Byte> handCards = new ArrayList<Byte>();
				List<Integer> cardsDown = new ArrayList<Integer>();
				cardsDown.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsDown);
				handCards.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsInHand);
				MJHelper.add2SortedList(card, handCards);
				Byte card1 = (byte) (card + 1);
				Byte card_1 = (byte) (card - 1);
				if (handCards.remove((Object) card) && handCards.remove(card1) && handCards.remove(card_1)) {
					MJHelper.addCardDown(card_1, card1, 2, cardsDown);
					Map<Byte, Set<Byte>> chuAndTingMap = dqmjProc.canTingInternal(handCards, cardsDown, ctx).chuAndTingMap;
					if (chuAndTingMap.isEmpty() == false) {
						code.opertaion |= MJConstants.MAHJONG_OPERTAION_CHI_TING;
						CardCombo combo = new CardCombo(card_1, card1, card);
						code.chiCombos.add(combo);
						ChuTingModel model = new ChuTingModel();
						model.chuAndTingMap = chuAndTingMap;
						code.chiChuAndTingModelMap.put(combo, model);
						code.playerTableIndex = pl.getTablePos();
					}
				}
			}
			{
				// 检查是否可以吃:1,2,3
				List<Byte> handCards = new ArrayList<Byte>();
				List<Integer> cardsDown = new ArrayList<Integer>();
				cardsDown.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsDown);
				handCards.addAll(gameData.mPlayerCards[pl.getTablePos()].cardsInHand);
				MJHelper.add2SortedList(card, handCards);
				Byte card_1 = (byte) (card - 1);
				Byte card_2 = (byte) (card - 2);
				if (handCards.remove((Byte) card) && handCards.remove(card_1) && handCards.remove(card_2)) {
					MJHelper.addCardDown(card_2, card, 2, cardsDown);
					Map<Byte, Set<Byte>> chuAndTingMap = dqmjProc.canTingInternal(handCards, cardsDown, ctx).chuAndTingMap;
					if (chuAndTingMap.isEmpty() == false) {
						code.opertaion |= MJConstants.MAHJONG_OPERTAION_CHI_TING;
						CardCombo combo = new CardCombo(card_2, card_1, card);
						code.chiCombos.add(combo);
						ChuTingModel model = new ChuTingModel();
						model.chuAndTingMap = chuAndTingMap;
						code.chiChuAndTingModelMap.put(combo, model);
						code.playerTableIndex = pl.getTablePos();
					}
				}
			}
			if (code.opertaion > 0) {
				return code;
			}
		}
		return null;
	}

	public boolean check_bao_change(GameData gt, DQMJDesk desk) {
		System.out.println(gt.getTIngPls());
		if (!gt.havePlayerTingCard()) {
			return false;
		}

		byte bao = gt.mPublic.mBaoCard;
		byte gunbao = desk.getGunBaoCard(bao);
		List<PlayerInfo> plist = desk.getPlayers();
		int bao_num = 0;
		for (int i = 0; i < plist.size(); i++) {
			PlayerInfo pl = plist.get(i);
			bao_num += gt.getVisibleCardNum(bao, pl.position);
			if (bao_num >= 3)
				break;
		}
		//滚宝时若宝为红中出现三次换宝,若为其余牌7次换宝
		if(desk.canGunBao()){
			for (int i = 0; i < plist.size(); i++) {
				PlayerInfo pl = plist.get(i);
				bao_num += gt.getVisibleCardNum(gunbao, pl.position);
				if (bao_num >= 3)
					break;
			}
		}

		// 刚出的牌也要判断一下，不然就少算了
		byte card = gt.getCurrentCard();
		if (card != 0 && card == bao) {
			bao_num++;
		}
		if(desk.canGunBao()){
			if (card != 0 && card == gunbao) {
				bao_num++;
			}
		}

		// 需要换宝
		boolean canChangeBao = !desk.canHZMTF() || (gt.mPublic.mBaoCard != MJConstants.MAHJONG_CODE_HONG_ZHONG && desk.canHZMTF());
		boolean needChangeBao = (bao_num >= 3 && !desk.canGunBao()) || (bao_num >= 7 && desk.canGunBao()) ;
		if (canChangeBao && needChangeBao) {
			byte new_bao = gt.popCard();
			byte old_bao = gt.mPublic.mBaoCard;
			gt.mPublic.mBaoCard = new_bao;
			// 通知没听牌的玩家，换宝了，并告诉上一只宝牌的内容
			pushBaoChangeSyn(gameData, desk, old_bao, gameData.mPublic.mBaoCard, (int) (System.nanoTime() % 6) + 1);

			gameData.setWaitingStartTime(System.currentTimeMillis());
			gameData.setPlaySubstate(MJConstants.GAME_TABLE_SUB_STATE_SHOW_CHANGE_BAO);

			plist = gt.getTIngPls();
			for (PlayerInfo pl : plist) {
				if (checkPlayerHuAfterTing(gt, desk, pl)) {
					return true;
				}
			}
			gt.setBaoChangeNum(gt.getBaoChangeNum() + 1);
		}
		return false;
	}

	private void pushBaoChangeSyn(GameData data, DQMJDesk desk, int old_bao, int new_bao, int dice) {
		GameOperBaoChangeSyn.Builder msg = GameOperBaoChangeSyn.newBuilder();
		msg.setOldBao(old_bao);
		msg.setDice(dice);
		msg.setPosition(data.playerChangeBao);
		PokerPushHelper.pushBaoChange(desk, -100, msg, MJConstants.SEND_TYPE_ALL);
		
		gameData.recorder.recordPlayerAction(gameData.genSeq(), -1, MJConstants.MAHJONG_OPERTAION_CHNAGE_BAO, new_bao, old_bao,
				MJHelper.getActionName(MJConstants.MAHJONG_OPERTAION_CHNAGE_BAO) + ":" + MJHelper.getSingleCardName(new_bao) + ",旧宝:"
						+ MJHelper.getSingleCardName(old_bao), 0);
	}
	
	@Override
	public void playerOperation(GameData gameData, DQMJDesk gt, GameOperPlayerActionSyn.Builder msg, PlayerInfo pl) {
		if (msg == null || pl == null)
			return;

		logger.info("act=playerOperation;code={};position={};deskId={};", msg.getAction(), pl.position, gt.getDeskID());

		// 玩家出牌
		if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CHU) == MJConstants.MAHJONG_OPERTAION_CHU) {
			player_op_chu(gameData, gt, msg, pl);
		}

		// 玩家吃牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CHI) == MJConstants.MAHJONG_OPERTAION_CHI
				|| (msg.getAction() & MJConstants.MAHJONG_OPERTAION_CHI_TING) == MJConstants.MAHJONG_OPERTAION_CHI_TING) {
			player_op_chi(gameData, gt, msg, pl);
		}

		// 玩家碰牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_PENG) == MJConstants.MAHJONG_OPERTAION_PENG
				|| (msg.getAction() & MJConstants.MAHJONG_OPERTAION_PENG_TING) == MJConstants.MAHJONG_OPERTAION_PENG_TING) {
			player_op_peng(gameData, gt, msg, pl);
		}

		// 玩家取消吃碰牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_CANCEL) == MJConstants.MAHJONG_OPERTAION_CANCEL) {
			player_cancel(gameData, gt, msg, pl);
		}

		// 玩家听牌
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_TING) == MJConstants.MAHJONG_OPERTAION_TING) {
			player_op_ting(gameData, gt, msg, pl);
		}
		// 玩家支对
		else if (gt.canZhiDuiHU() && (msg.getAction() & MJConstants.MAHJONG_OPERTAION_ZD_TING) == MJConstants.MAHJONG_OPERTAION_ZD_TING) {
			player_op_danzhidui(gameData, gt, msg, pl);
		}
		
		// 玩家甩九幺
		else if ((msg.getAction() & MJConstants.MAHJONG_OPERTAION_SHUAIJIUYAO) == MJConstants.MAHJONG_OPERTAION_SHUAIJIUYAO) {
			player_op_shuaijiuyao(gameData, gt, msg, pl);
		}

		else
			throw new RuntimeException("UnKnowOperation;");
	}

	@Override
	public void re_notify_current_operation_player(GameData gameData, DQMJDesk desk, int position) {
		ActionWaitingModel waiting = gameData.getWaitingPlayerOperate();
		if (waiting != null) {
			PlayerInfo plx = desk.getDeskPlayer(waiting.playerTableIndex);
			if (plx != null && plx.position == position) {
				notifyPlayerWaitingOperation(gameData, desk, plx, waiting);
			}
		} else if (gameData.getCurrentOpertaionPlayerIndex() == position) {
			this.player_chu_notify(gameData, desk);
		}

		if (isEnd(gameData))// 流局了
		{
			liuju(gameData, desk);
		}
	}

	public void autoPlay(GameData gt, DQMJDesk desk, PlayerInfo pl, ActionWaitingModel waiting) {
		GameOperPlayerActionSyn.Builder msg = GameOperPlayerActionSyn.newBuilder();
		msg.setPosition(pl.position);
		// 自动吃碰听
		if (waiting != null) {
			// 碰听
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG_TING) == MJConstants.MAHJONG_OPERTAION_PENG_TING) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_PENG_TING);
				msg.addCardValue(waiting.peng_card_value);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 碰
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_PENG) == MJConstants.MAHJONG_OPERTAION_PENG) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_PENG);
				msg.addCardValue(waiting.peng_card_value);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 吃
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI_TING) == MJConstants.MAHJONG_OPERTAION_CHI_TING) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_CHI_TING);
				CardCombo combo = waiting.chiCombos.get(0);
				msg.addCardValue(combo.card1 & 0xFF);
				msg.addCardValue(combo.card2 & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 吃
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHI) == MJConstants.MAHJONG_OPERTAION_CHI) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_CHI);
				CardCombo combo = waiting.chiCombos.get(0);
				msg.addCardValue(combo.card1 & 0xFF);
				msg.addCardValue(combo.card2 & 0xFF);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 听
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_TING) == MJConstants.MAHJONG_OPERTAION_TING) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_TING);
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 支对
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_ZD_TING) == MJConstants.MAHJONG_OPERTAION_ZD_TING) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_ZD_TING);
				msg.addCardValue(waiting.zhiDuiTingList.get(0));
				this.playerOperation(gt, desk, msg, pl);
				return;
			}

			// 出
			if ((waiting.opertaion & MJConstants.MAHJONG_OPERTAION_CHU) == MJConstants.MAHJONG_OPERTAION_CHU) {
				msg.setAction(MJConstants.MAHJONG_OPERTAION_CHU);
				if (waiting.tingList.size() > 0) {
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
	public void handleSetGamingData(GameCardDealer mCardDealer, GameData gameData, DQMJDesk desk, String json) {
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
		logger.info("玩家1手牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[0].cardsInHand));
		logger.info("玩家1吃碰杠:" + MJHelper.getCompositeCardListName(gameData.mPlayerCards[0].cardsDown));
		logger.info("玩家1打出的牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[0].cardsBefore));

		logger.info("玩家2手牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[1].cardsInHand));
		logger.info("玩家2吃碰杠:" + MJHelper.getCompositeCardListName(gameData.mPlayerCards[1].cardsDown));
		logger.info("玩家2打出的牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[1].cardsBefore));

		logger.info("玩家3手牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[2].cardsInHand));
		logger.info("玩家3吃碰杠:" + MJHelper.getCompositeCardListName(gameData.mPlayerCards[2].cardsDown));
		logger.info("玩家3打出的牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[2].cardsBefore));

		logger.info("玩家4手牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[3].cardsInHand));
		logger.info("玩家4吃碰杠:" + MJHelper.getCompositeCardListName(gameData.mPlayerCards[3].cardsDown));
		logger.info("玩家4打出的牌:" + MJHelper.getSingleCardListName(gameData.mPlayerCards[3].cardsBefore));
	}

	private void fillOtherCard(GameData gameData, int pos) {
		List<Byte> cardsInHand = gameData.mPlayerCards[pos].cardsInHand;
		int cardsdownSize = gameData.mPlayerCards[pos].cardsDown.size() * 3;
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
	public void repushGameData(GameData gameData, DQMJDesk desk, int position) {
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
