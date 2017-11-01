package com.buding.common.network.codec;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public interface NetWorkListener {

	public void msgRead(byte[] msg);

	public void channelActive(ChannelHandlerContext ctx) throws Exception;

	public void channelInactive(ChannelHandlerContext ctx) throws Exception;

	public void exceptionCaught(Throwable cause) throws Exception;
}
