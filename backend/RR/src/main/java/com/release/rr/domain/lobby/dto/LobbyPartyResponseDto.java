package com.release.rr.domain.lobby.dto;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;
// 파티 전체 정보를 리턴
@Data
@Builder
public class LobbyPartyResponseDto {
    private Long partyId;
    private Long hostId;
    private String roomId; //
    private CharacterResponseDto myCharacter;
    private List<CharacterResponseDto> members;
    private String keySetting;
    private int stage;


}
