package com.visiboard.backend.repository;

import com.visiboard.backend.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    Optional<UserFollow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    long countByFollowerId(UUID followerId);
    long countByFollowedId(UUID followedId);
    List<UserFollow> findByFollowerId(UUID followerId);
    List<UserFollow> findByFollowedId(UUID followedId);
}
