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

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(owner);
        member.setRole(GroupMember.Role.ADMIN);
        groupMemberRepository.save(member);

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

    public void setRole(Long groupId, String username, GroupMember.Role role) {
        groupMemberRepository.findByGroupId(groupId).stream()
                .filter(m -> m.getUser().getUsername().equals(username))
                .findFirst()
                .ifPresent(m -> {
                    m.setRole(role);
                    groupMemberRepository.save(m);
                });
    }

    public void kickMember(Long groupId, String username) {
        groupMemberRepository.findByGroupId(groupId).stream()
                .filter(m -> m.getUser().getUsername().equals(username))
                .findFirst()
                .ifPresent(groupMemberRepository::delete);
    }

    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }

    public Group save(Group group) {
        return groupRepository.save(group);
    }

    public void delete(Long groupId) {
        groupRepository.deleteById(groupId);
    }

    private final GroupInviteRepository inviteRepository;

    public GroupInvite createInvite(Long groupId, User invitedUser, User invitedBy) {

        boolean alreadyMember = getMembers(groupId).stream()
                .anyMatch(m -> m.getUser().getId().equals(invitedUser.getId()));
        if (alreadyMember) {
            throw new RuntimeException("Пользователь уже в сообществе");
        }

        if (inviteRepository.findByGroupIdAndInvitedUserIdAndStatus(
                groupId, invitedUser.getId(), GroupInvite.Status.PENDING).isPresent()) {
            throw new RuntimeException("Приглашение уже отправлено");
        }

        Group group = findById(groupId);
        GroupInvite invite = new GroupInvite();
        invite.setGroup(group);
        invite.setInvitedUser(invitedUser);
        invite.setInvitedBy(invitedBy);
        invite.setStatus(GroupInvite.Status.PENDING);
        return inviteRepository.save(invite);
    }

    public List<GroupInvite> getPendingInvites(Long userId) {
        return inviteRepository.findByInvitedUserIdAndStatus(userId, GroupInvite.Status.PENDING);
    }

    public GroupInvite getInvite(Long inviteId) {
        return inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Приглашение не найдено"));
    }

    public Group acceptInvite(Long inviteId, User user) {
        GroupInvite invite = getInvite(inviteId);
        if (!invite.getInvitedUser().getId().equals(user.getId())) {
            throw new RuntimeException("Это не ваше приглашение");
        }
        invite.setStatus(GroupInvite.Status.ACCEPTED);
        inviteRepository.save(invite);
        addMember(invite.getGroup().getId(), user); // существующий метод
        return invite.getGroup();
    }

    public void declineInvite(Long inviteId, User user) {
        GroupInvite invite = getInvite(inviteId);
        if (!invite.getInvitedUser().getId().equals(user.getId())) {
            throw new RuntimeException("Это не ваше приглашение");
        }
        invite.setStatus(GroupInvite.Status.DECLINED);
        inviteRepository.save(invite);
    }

    public void deleteChannel(Long channelId) {
        channelRepository.deleteById(channelId);
    }
}