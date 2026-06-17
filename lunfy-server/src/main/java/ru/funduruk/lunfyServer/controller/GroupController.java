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

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String username,
                                    @RequestBody Map<String, Object> body) {
        try {
            User owner = userService.findByUsername(username);
            String name = (String) body.get("name");
            Group group = groupService.create(name, owner);

            List<String> textChannels = (List<String>) body.getOrDefault("textChannels", List.of());
            List<String> voiceChannels = (List<String>) body.getOrDefault("voiceChannels", List.of());

            for (String ch : textChannels) {
                if (!ch.equals("general")) {
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

                    Map<String, Object> groupMap = new java.util.HashMap<>();
                    groupMap.put("id", g.getId());
                    groupMap.put("name", g.getName());
                    groupMap.put("role", m.getRole().name());
                    groupMap.put("channels", channels);
                    groupMap.put("type", g.getType() != null ? g.getType().name() : "COMMUNITY");
                    groupMap.put("avatarPath", g.getAvatarPath());
                    return groupMap;
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

            // Notify all members
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

    private final ChatWebSocketHandler chatWebSocketHandler;

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

    @PostMapping("/dm")
    public ResponseEntity<?> createDM(@AuthenticationPrincipal String username,
                                      @RequestBody Map<String, Object> body) {
        try {
            User owner = userService.findByUsername(username);
            String name = (String) body.get("name");
            List<String> memberUsernames = (List<String>) body.getOrDefault("members", List.of());

            Group group = new Group();
            group.setName(name);
            group.setOwner(owner);
            group.setType(Group.GroupType.DM);
            group = groupService.save(group);

            groupService.addMember(group.getId(), owner);

            for (String memberName : memberUsernames) {
                try {
                    User user = userService.findByUsername(memberName);
                    groupService.addMember(group.getId(), user);
                } catch (Exception ignored) {
                }
            }

            // notify all members
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", group.getId());
            data.put("name", group.getName());
            data.put("type", "DM");

            EnvelopeDTO env = new EnvelopeDTO("GROUP_DM_CREATED", data);
            for (String memberName : memberUsernames) {
                chatWebSocketHandler.sendToUserByUsername(memberName, env);
            }
            chatWebSocketHandler.sendToUserByUsername(username, env);

            return ResponseEntity.ok(Map.of(
                    "id", group.getId(),
                    "name", group.getName(),
                    "type", "DM"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupInfo(@PathVariable Long groupId) {
        try {
            Group g = groupService.findById(groupId);
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", g.getId());
            info.put("name", g.getName());
            info.put("ownerUsername", g.getOwner() != null ? g.getOwner().getUsername() : null);
            info.put("type", g.getType() != null ? g.getType().name() : "COMMUNITY");
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long groupId) {
        try {
            Group group = groupService.findById(groupId);
            if (group.getAvatarPath() == null) {
                return ResponseEntity.notFound().build();
            }

            java.nio.file.Path path = java.nio.file.Paths.get(group.getAvatarPath());
            if (!java.nio.file.Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            String contentType = "image/png";
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


}