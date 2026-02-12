package com.dev.napolme.controller.stat;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.stat.PopularStatItemDto;
import com.dev.napolme.dto.stat.PopularStatResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stat")
public class StatController {

    @GetMapping("/popular")
    public ApiResponse<PopularStatResponse> getPopular() {
        List<PopularStatItemDto> items = List.of();
        CachePolicyDto cache = new CachePolicyDto(false, true);
        PopularStatResponse response = new PopularStatResponse(
            "character",
            Instant.now(),
            items,
            cache
        );
        return ApiResponse.success("OK", response, cache.cacheHit(), 0);
    }
}
