package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ContentHandling {


void writeLemmasInSql(List<Page>pageList, Site site) throws IOException;
    Map<String, Integer> amountOfLemmas(String text) throws IOException;
    boolean lemmasIsEquals(String one, String two) throws IOException;

}
