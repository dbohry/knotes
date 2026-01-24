package com.lhamacorp.knotes.service;

import com.github.f4b6a3.ulid.Ulid;
import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.NotFoundException;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.github.f4b6a3.ulid.UlidCreator.getUlid;
import static com.lhamacorp.knotes.domain.EncryptionMode.PRIVATE;
import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;

@Service
public class NoteService {

    private final NoteRepository repository;

    private static final String NOT_FOUND = "Note not found!";

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public boolean exists(String id) {
        return repository.existsById(id);
    }

    public List<String> findAll() {
        UserContext user = UserContextHolder.get();

        return "1".equals(user.id())
                ? emptyList()
                : repository.findAllByCreatedBy(user.id()).stream().map(Note::id).toList();
    }

    @Cacheable(value = "content", key = "#id")
    public Note findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    @Cacheable(value = "metadata", key = "#id")
    public NoteMetadata findMetadataById(String id) {
        Note noteProjection = repository.findMetadataById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        return NoteMetadata.from(noteProjection);
    }

    public Note save(String content, EncryptionMode encryptionMode) {
        Ulid id = getUlid();
        UserContext user = UserContextHolder.get();

        Instant now = now();
        return repository.save(new Note(id.toString(), content, user.id(), now, now, encryptionMode, null));
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public Note update(String id, String content, EncryptionMode encryptionMode, String password) {
        Note existingNote = repository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        UserContext user = UserContextHolder.get();

        if (existingNote.encryptionMode() == PRIVATE && !existingNote.createdBy().equals(user.id())) {
            throw new UnauthorizedException("Not authorized to update this content");
        }

        if ("1".equals(user.id())) {
            encryptionMode = PUBLIC;
            password = null;
        }

        if (encryptionMode == null) {
            encryptionMode = existingNote.encryptionMode();
        }

        return repository.save(new Note(id, content, existingNote.createdBy(), existingNote.createdAt(), now(), encryptionMode, password));
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public Note update(String id, String content) {
        Note note = repository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        return update(id, content, note.encryptionMode(), null);
    }

    public void delete(String id) {
        Note note = repository.findById(id).orElseThrow(() -> new NotFoundException("Note not found"));
        String userId = UserContextHolder.get().id();

        if (note.createdBy().equals(userId)) {
            repository.deleteById(note.id());
        }

    }

}
