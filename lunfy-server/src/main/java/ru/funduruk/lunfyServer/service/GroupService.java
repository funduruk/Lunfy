package ru.funduruk.lunfyServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.lunfyServer.entity.*;
import ru.funduruk.lunfyServer.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final ChannelRepository channelRepository;
    private final GroupMemberRepository groupMemberRepository;

    public Group create(String name, User owner) {
        Group group = new Group();
        group.setName(name);
        group.setOwner(owner);
        group = groupRepository.save(group);

        // Создаём участника-владельца
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(owner);
        member.setRole(GroupMember.Role.ADMIN);
        groupMemberRepository.save(member);

        // Создаём дефолтный канал
        Channel general = new Channel();
        general.setName("general");
        general.setType(Channel.ChannelType.TEXT);
        general.setGroup(group);
        channelRepository.save(general);

        return group;
    }

    public Channel addChannel(Long groupId, String name, Channel.ChannelType type) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
        Channel channel = new Channel();
        channel.setName(name);
        channel.setType(type);
        channel.setGroup(group);
        return channelRepository.save(channel);
    }

    public void addMember(Long groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMember.Role.MEMBER);
        groupMemberRepository.save(member);
    }

    public List<GroupMember> getMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    public List<Channel> getChannels(Long groupId) {
        return channelRepository.findByGroupId(groupId);
    }

    public List<GroupMember> getMembersByUser(Long userId) {
        return groupMemberRepository.findByUserId(userId);
    }
}