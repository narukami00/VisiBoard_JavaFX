package com.visiboard.backend.repository;

import com.visiboard.backend.model.NoteLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteLikeRepository extends JpaRepository<NoteLike, UUID> {
    List<NoteLike> findByNoteId(UUID noteId);
    Optional<NoteLike> findByUserIdAndNoteId(UUID userId, UUID noteId);
}
