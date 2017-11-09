package com.buding.mj.common;

import java.util.List;


public interface MJRule {
	public boolean canHu(List<Byte> list, int guiCard);
	public boolean canHu(List<Byte> list, MjCheckResult result);
	public boolean canChengPai(List<Byte> list);
	public boolean canChengPai(List<Byte> list, MjCheckResult result);
}
