package com.quickkart.service;

import com.quickkart.dto.AdminDtos;
import com.quickkart.dto.AuthDtos;
import com.quickkart.dto.CatalogDtos;
import com.quickkart.entity.Role;
import com.quickkart.entity.User;
import com.quickkart.entity.enums.RoleName;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.OrderRepository;
import com.quickkart.repository.RoleRepository;
import com.quickkart.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrderRepository orderRepository,
            PasswordEncoder passwordEncoder,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<AdminDtos.CustomerSummaryDto> getCustomers(String q, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = (q != null && !q.isBlank())
                ? userRepository.searchByRoleAndQuery(RoleName.CUSTOMER, q.trim(), pr)
                : userRepository.findByRoleName(RoleName.CUSTOMER, pr);

        Page<AdminDtos.CustomerSummaryDto> dtoPage = users.map(u -> {
            long orderCount = orderRepository.countByUserId(u.getId());
            BigDecimal totalSpent = orderRepository.sumUserTotalSpent(u.getId());
            return new AdminDtos.CustomerSummaryDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getPhone(),
                    u.getActive(),
                    orderCount,
                    totalSpent != null ? totalSpent : BigDecimal.ZERO,
                    u.getCreatedAt()
            );
        });
        return mapper.toPageResponse(dtoPage);
    }

    @Transactional
    public void toggleCustomerStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
        boolean isOwner = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.OWNER);
        if (isOwner) {
            throw new BadRequestException("Owner account cannot be deactivated here.");
        }
        user.setActive(active);
        userRepository.save(user);
        auditLogService.log("CUSTOMER_STATUS_UPDATED", "USER", String.valueOf(userId), "Set active=" + active);
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UserProfileDto> getStaffList() {
        return userRepository.findByRoleNames(List.of(RoleName.OWNER, RoleName.ADMIN, RoleName.STAFF)).stream()
                .map(mapper::toUserProfileDto)
                .toList();
    }

    @Transactional
    public AuthDtos.UserProfileDto createStaff(AdminDtos.StaffCreateRequest req) {
        if (req.role() == RoleName.CUSTOMER || req.role() == RoleName.OWNER) {
            throw new BadRequestException("Staff accounts can only be assigned ADMIN or STAFF roles.");
        }
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        Role role = roleRepository.findByName(req.role())
                .orElseThrow(() -> new IllegalStateException("Role not found: " + req.role()));

        User staff = new User();
        staff.setFullName(req.fullName().trim());
        staff.setEmail(email);
        staff.setPasswordHash(passwordEncoder.encode(req.password()));
        staff.setPhone(req.phone());
        staff.setActive(true);
        staff.setEmailVerified(true);
        staff.setRoles(Set.of(role));
        User saved = userRepository.save(staff);

        auditLogService.log("STAFF_CREATED", "USER", String.valueOf(saved.getId()),
                "Created " + req.role() + " account for " + email);
        return mapper.toUserProfileDto(saved);
    }

    @Transactional
    public AuthDtos.UserProfileDto updateStaff(Long userId, AdminDtos.StaffUpdateRequest req, Long actorUserId) {
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));
        boolean isOwner = staff.getRoles().stream().anyMatch(r -> r.getName() == RoleName.OWNER);
        if (isOwner && userId.equals(actorUserId)) {
            throw new BadRequestException("You cannot deactivate or downgrade your own OWNER account.");
        }
        if (req.role() != null) {
            if (req.role() == RoleName.OWNER || req.role() == RoleName.CUSTOMER) {
                throw new BadRequestException("Allowed staff roles are ADMIN or STAFF.");
            }
            Role role = roleRepository.findByName(req.role())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
            staff.getRoles().clear();
            staff.getRoles().add(role);
        }
        if (req.active() != null) {
            staff.setActive(req.active());
        }
        User saved = userRepository.save(staff);
        auditLogService.log("STAFF_UPDATED", "USER", String.valueOf(saved.getId()),
                "Updated staff member " + saved.getEmail());
        return mapper.toUserProfileDto(saved);
    }
}
