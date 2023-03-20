package searchengine.services;
import org.springframework.http.ResponseEntity;
import searchengine.dto.InformationAboutSearching;
import java.io.IOException;

public interface SearchLemmasService {


    ResponseEntity<?> startSearchingLemmasInController(String query, String site, int offset, int limit)
            throws IOException;


}
