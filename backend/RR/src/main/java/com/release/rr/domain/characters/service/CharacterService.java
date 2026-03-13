package com.release.rr.domain.characters.service;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import com.release.rr.domain.characters.dto.UserCharacterDto;
import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.factory.CharacterStatsFactory;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.characters.stat.CharacterStatSnapshot;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterStateRedisDao characterStateRedisDao;

    private final UserRepository userRepository;
    private final MapRepository mapRepository;

    // ==========================================================
    // 🔥 캐릭터 생성
    // ==========================================================
    @Transactional
    public CharacterEntity createCharacter(Long userId, CharacterEntity.Job job) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 캐릭터 존재하는지 체크
        if (characterRepository.existsByUser(user)) {
            throw new CustomException(ErrorCode.CHARACTER_ALREADY_EXISTS);
        }

        // 기본 맵: 1번
        MapEntity map = mapRepository.findByOwner(user)
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        String statusJson = CharacterStatsFactory.defaultStats(job);

        // status → snapshot
        CharacterStatSnapshot stats =
                CharacterStatSnapshot.fromStatusJson(statusJson);

        CharacterEntity character = CharacterEntity.builder()
                .user(user)
                .job(job)
                .map(map)
                .level(0)
                .exp(0)
                .status(statusJson)
                .maxHp(stats.getMaxHp())
                .hp(stats.getMaxHp()) // ✅ 처음엔 풀피
                .posX(0f)
                .posY(0f)
                .isEnd(false)
                .build();


        return characterRepository.save(character);
    }


    // ==========================================================
    // 🔥 캐릭터 상태 초기화
    // ==========================================================
    @Transactional(readOnly = true)
    public void resetCharacter(Long characterId) {

        CharacterEntity entity = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        CharacterStatSnapshot stats = entity.getStatSnapshot();

        CharacterStateDto init = CharacterStateDto.builder()
                .characterId(entity.getId())
                .userId(entity.getUser().getUserId())

                .x(entity.getPosX())
                .y(entity.getPosY())
                .angle(entity.getAngle())

                .hp(entity.getHp())
                .maxHp(stats.getMaxHp())
                .attack(stats.getAttack())

                .strength(stats.getStrength())
                .agility(stats.getAgility())
                .health(stats.getHealth())
                .reload(stats.getReload())

                .level(entity.getLevel())
                .exp(entity.getExp())
                .pendingStatPoints(stats.getPendingPoints())
                .connected(true)
                .build();

        characterStateRedisDao.saveState(entity.getId(), init);

    }


    // ==========================================================
    // 🔥 Redis → DB 저장
    // ==========================================================
    @Transactional
    public void saveCharacterStateToDB(Long characterId) {

        CharacterStateDto state = characterStateRedisDao.getState(characterId);

        CharacterEntity entity = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        entity.setLevel(state.getLevel());
        entity.setExp(state.getExp());
        entity.setHp(state.getHp());
        entity.setPosX(state.getX());
        entity.setPosY(state.getY());
        
        // 모든 스탯 및 포인트 JSON 저장 (status 컬럼 활용)
        String statusJson = com.release.rr.domain.characters.mapper.CharacterStatMapper.toStatusJson(
            state.getStrength(), state.getAgility(), state.getHealth(), state.getReload(), state.getPendingStatPoints()
        );
        entity.setStatus(statusJson);

        characterRepository.save(entity);
    }



    // ==========================================================
    // 🔥 캐릭터 삭제
    // ==========================================================
    @Transactional
    public void deleteCharacter(Long characterId, Long userId) {

        CharacterEntity entity = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        // 본인 캐릭터인지 확인
        if (!entity.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        characterRepository.delete(entity);
    }


    // ==========================================================
    // 🔥 유저 캐릭터 조회
    // ==========================================================
    public UserCharacterDto getUserCharacter(Long userId) {

        // 1) 유저 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2) 유저 캐릭터 조회
        CharacterEntity character = characterRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        // 3) 캐릭터 DTO 변환
        CharacterResponseDto characterDto = CharacterResponseDto.from(character);

        // 4) 유저 + 캐릭터 정보 DTO로 묶어서 반환
        return UserCharacterDto.of(user.getNickname(), characterDto);
    }


    // ==========================================================
    // 🔥 유저 ID 기반 캐릭터 조회 (대기실/파티용)
    // ==========================================================
        @Transactional(readOnly = true)
        public CharacterEntity getCharacterByUserId(Long userId) {

            return characterRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
        }


    public void recalcDerivedStats(CharacterEntity character) {

        CharacterStatSnapshot stats = character.getStatSnapshot();

        character.setMaxHp(stats.getMaxHp());

        // hp 보정 (maxHp 줄어들었을 경우)
        if (character.getHp() > character.getMaxHp()) {
            character.setHp(character.getMaxHp());
        }
    }



}
