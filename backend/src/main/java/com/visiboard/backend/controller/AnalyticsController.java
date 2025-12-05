package com.visiboard.backend.controller;

import com.visiboard.backend.model.Note;
import com.visiboard.backend.repository.NoteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final NoteRepository noteRepository;

    public AnalyticsController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping("/engagement")
    public Map<String, Long> getWeeklyEngagement() {
        // Fetch all notes (in a real app, filter by last 7 days)
        List<Note> notes = noteRepository.findAll();
        
        // Group by Day of Week
        Map<String, Long> engagement = notes.stream()
                .filter(n -> n.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        n -> n.getCreatedAt().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        Collectors.counting()
                ));
        
        // Ensure all days are present (optional, but good for charts)
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            engagement.putIfAbsent(dayName, 0L);
        }
        
        return engagement;
    }
}
