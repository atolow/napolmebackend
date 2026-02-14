package com.dev.napolme.service.nickname;

import com.dev.napolme.dto.nickname.GeneratedNickname;
import com.dev.napolme.dto.nickname.NicknameGenerateRequest;
import com.dev.napolme.dto.nickname.NicknameGenerateResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterSearchResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NicknameGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(NicknameGeneratorService.class);
    
    // 한국어 자모
    private static final char[] INITIAL_CONSONANTS = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    
    private static final char[] MEDIAL_VOWELS = {
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    
    private static final char[] FINAL_CONSONANTS = {
        'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    
    // 쌍자음 (초성)
    private static final Set<Character> DOUBLE_CONSONANTS = Set.of('ㄲ', 'ㄸ', 'ㅃ', 'ㅆ', 'ㅉ');
    
    // 자음군 (종성)
    private static final Set<Character> CONSONANT_CLUSTERS = Set.of('ㄳ', 'ㄵ', 'ㄶ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅄ');
    
    // ㅡ, ㅟ, ㅞ류 모음
    private static final Set<Character> U_TYPE_VOWELS = Set.of('ㅡ', 'ㅟ', 'ㅞ', 'ㅜ', 'ㅝ');
    
    // ㅔ, ㅐ류 모음
    private static final Set<Character> E_TYPE_VOWELS = Set.of('ㅔ', 'ㅐ', 'ㅖ', 'ㅒ');
    
    private final PlayNcClient playNcClient;
    private final Random random = new Random();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    public NicknameGeneratorService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }
    
    public NicknameGenerateResponse generate(NicknameGenerateRequest request) {
        List<String> generatedNames = new ArrayList<>();
        
        if ("direct".equals(request.mode())) {
            // 직접입력 모드
            if (request.directInput() != null && !request.directInput().isBlank()) {
                generatedNames.add(request.directInput());
            }
        } else if ("normal".equals(request.mode())) {
            // 일반모드: 랜덤 생성
            generatedNames = generateRandomNicknames(
                request.length(),
                request.excludeFinalConsonant(),
                request.excludeDoubleConsonant(),
                request.excludeUType(),
                request.excludeEType(),
                request.fixedCharacters(),
                request.excludedCharacters(),
                18 // 18개 생성
            );
        } else if ("combination".equals(request.mode())) {
            // 조합모드: 선택된 자모로 생성
            generatedNames = generateCombinationNicknames(
                request.length(),
                request.selectedInitials(),
                request.selectedMedials(),
                request.selectedFinals(),
                request.excludeFinalConsonant(),
                request.fixedCharacters(),
                request.excludedCharacters(),
                18
            );
        }
        
        // 캐릭터 존재 여부 확인
        List<GeneratedNickname> nicknames = checkNicknameAvailability(generatedNames, request.serverId());
        
        return new NicknameGenerateResponse(nicknames);
    }
    
    private List<String> generateRandomNicknames(
        int length,
        boolean excludeFinalConsonant,
        boolean excludeDoubleConsonant,
        boolean excludeUType,
        boolean excludeEType,
        String fixedCharacters,
        String excludedCharacters,
        int count
    ) {
        List<String> nicknames = new ArrayList<>();
        Set<String> used = new HashSet<>();
        Set<Character> excludedChars = new HashSet<>();
        Set<Character> fixedChars = new HashSet<>();
        
        // 제외할 글자 설정
        if (excludedCharacters != null && !excludedCharacters.isBlank()) {
            for (char c : excludedCharacters.toCharArray()) {
                excludedChars.add(c);
            }
        }
        
        // 고정할 글자 설정
        if (fixedCharacters != null && !fixedCharacters.isBlank()) {
            for (char c : fixedCharacters.toCharArray()) {
                fixedChars.add(c);
            }
        }
        
        int attempts = 0;
        int maxAttempts = count * 1000; // 최대 시도 횟수 증가
        
        while (nicknames.size() < count && attempts < maxAttempts) {
            attempts++;
            StringBuilder sb = new StringBuilder();
            boolean valid = true;
            
            // 랜덤으로 글자 생성
            for (int i = 0; i < length; i++) {
                char ch = generateRandomCharacterWithRetry(
                    excludeFinalConsonant,
                    excludeDoubleConsonant,
                    excludeUType,
                    excludeEType,
                    excludedChars,
                    50 // 최대 50번 재시도
                );
                
                if (ch == 0) {
                    valid = false;
                    break;
                }
                
                sb.append(ch);
            }
            
            if (!valid) {
                continue;
            }
            
            String nickname = sb.toString();
            
            // 길이가 맞는지 확인
            if (nickname.length() != length) {
                continue;
            }
            
            // 고정 글자가 모두 포함되어 있는지 확인
            if (!fixedChars.isEmpty()) {
                boolean allFixedIncluded = true;
                for (char fixedChar : fixedChars) {
                    if (nickname.indexOf(fixedChar) == -1) {
                        allFixedIncluded = false;
                        break;
                    }
                }
                if (!allFixedIncluded) {
                    continue;
                }
            }
            
            // 제외 글자가 포함되어 있지 않은지 확인
            boolean hasExcluded = false;
            for (char excludedChar : excludedChars) {
                if (nickname.indexOf(excludedChar) != -1) {
                    hasExcluded = true;
                    break;
                }
            }
            if (hasExcluded) {
                continue;
            }
            
            if (!used.contains(nickname)) {
                used.add(nickname);
                nicknames.add(nickname);
            }
        }
        
        return nicknames;
    }
    
    private List<String> generateCombinationNicknames(
        int length,
        List<String> selectedInitials,
        List<String> selectedMedials,
        List<String> selectedFinals,
        boolean excludeFinalConsonant,
        String fixedCharacters,
        String excludedCharacters,
        int count
    ) {
        List<String> nicknames = new ArrayList<>();
        Set<String> used = new HashSet<>();
        Set<Character> excludedChars = new HashSet<>();
        Set<Character> fixedChars = new HashSet<>();
        
        // 제외할 글자 설정
        if (excludedCharacters != null && !excludedCharacters.isBlank()) {
            for (char c : excludedCharacters.toCharArray()) {
                excludedChars.add(c);
            }
        }
        
        // 고정할 글자 설정
        if (fixedCharacters != null && !fixedCharacters.isBlank()) {
            for (char c : fixedCharacters.toCharArray()) {
                fixedChars.add(c);
            }
        }
        
        Set<Character> initials = selectedInitials != null 
            ? selectedInitials.stream().map(s -> s.charAt(0)).collect(Collectors.toSet())
            : Set.of();
        Set<Character> medials = selectedMedials != null
            ? selectedMedials.stream().map(s -> s.charAt(0)).collect(Collectors.toSet())
            : Set.of();
        Set<Character> finals = Set.of();
        // 받침 제외 옵션이 false이고 종성이 선택된 경우에만 종성 사용
        if (!excludeFinalConsonant && selectedFinals != null && !selectedFinals.isEmpty()) {
            finals = selectedFinals.stream().map(s -> s.charAt(0)).collect(Collectors.toSet());
        }
        
        int attempts = 0;
        int maxAttempts = count * 1000; // 최대 시도 횟수 증가
        
        while (nicknames.size() < count && attempts < maxAttempts) {
            attempts++;
            StringBuilder sb = new StringBuilder();
            boolean valid = true;
            
            // 랜덤으로 글자 생성
            for (int i = 0; i < length; i++) {
                char ch = generateCharacterFromSetsWithRetry(
                    initials,
                    medials,
                    finals,
                    excludedChars,
                    50 // 최대 50번 재시도
                );
                
                if (ch == 0) {
                    valid = false;
                    break;
                }
                
                sb.append(ch);
            }
            
            if (!valid) {
                continue;
            }
            
            String nickname = sb.toString();
            
            // 길이가 맞는지 확인
            if (nickname.length() != length) {
                continue;
            }
            
            // 고정 글자가 모두 포함되어 있는지 확인
            if (!fixedChars.isEmpty()) {
                boolean allFixedIncluded = true;
                for (char fixedChar : fixedChars) {
                    if (nickname.indexOf(fixedChar) == -1) {
                        allFixedIncluded = false;
                        break;
                    }
                }
                if (!allFixedIncluded) {
                    continue;
                }
            }
            
            // 제외 글자가 포함되어 있지 않은지 확인
            boolean hasExcluded = false;
            for (char excludedChar : excludedChars) {
                if (nickname.indexOf(excludedChar) != -1) {
                    hasExcluded = true;
                    break;
                }
            }
            if (hasExcluded) {
                continue;
            }
            
            if (!used.contains(nickname)) {
                used.add(nickname);
                nicknames.add(nickname);
            }
        }
        
        return nicknames;
    }
    
    private char generateRandomCharacter(
        boolean excludeFinalConsonant,
        boolean excludeDoubleConsonant,
        boolean excludeUType,
        boolean excludeEType,
        Set<Character> excludedChars
    ) {
        int initialIdx = random.nextInt(INITIAL_CONSONANTS.length);
        char initial = INITIAL_CONSONANTS[initialIdx];
        
        // 쌍자음 제외 체크
        if (excludeDoubleConsonant && DOUBLE_CONSONANTS.contains(initial)) {
            return 0; // 재시도 필요
        }
        
        int medialIdx = random.nextInt(MEDIAL_VOWELS.length);
        char medial = MEDIAL_VOWELS[medialIdx];
        
        // 모음 제외 체크
        if (excludeUType && U_TYPE_VOWELS.contains(medial)) {
            return 0;
        }
        if (excludeEType && E_TYPE_VOWELS.contains(medial)) {
            return 0;
        }
        
        char finalConsonant = 0;
        if (!excludeFinalConsonant) {
            int finalIdx = random.nextInt(FINAL_CONSONANTS.length + 1); // +1은 받침 없음
            if (finalIdx < FINAL_CONSONANTS.length) {
                finalConsonant = FINAL_CONSONANTS[finalIdx];
                // 자음군 제외 체크
                if (excludeDoubleConsonant && CONSONANT_CLUSTERS.contains(finalConsonant)) {
                    return 0;
                }
            }
        }
        
        // 한글 조합
        char ch = composeHangul(initial, medial, finalConsonant);
        
        // 제외할 글자 체크
        if (excludedChars.contains(ch)) {
            return 0;
        }
        
        return ch;
    }
    
    private char generateRandomCharacterWithRetry(
        boolean excludeFinalConsonant,
        boolean excludeDoubleConsonant,
        boolean excludeUType,
        boolean excludeEType,
        Set<Character> excludedChars,
        int maxRetries
    ) {
        for (int i = 0; i < maxRetries; i++) {
            char ch = generateRandomCharacter(
                excludeFinalConsonant,
                excludeDoubleConsonant,
                excludeUType,
                excludeEType,
                excludedChars
            );
            if (ch != 0) {
                return ch;
            }
        }
        return 0;
    }
    
    private char generateCharacterFromSets(
        Set<Character> initials,
        Set<Character> medials,
        Set<Character> finals,
        Set<Character> excludedChars
    ) {
        if (initials.isEmpty() || medials.isEmpty()) {
            return 0;
        }
        
        List<Character> initialList = new ArrayList<>(initials);
        List<Character> medialList = new ArrayList<>(medials);
        
        char initial = initialList.get(random.nextInt(initialList.size()));
        char medial = medialList.get(random.nextInt(medialList.size()));
        
        char finalConsonant = 0;
        if (!finals.isEmpty()) {
            List<Character> finalList = new ArrayList<>(finals);
            if (random.nextBoolean()) {
                finalConsonant = finalList.get(random.nextInt(finalList.size()));
            }
        }
        
        char ch = composeHangul(initial, medial, finalConsonant);
        
        if (excludedChars.contains(ch)) {
            return 0;
        }
        
        return ch;
    }
    
    private char generateCharacterFromSetsWithRetry(
        Set<Character> initials,
        Set<Character> medials,
        Set<Character> finals,
        Set<Character> excludedChars,
        int maxRetries
    ) {
        for (int i = 0; i < maxRetries; i++) {
            char ch = generateCharacterFromSets(initials, medials, finals, excludedChars);
            if (ch != 0) {
                return ch;
            }
        }
        return 0;
    }
    
    private char composeHangul(char initial, char medial, char finalConsonant) {
        // 초성 인덱스 찾기
        int initialIdx = -1;
        for (int i = 0; i < INITIAL_CONSONANTS.length; i++) {
            if (INITIAL_CONSONANTS[i] == initial) {
                initialIdx = i;
                break;
            }
        }
        if (initialIdx == -1) return 0;
        
        // 중성 인덱스 찾기
        int medialIdx = -1;
        for (int i = 0; i < MEDIAL_VOWELS.length; i++) {
            if (MEDIAL_VOWELS[i] == medial) {
                medialIdx = i;
                break;
            }
        }
        if (medialIdx == -1) return 0;
        
        // 종성 인덱스 찾기 (없으면 0)
        int finalIdx = 0;
        if (finalConsonant != 0) {
            for (int i = 0; i < FINAL_CONSONANTS.length; i++) {
                if (FINAL_CONSONANTS[i] == finalConsonant) {
                    finalIdx = i + 1; // 종성은 1부터 시작 (0은 받침 없음)
                    break;
                }
            }
        }
        
        // 한글 유니코드 계산: 0xAC00 + (초성 * 588) + (중성 * 28) + 종성
        return (char) (0xAC00 + initialIdx * 588 + medialIdx * 28 + finalIdx);
    }
    
    private List<GeneratedNickname> checkNicknameAvailability(List<String> nicknames, String serverId) {
        List<GeneratedNickname> results = new ArrayList<>();
        
        // 먼저 모든 닉네임을 "checking" 상태로 설정
        for (String nickname : nicknames) {
            results.add(new GeneratedNickname(nickname, "checking"));
        }
        
        // 비동기로 존재 여부 확인
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < nicknames.size(); i++) {
            final int index = i;
            final String nickname = nicknames.get(i);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    boolean exists = checkCharacterExists(nickname, serverId);
                    results.set(index, new GeneratedNickname(
                        nickname,
                        exists ? "unavailable" : "available"
                    ));
                } catch (Exception e) {
                    log.error("Error checking nickname availability: {}", nickname, e);
                    results.set(index, new GeneratedNickname(nickname, "unavailable"));
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 모든 확인이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return results;
    }
    
    private boolean checkCharacterExists(String nickname, String serverId) {
        try {
            PlayNcCharacterSearchResponse response = playNcClient.fetchCharacterSearch(
                nickname,
                null, // race
                serverId,
                1,
                10
            );
            
            if (response == null || response.list() == null) {
                return false;
            }
            
            // 정규화된 검색어
            String normalizedKeyword = normalizeKeyword(nickname);
            
            // 정확히 일치하는 닉네임이 있는지 확인
            for (PlayNcCharacterSearchResponse.SearchItem item : response.list()) {
                if (item == null || item.name() == null) {
                    continue;
                }
                String name = stripHtml(item.name());
                if (isExactMatch(name, normalizedKeyword)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.warn("Failed to check character existence for nickname: {}", nickname, e);
            return false; // 에러 시 존재하지 않는 것으로 간주
        }
    }
    
    private String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "");
    }
    
    private String normalizeKeyword(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
    
    private boolean isExactMatch(String name, String normalizedKeyword) {
        if (normalizedKeyword.isBlank()) {
            return false;
        }
        return normalizedKeyword.equals(name);
    }
}
