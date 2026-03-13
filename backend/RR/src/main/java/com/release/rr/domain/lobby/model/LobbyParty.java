package com.release.rr.domain.lobby.model;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class LobbyParty {

    private final Long partyId;        // 파티 고유 ID
    private Long hostId;               // 방장 ID
    private final Set<Long> memberIds;
    private String roomId;       // 🔥 DB map.nanoId


    private LobbyParty(Long hostId, String roomId) {
        this.partyId = hostId;
        this.hostId = hostId;
        this.roomId = roomId;
        this.memberIds = new LinkedHashSet<>();
        this.memberIds.add(hostId);
    }


    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
    public static LobbyParty create(Long hostId, String roomId) {
        return new LobbyParty(hostId, roomId);
    }

    public boolean isFull() { return memberIds.size() >= 4; }
    public void addMember(Long userId) { if (!isFull()) memberIds.add(userId); }
    public void removeMember(Long userId) { memberIds.remove(userId); }
}

