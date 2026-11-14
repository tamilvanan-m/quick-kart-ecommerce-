package com.quickkart.repository;

import com.quickkart.entity.Role;
import com.quickkart.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
