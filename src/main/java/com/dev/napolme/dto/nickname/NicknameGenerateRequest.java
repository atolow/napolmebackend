package com.dev.napolme.dto.nickname;

import java.util.List;

public record NicknameGenerateRequest(
    String mode, // "normal", "combination", "direct"
    Integer length,
    Boolean excludeFinalConsonant,
    Boolean excludeDoubleConsonant,
    Boolean excludeUType,
    Boolean excludeEType,
    String fixedCharacters,
    String excludedCharacters,
    String directInput,
    List<String> selectedInitials,
    List<String> selectedMedials,
    List<String> selectedFinals,
    String serverId
) {}
