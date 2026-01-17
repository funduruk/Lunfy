package ru.funduruk.meetgridServer.service.entity;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.meetgridServer.entity.Channel;
import ru.funduruk.meetgridServer.entity.Server;
import ru.funduruk.meetgridServer.entity.enums.ChannelType;
import ru.funduruk.meetgridServer.entity.repository.ChannelRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    public Channel createTextChannel(Channel channel, Server server) {
        Channel newChannel = new Channel();
        if(channelRepository.findByServerAndName(server, channel.getName()).isPresent()) {
            throw new IllegalArgumentException("Channel already exists");
        }
        newChannel.setName(channel.getName());
        newChannel.setType(ChannelType.TEXT);
        newChannel.setServer(server);
        return channelRepository.save(newChannel);
    }
    public List<Channel> getChannels(Server server) {
        return channelRepository.findByServer(server);
    }

    public void deleteChannel(Channel channel, Server server) {
        Optional<Channel> oldChannel = channelRepository.findByServerAndName(server, channel.getName());
       if(oldChannel.isPresent()) {
           Channel delChannel = oldChannel.get();
           channelRepository.delete(delChannel);
       } else {
           throw new IllegalArgumentException("Channel not found");
       }
    }

}
