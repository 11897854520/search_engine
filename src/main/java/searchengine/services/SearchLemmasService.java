package searchengine.services;

import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface SearchLemmasService {
    ResponseEntity<?> startSearchingLemmas(String query, String site, int offset, int limit)
            throws IOException;
}
