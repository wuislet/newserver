package com.buding.game;

import java.util.ArrayList;
import java.util.List;

import com.buding.mj.model.ShouPaoModel;

public class JCMJGameData extends GameData { //TODO WXD 用子类进一步区分不同麻将的GameData里的特殊数据。
	public JCMJGameData() {
		super();
		this.Reset();
	}

	public void Reset() {
		for(int index = 0; index < GameConstants.MyGame_Max_Players_Count; index ++){
			shouPaoData[index] = new ShouPaoModel();
		}
		super.Reset();
	}
	
	public ShouPaoModel[] shouPaoData = new ShouPaoModel[GameConstants.MyGame_Max_Players_Count];
}
