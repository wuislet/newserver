package com.buding.common.network.codec;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
	private List<ChannelHandler> handlers;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new IdleStateHandler(180, 0, 0));
		pipeline.addLast(new Netty4Codec());
		for (ChannelHandler chr : handlers) {
			pipeline.addLast(chr);
		}
	}

	public List<ChannelHandler> getHandlers() {
		return handlers;
	}

	public void setHandlers(List<ChannelHandler> handlers) {
		this.handlers = handlers;
	}

}
