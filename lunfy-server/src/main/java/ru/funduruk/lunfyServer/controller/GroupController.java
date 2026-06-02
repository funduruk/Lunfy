package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.funduruk.dto.EnvelopeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.*;
import ru.funduruk.lunfyServer.service.GroupService;
import ru.funduruk.lunfyServer.service.UserService;
import ru.funduruk.lunfyServer.ws.ChatWebSocketHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String username,
                                    @RequestBody Map<String, Object> body) {
        try {
            User owner = userService.findByUsername(username);
            String name = (String) body.get("name");
            Group group = groupService.create(name, owner);

            // Добавляем дополнительные каналы если переданы
            List<String> textChannels = (List<String>) body.getOrDefault("textChannels", List.of());
            List<String> voiceChannels = (List<String>) body.getOrDefault("voiceChannels", List.of());

            for (String ch : textChannels) {
                if (!ch.equals("general")) { // general уже создан в GroupService
                    groupService.addChannel(group.getId(), ch, Channel.ChannelType.TEXT);
                }
            }
            for (String ch : voiceChannels) {
                groupService.addChannel(group.getId(), ch, Channel.ChannelType.VOICE);
            }

            return ResponseEntity.ok(Map.of(
                    "id", group.getId(),
                    "name", group.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyGroups(@AuthenticationPrincipal String username) {
        User user = userService.findByUsername(username);
        List<GroupMember> memberships = groupService.getMembersByUser(user.getId());

        List<Map<String, Object>> result = memberships.stream()
                .map(m -> {
                    Group g = m.getGroup();
                    List<Map<String, Object>> channels = groupService.getChannels(g.getId())
                            .stream()
                            .map(ch -> Map.<String, Object>of(
                                    "id", ch.getId(),
                                    "name", ch.getName(),
                                    "type", ch.getType().name()
                            ))
                            .collect(Collectors.toList());

                    return Map.<String, Object>of(
                            "id", g.getId(),
                            "name", g.getName(),
                            "role", m.getRole().name(),
                            "channels", channels
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long groupId,
                                       @RequestBody Map<String, String> body) {
        try {
            User user = userService.findByUsernameAndTag(
                    body.get("username"), body.get("tag")
            );
            groupService.addMember(groupId, user);
            return ResponseEntity.ok(Map.of("message", "Участник добавлен"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getMembers(groupId);
        List<Map<String, Object>> result = members.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "username", m.getUser().getUsername(),
                        "tag", m.getUser().getTag(),
                        "role", m.getRole().name()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{groupId}/channels")
    public ResponseEntity<?> addChannel(@PathVariable Long groupId,
                                        @RequestBody Map<String, String> body) {
        try {
            Channel.ChannelType type = Channel.ChannelType.valueOf(body.get("type"));
            Channel channel = groupService.addChannel(groupId, body.get("name"), type);
            return ResponseEntity.ok(Map.of(
                    "id", channel.getId(),
                    "name", channel.getName(),
                    "type", channel.getType().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/channels")
    public ResponseEntity<?> getChannels(@PathVariable Long groupId) {
        List<Channel> channels = groupService.getChannels(groupId);
        List<Map<String, Object>> result = channels.stream()
                .map(ch -> Map.<String, Object>of(
                        "id", ch.getId(),
                        "name", ch.getName(),
                        "type", ch.getType().name()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{groupId}/members/{username}/kick")
    public ResponseEntity<?> kick(@PathVariable Long groupId,
                                  @PathVariable String username,
                                  @AuthenticationPrincipal String currentUser) {
        try {
            groupService.kickMember(groupId, username);

            // Уведомляем всех участников группы через WebSocket
            notifyGroupMembers(groupId, "GROUP_MEMBER_KICKED",
                    Map.of("groupId", groupId, "username", username));

            return ResponseEntity.ok(Map.of("message", "Участник исключён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members/{username}/role")
    public ResponseEntity<?> setRole(@PathVariable Long groupId,
                                     @PathVariable String username,
                                     @RequestBody Map<String, String> body) {
        try {
            groupService.setRole(groupId, username, GroupMember.Role.valueOf(body.get("role")));

            notifyGroupMembers(groupId, "GROUP_ROLE_CHANGED",
                    Map.of("groupId", groupId, "username", username, "role", body.get("role")));

            return ResponseEntity.ok(Map.of("message", "Роль обновлена"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    private void notifyGroupMembers(Long groupId, String type, Map<String, Object> data) {
        try {
            List<GroupMember> members = groupService.getMembers(groupId);
            EnvelopeDTO env = new EnvelopeDTO(type, data);
            for (GroupMember m : members) {
                chatWebSocketHandler.sendToUserByUsername(m.getUser().getUsername(), env);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PutMapping("/{groupId}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable Long groupId,
                                            @RequestBody Map<String, String> body) {
        try {
            Group group = groupService.findById(groupId);
            if (body.containsKey("name")) group.setName(body.get("name"));
            groupService.save(group);
            return ResponseEntity.ok(Map.of("message", "Обновлено"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId) {
        try {
            groupService.delete(groupId);
            return ResponseEntity.ok(Map.of("message", "Группа удалена"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/avatar")
    public ResponseEntity<?> uploadAvatar(@PathVariable Long groupId,
                                          @RequestParam("file")
                                          org.springframework.web.multipart.MultipartFile file) {
        try {
            String fileName = groupId + "_" + System.currentTimeMillis() +
                    getExtension(file.getOriginalFilename());
            Path path = Paths.get("uploads/groups/" + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            Group group = groupService.findById(groupId);
            group.setAvatarPath(path.toString());
            groupService.save(group);

            return ResponseEntity.ok(Map.of("avatarPath", path.toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".png";
    }


}