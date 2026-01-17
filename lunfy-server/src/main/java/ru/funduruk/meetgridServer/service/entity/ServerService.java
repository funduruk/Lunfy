package ru.funduruk.meetgridServer.service.entity;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.funduruk.meetgridServer.entity.Server;
import ru.funduruk.meetgridServer.entity.ServerMember;
import ru.funduruk.meetgridServer.entity.User;
import ru.funduruk.meetgridServer.entity.repository.ServerMemberRepository;
import ru.funduruk.meetgridServer.entity.repository.ServerRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ServerService {
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;

    public Server createServer(Server server, User user) {
        Server newServer = new Server();
        newServer.setName(server.getName());
        newServer.setOwner(user);
        newServer = serverRepository.save(newServer);
        ServerMember serverMember = new ServerMember();
        serverMember.setServer(newServer);
        serverMember.setUser(user);
        serverMember.setRole("OWNER");
        serverMemberRepository.save(serverMember);
        return newServer;
    }

    public List<Server> getServersByOwner(User owner) {
        return serverRepository.findByOwner(owner);
    }


    public ServerMember addMemberInServer(Server server, User user) {
        Optional<ServerMember> existing = serverMemberRepository.findByServerAndUser(server, user);
        if(existing.isPresent()) {
            throw new IllegalArgumentException("User is already member of this server");
        }

        ServerMember serverMember = new ServerMember();
        serverMember.setUser(user);
        serverMember.setServer(server);
        return serverMemberRepository.save(serverMember);
    }

    public void removeMemberFromServer(Server server, User user) {
        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerAndUser(server, user);

        if(memberOpt.isPresent()) {
            serverMemberRepository.delete(memberOpt.get());
        } else {
            throw new IllegalArgumentException("User is not member of this server");
        }

    }

    public void removeServer(Server server) {
        if (!serverRepository.existsById(server.getId())) {
            throw new IllegalArgumentException("Server does not exist");
        }
        serverRepository.delete(server);
    }

    public ServerMember changeRoleFromUser(Server server, User user, String role) {
        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerAndUser(server, user);
        if(memberOpt.isPresent()) {
            ServerMember member = memberOpt.get();
            member.setRole(role);
            return serverMemberRepository.save(member);
        }else{
            throw new IllegalArgumentException("User is not member of this server");
        }
    }
}
