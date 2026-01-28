package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.api.dto.PinRequest;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.Pin;
import com.lhamacorp.knotes.service.PinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lhamacorp.knotes.context.UserContextHolder.isAuthenticated;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/pins")
@CrossOrigin(origins = "*")
public class PinController {

    private final PinService service;

    public PinController(PinService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Pin>> get() {
        if (isAuthenticated()) {
            List<Pin> pins = service.get(UserContextHolder.get().id());
            return ResponseEntity.ok(pins);
        }

        return ResponseEntity.status(UNAUTHORIZED).build();
    }

    @PostMapping
    public ResponseEntity<Pin> pin(@RequestBody PinRequest request) {
        if (isAuthenticated()) {
            Pin pin = service.create(request.noteId(), UserContextHolder.get().id());
            return ResponseEntity.ok(pin);
        }

        return ResponseEntity.status(UNAUTHORIZED).build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (isAuthenticated()) {
            service.remove(id, UserContextHolder.get().id());
        }

        return ResponseEntity.ok().build();
    }

}
