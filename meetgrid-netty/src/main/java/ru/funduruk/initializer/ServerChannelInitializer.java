package ru.funduruk.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ru.funduruk.handler.JsonMessageHandler;

import java.nio.charset.StandardCharsets;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        p.addLast(new LineBasedFrameDecoder(8192));

        p.addLast(new StringDecoder(StandardCharsets.UTF_8));

        p.addLast(new StringEncoder(StandardCharsets.UTF_8));

        p.addLast(new JsonMessageHandler());
    }
}

