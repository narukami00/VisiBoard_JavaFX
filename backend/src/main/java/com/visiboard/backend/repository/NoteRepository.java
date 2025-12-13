package com.visiboard.backend.repository;

import com.visiboard.backend.model.Note;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    
    @Query(value = "SELECT * FROM notes n WHERE ST_DWithin(n.location, :point, :radiusInMeters)", nativeQuery = true)
    List<Note> findNearbyNotes(@Param("point") Point point, @Param("radiusInMeters") double radiusInMeters);

    List<Note> findByUserId(java.util.UUID userId);

    Note findByFirebaseId(String firebaseId);
}
