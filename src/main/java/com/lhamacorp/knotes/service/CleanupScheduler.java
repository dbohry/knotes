package com.lhamacorp.knotes.service;

import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.repository.NoteRepository;
import com.lhamacorp.knotes.repository.PinRepository;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class CleanupScheduler {

    private final NoteRepository noteRepository;
    private final PinRepository pinRepository;

    private static final String ONCE_PER_DAY_AT_2AM = "0 0 2 * * *";
    private static final Logger log = getLogger(CleanupScheduler.class);

    public CleanupScheduler(NoteRepository noteRepository, PinRepository pinRepository) {
        this.noteRepository = noteRepository;
        this.pinRepository = pinRepository;
    }

    @Scheduled(cron = ONCE_PER_DAY_AT_2AM)
    public void cleanup() {
        List<String> ids = noteRepository.findEmptyNotes()
                .stream()
                .filter(note -> note.createdAt().isBefore(now().minus(1, DAYS)))
                .map(Note::id)
                .filter(id -> !isPinned(id))
                .toList();

        if (!ids.isEmpty()) {
            log.info("Cleaning empty notes [{}]", ids);
            noteRepository.deleteAllById(ids);
        }
    }

    private boolean isPinned(String noteId) {
        return !pinRepository.findAllByNoteId(noteId).isEmpty();
    }

}
