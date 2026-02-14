package com.dev.napolme.service.board;

import com.dev.napolme.domain.board.NapolmeUpdate;
import com.dev.napolme.dto.board.NapolmeUpdateItemDto;
import com.dev.napolme.dto.board.NapolmeUpdatesResponse;
import com.dev.napolme.repository.board.NapolmeUpdateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NapolmeUpdateService {

    private static final String ALLOWED_WRITE_IP = "1.236.123.32";

    private final NapolmeUpdateRepository napolmeUpdateRepository;

    public NapolmeUpdateService(NapolmeUpdateRepository napolmeUpdateRepository) {
        this.napolmeUpdateRepository = napolmeUpdateRepository;
    }

    public NapolmeUpdatesResponse getList(String clientIp, boolean includeSeenIp) {
        String ip = clientIp != null ? clientIp.trim() : "";
        boolean allowWrite = ALLOWED_WRITE_IP.equals(ip);
        List<NapolmeUpdate> list = napolmeUpdateRepository.findAllByOrderByCreatedAtDesc();
        List<NapolmeUpdateItemDto> items = list.stream()
            .map(e -> new NapolmeUpdateItemDto(
                e.getId(),
                e.getTitle(),
                e.getContent(),
                e.getCreatedAt()
            ))
            .toList();
        return new NapolmeUpdatesResponse(items, allowWrite, includeSeenIp ? ip : null);
    }

    @Transactional
    public NapolmeUpdateItemDto create(String clientIp, String title, String content) {
        if (!ALLOWED_WRITE_IP.equals(clientIp != null ? clientIp.trim() : "")) {
            throw new IllegalArgumentException("FORBIDDEN");
        }
        String safeTitle = title != null && !title.isBlank() ? title.trim() : "(제목 없음)";
        if (safeTitle.length() > 500) {
            safeTitle = safeTitle.substring(0, 500);
        }
        String safeContent = content != null ? content.trim() : "";
        NapolmeUpdate entity = new NapolmeUpdate(safeTitle, safeContent);
        entity = napolmeUpdateRepository.save(entity);
        return new NapolmeUpdateItemDto(
            entity.getId(),
            entity.getTitle(),
            entity.getContent(),
            entity.getCreatedAt()
        );
    }
}
