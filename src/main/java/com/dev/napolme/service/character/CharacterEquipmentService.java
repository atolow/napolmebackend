package com.dev.napolme.service.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse.EquipmentItem;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse.PetInfo;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse.SkillItem;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse.WingInfo;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterEquipmentSkillResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CharacterEquipmentService {
    private final PlayNcClient playNcClient;

    public CharacterEquipmentService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public CharacterEquipmentSkillResponse fetchEquipmentAndSkills(
        String serverId,
        String characterId,
        String lang
    ) {
        PlayNcCharacterEquipmentSkillResponse raw =
            playNcClient.fetchCharacterEquipmentSkill(serverId, characterId, lang);

        CachePolicyDto cache = new CachePolicyDto(false, true);
        if (raw == null) {
            return new CharacterEquipmentSkillResponse(
                List.of(),
                List.of(),
                null,
                null,
                null,
                List.of(),
                Instant.now(),
                cache
            );
        }

        return new CharacterEquipmentSkillResponse(
            mapEquipmentList(raw.equipment() == null ? null : raw.equipment().equipmentList()),
            mapEquipmentList(raw.equipment() == null ? null : raw.equipment().skinList()),
            mapPet(raw.petwing() == null ? null : raw.petwing().pet()),
            mapWing(raw.petwing() == null ? null : raw.petwing().wing()),
            mapWing(raw.petwing() == null ? null : raw.petwing().wingSkin()),
            mapSkillList(raw.skill() == null ? null : raw.skill().skillList()),
            Instant.now(),
            cache
        );
    }

    private List<EquipmentItem> mapEquipmentList(
        List<PlayNcCharacterEquipmentSkillResponse.EquipmentItem> list
    ) {
        if (list == null) {
            return List.of();
        }
        List<EquipmentItem> mapped = new ArrayList<>();
        for (PlayNcCharacterEquipmentSkillResponse.EquipmentItem item : list) {
            if (item == null) {
                continue;
            }
            mapped.add(new EquipmentItem(
                item.id(),
                item.name(),
                item.enchantLevel(),
                item.exceedLevel(),
                item.grade(),
                item.slotPos(),
                item.slotPosName(),
                item.icon()
            ));
        }
        return mapped;
    }

    private PetInfo mapPet(PlayNcCharacterEquipmentSkillResponse.Pet pet) {
        if (pet == null) {
            return null;
        }
        return new PetInfo(pet.id(), pet.name(), pet.level(), pet.icon());
    }

    private WingInfo mapWing(PlayNcCharacterEquipmentSkillResponse.Wing wing) {
        if (wing == null) {
            return null;
        }
        return new WingInfo(wing.id(), wing.name(), wing.enchantLevel(), wing.grade(), wing.icon());
    }

    private List<SkillItem> mapSkillList(
        List<PlayNcCharacterEquipmentSkillResponse.SkillItem> list
    ) {
        if (list == null) {
            return List.of();
        }
        List<SkillItem> mapped = new ArrayList<>();
        for (PlayNcCharacterEquipmentSkillResponse.SkillItem item : list) {
            if (item == null) {
                continue;
            }
            mapped.add(new SkillItem(
                item.id(),
                item.name(),
                item.needLevel(),
                item.skillLevel(),
                item.icon(),
                item.category(),
                item.acquired(),
                item.equip()
            ));
        }
        return mapped;
    }
}
