package searchengine.services;

public interface IndexSites {

    void index();
    void indexSingleSite(String url);
    boolean isIndexed();
    void stopIndexing();
    boolean isInterrupted();
    void interruptThread();
}
