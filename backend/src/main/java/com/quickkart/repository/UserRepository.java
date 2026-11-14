package com.quickkart.repository;

import com.quickkart.entity.User;
import com.quickkart.entity.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByResetToken(String resetToken);
    boolean existsByRoles_Name(RoleName roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") RoleName roleName, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<RoleName> roleNames);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchByRoleAndQuery(@Param("roleName") RoleName roleName, @Param("q") String q, Pageable pageable);
}
