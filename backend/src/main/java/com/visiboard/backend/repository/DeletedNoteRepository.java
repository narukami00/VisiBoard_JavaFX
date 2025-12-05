package com.visiboard.backend.repository;

import com.visiboard.backend.model.DeletedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeletedNoteRepository extends JpaRepository<DeletedNote, UUID> {
    boolean existsByContent(String content);
    boolean existsByFirebaseDocId(String firebaseDocId);
}
