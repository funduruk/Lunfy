package ru.funduruk.lunfyServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.funduruk.lunfyServer.entity.*;
import ru.funduruk.lunfyServer.service.GroupService;
import ru.funduruk.lunfyServer.service.UserService;

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
}