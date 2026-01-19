package com.lhamacorp.knotes.service;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.NotFoundException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.slf4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class NoteService {

    private final NoteRepository repository;

    private static final Logger log = getLogger(NoteService.class);
    private static final String NOT_FOUND = "Note not found!";

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public boolean exists(String id) {
        return repository.existsById(id);
    }

    public List<String> findAll() {
        UserContext user = UserContextHolder.get();

        //id 1 is anon and this should only return a list for authenticated users
        return "1".equals(user.id())
                ? emptyList()
                : repository.findAllByCreatedBy(user.id()).stream().map(Note::id).toList();

    }

    @Cacheable(value = "content", key = "#id")
    public Note findById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    @Cacheable(value = "metadata", key = "#id")
    public NoteMetadata findMetadataById(String id) {
        Note noteProjection = repository.findMetadataById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        return NoteMetadata.from(noteProjection);
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#result.id")
    public Note save(String content) {
        Ulid id = UlidCreator.getUlid();

        log.debug("Saving note [{}]", id);

        Instant now = now();
        return repository.save(new Note(id.toString(), content, UserContextHolder.get().id(), now, now));
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public Note update(String id, String content) {
        Note note = repository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));

        log.debug("Updating note [{}]", id);

        return repository.save(new Note(id, content, note.createdBy(), note.createdAt(), now()));
    }

}
