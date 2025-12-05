package com.visiboard.backend.repository;

import com.visiboard.backend.model.Message;
import com.visiboard.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByToUser(User toUser);
    List<Message> findByFromUser(User fromUser);
}
