package com.release.rr.global.debug;

import com.release.rr.domain.characters.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DbCheckTask implements CommandLineRunner {

    private final CharacterRepository characterRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("============== [DB CHECK] 캐릭터 정합성 점검 시작 ==============");
        
        // ✅ [MIGRATION] 기존 Redis 방 정보가 있다면 세트로 마이그레이션 (keys()는 여기서 한 번만 사용)
        Set<String> keys = redisTemplate.keys("game:room:*");
        if (keys != null && !keys.isEmpty()) {
            System.out.println("🔄 [REDIS] Found " + keys.size() + " existing room keys. Syncing to RR:ACTIVE_ROOMS...");
            for (String key : keys) {
                if (key.startsWith("game:room:") && !key.contains(":characters")) {
                    String nanoId = key.substring("game:room:".length());
                    redisTemplate.opsForSet().add("RR:ACTIVE_ROOMS", nanoId);
                }
            }
        }

        characterRepository.findAll().forEach(c -> {
            String nickname = (c.getUser() != null) ? c.getUser().getNickname() : "UNKNOWN";
            System.out.println(String.format(
                "Character ID: %d | Nickname: %s | Level: %d | Exp: %d | Status: %s",
                c.getId(),
                nickname,
                c.getLevel(),
                c.getExp(),
                c.getStatus()
            ));

            // 비정상 데이터 보정 (예: 레벨 0인데 경험치가 50 이상인 경우 -> 즉시 레벨업 방지)
            // 현재 설정상 레벨 0 -> 1 필요 경험치는 50임.
            if (c.getLevel() == 0 && c.getExp() >= 50) {
                System.out.println("⚠️ [CORRECTION] Character " + c.getId() + " (" + nickname + ") is Lv.0 but has Exp " + c.getExp() + ". Resetting Exp to 0 to prevent instant Lvup.");
                c.setExp(0);
                characterRepository.save(c);
            }
        });
        System.out.println("============== [DB CHECK] 캐릭터 정합성 점검 완료 ==============");
    }
}
