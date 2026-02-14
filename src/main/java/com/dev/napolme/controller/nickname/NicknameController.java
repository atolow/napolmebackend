package com.dev.napolme.controller.nickname;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.nickname.NicknameGenerateRequest;
import com.dev.napolme.dto.nickname.NicknameGenerateResponse;
import com.dev.napolme.service.nickname.NicknameGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nickname")
public class NicknameController {
    private final NicknameGeneratorService nicknameGeneratorService;
    
    public NicknameController(NicknameGeneratorService nicknameGeneratorService) {
        this.nicknameGeneratorService = nicknameGeneratorService;
    }
    
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<NicknameGenerateResponse>> generate(
        @RequestBody NicknameGenerateRequest request
    ) {
        NicknameGenerateResponse response = nicknameGeneratorService.generate(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
