package com.buding.common.network.codec;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;

import java.util.List;

/**
 * @author tiny qq_381360993
 * @Description:
 * 
 */
public class NettyHttpServerInitializer extends ChannelInitializer<SocketChannel> {
	private List<ChannelHandler> handlers;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new HttpRequestDecoder());
		pipeline.addLast(new HttpObjectAggregator(65535));
		pipeline.addLast(new HttpResponseEncoder());
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
