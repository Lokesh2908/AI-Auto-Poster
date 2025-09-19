package com.aiautoposter.repository;

import com.aiautoposter.entity.LinkedInUser;
import com.aiautoposter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LinkedInUserRepository extends JpaRepository<LinkedInUser,Long> {


    Optional<LinkedInUser> findByUser(User user);
    Optional<LinkedInUser> findByLinkedinUserId(String linkedinUserId);
    Optional<LinkedInUser> findByPersonUrn(String personUrn);

    @Query("SELECT lu FROM LinkedInUser lu WHERE lu.user = :user AND lu.accessTokenExpiresAt > :now")
    Optional<LinkedInUser> findValidTokenByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
