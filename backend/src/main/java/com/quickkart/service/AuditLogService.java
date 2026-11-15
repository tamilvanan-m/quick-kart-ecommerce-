package com.quickkart.service;

import com.quickkart.dto.AdminDtos;
import com.quickkart.entity.AuditLog;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.AuditLogRepository;
import com.quickkart.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EntityMapper mapper;

    public AuditLogService(AuditLogRepository auditLogRepository, EntityMapper mapper) {
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
    }

    @Transactional
    public void log(String action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            entry.setUserId(principal.getId());
            entry.setUserEmail(principal.getEmail());
            entry.setUserRole(principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(",")));
        }
        auditLogRepository.save(entry);
    }

    public Page<AdminDtos.AuditLogDto> getLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(mapper::toAuditLogDto);
    }
}
