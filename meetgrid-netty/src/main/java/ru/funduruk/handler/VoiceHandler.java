package ru.funduruk.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import ru.funduruk.Entity.VoiceSession;
import ru.funduruk.Service.VoiceSessionService;
import ru.funduruk.model.VoicePacket;

@AllArgsConstructor
public class VoiceHandler extends SimpleChannelInboundHandler<VoicePacket> {

    private final VoiceSessionService sessionService;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VoicePacket packet) {
        VoiceSession session = VoiceSessionService.getSession(packet.getSessionId());
        if (session == null) return;


        session.getParticipants().stream()
                .filter(p -> !p.getUserId().equals(packet.getUserId()))
                .forEach(p -> {
                    if (p.getChannel() != null && p.getChannel().isActive()) {
                        p.getChannel().writeAndFlush(packet);
                    }
                });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // TODO add logic exit user in all session
        super.channelInactive(ctx);
    }
}

