package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.api.dto.NoteRequest;
import com.lhamacorp.knotes.api.dto.NoteResponse;
import com.lhamacorp.knotes.api.dto.NoteUpdateRequest;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lhamacorp.knotes.context.UserContextHolder.isAuthenticated;
import static com.lhamacorp.knotes.domain.EncryptionMode.PRIVATE;
import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<String>> findByUserId() {
        return ok(service.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<NoteResponse> find(@PathVariable String id,
                                             @RequestParam(required = false) String password) {
        UserContext user = UserContextHolder.get();
        Note note = service.findById(id);

        EncryptionMode mode = note.encryptionMode() != null ? note.encryptionMode() : PUBLIC;

        return switch (mode) {
            case PRIVATE -> ok().body(NoteResponse.fromPrivate(note, user.id()));
            case PASSWORD_SHARED -> ok().body(NoteResponse.fromPasswordShared(note, password));
            case PUBLIC -> ok().body(NoteResponse.from(note));
        };
    }

    @GetMapping("{id}/metadata")
    public ResponseEntity<NoteMetadata> getMetadata(@PathVariable String id) {
        NoteMetadata metadata = service.findMetadataById(id);
        return ok().body(metadata);
    }

    @PutMapping("{id}")
    public ResponseEntity<NoteResponse> update(@PathVariable String id,
                                               @RequestBody NoteUpdateRequest request,
                                               @RequestParam(required = false) String password) {
        UserContext user = UserContextHolder.get();
        String userId = user.id();

        if ("1".equals(userId) && request.encryptionMode() != null
                && !request.encryptionMode().equals("PUBLIC")) {
            return badRequest().build();
        }

        EncryptionMode mode = null;
        if (request.encryptionMode() != null && !request.encryptionMode().isEmpty()) {
            try {
                mode = EncryptionMode.valueOf(request.encryptionMode().toUpperCase());
            } catch (IllegalArgumentException e) {
                return badRequest().build();
            }
        }

        Note updatedNote = service.update(id, request.content(), mode, password);
        EncryptionMode finalMode = updatedNote.encryptionMode() != null ? updatedNote.encryptionMode() : PUBLIC;

        return switch (finalMode) {
            case PRIVATE -> ok().body(NoteResponse.fromPrivate(updatedNote, userId));
            case PASSWORD_SHARED -> ok().body(NoteResponse.fromPasswordShared(updatedNote, password));
            case PUBLIC -> ok().body(NoteResponse.from(updatedNote));
        };
    }

    @PostMapping
    public ResponseEntity<NoteResponse> save(@RequestBody NoteRequest request) {
        String userId = isAuthenticated() ? UserContextHolder.get().id() : "1";

        EncryptionMode mode = userId.equals("1") ? PUBLIC : PRIVATE;
        Note savedNote = service.save(request.note(), mode);

        return switch (mode) {
            case PRIVATE -> ok().body(NoteResponse.fromPrivate(savedNote, userId));
            case PUBLIC -> ok().body(NoteResponse.from(savedNote));
            default -> throw new IllegalStateException("PASSWORD_SHARED not supported in this endpoint");
        };
    }

}
