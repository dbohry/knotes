package com.lhamacorp.knotes.service;

import com.github.f4b6a3.ulid.Ulid;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.domain.Pin;
import com.lhamacorp.knotes.exception.BadRequestException;
import com.lhamacorp.knotes.exception.NotFoundException;
import com.lhamacorp.knotes.repository.PinRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.github.f4b6a3.ulid.UlidCreator.getUlid;
import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;
import static java.time.Instant.now;

@Service
public class PinService {

    private final PinRepository repository;
    private final NoteService noteService;

    public PinService(PinRepository repository, NoteService noteService) {
        this.repository = repository;
        this.noteService = noteService;
    }

    public List<Pin> get(String userId) {
        return repository.findAllByUserId(userId);
    }

    public Pin create(String noteId, String userId) {
        Note note = noteService.get(noteId);

        if (!PUBLIC.equals(note.encryptionMode())) {
            throw new BadRequestException("Note cannot be pinned");
        }

        Ulid id = getUlid();

        return repository.save(new Pin(id.toString(), note.id(), userId, now()));
    }

    public void remove(String id, String userId) {
        Pin pin = repository.findById(id).orElseThrow(() -> new NotFoundException("Pin not found"));

        if (pin.userId().equals(userId)) {
            repository.deleteById(id);
        }
    }

}
