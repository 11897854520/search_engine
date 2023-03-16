package searchengine.services;
import searchengine.dto.InformationAboutSearching;
import java.io.IOException;

public interface SearchLemmas {

    InformationAboutSearching searchResult(String query, String site, int offset, int limit)
            throws IOException;


}
