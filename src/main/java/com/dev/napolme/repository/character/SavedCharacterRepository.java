package com.dev.napolme.repository.character;

import com.dev.napolme.domain.character.SavedCharacter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCharacterRepository extends JpaRepository<SavedCharacter, Long> {

    Optional<SavedCharacter> findByServerIdAndCharacterId(String serverId, String characterId);

    List<SavedCharacter> findByNicknameContainingIgnoreCase(String nickname);
}
