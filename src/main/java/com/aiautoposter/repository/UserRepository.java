package com.aiautoposter.repository;

import com.aiautoposter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true ORDER BY u.id ASC")
    Optional<User> findActiveByEmail(@Param("email") String email);
    List<User> findByManagerId(Long managerId);
    
    List<User> findByDepartment(String department);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByIsActiveTrue();
    
    @Query("SELECT u FROM User u WHERE u.managerId = :managerId AND u.isActive = true")
    List<User> findActiveUsersByManagerId(@Param("managerId") Long managerId);
    
    @Query("SELECT u FROM User u WHERE u.department = :department AND u.isActive = true")
    List<User> findActiveUsersByDepartment(@Param("department") String department);
    
    boolean existsByEmail(String email);
    List<User> findByRoleInAndIsActiveTrue(List<User.Role> roles);
}
