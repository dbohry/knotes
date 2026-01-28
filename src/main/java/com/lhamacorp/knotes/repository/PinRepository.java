package com.lhamacorp.knotes.repository;

import com.lhamacorp.knotes.domain.Pin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PinRepository extends MongoRepository<Pin, String> {

    List<Pin> findAllByUserId(String userId);

    void deleteAllByNoteId(String noteId);

    void deletePinByUserIdAndNoteId(String noteId, String userId);

}
