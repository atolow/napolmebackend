package com.dev.napolme.dto.nickname;

import java.util.List;

public record NicknameGenerateResponse(
    List<GeneratedNickname> nicknames
) {}
