package searchengine.parser;

import lombok.RequiredArgsConstructor;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.model.SiteStatus;
import searchengine.services.IndexSitesService;
import searchengine.services.IndexSitesServiceImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SqlWriter implements Runnable {
    private final searchengine.config.Site site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private Set<String> copyLinks = new HashSet<>();
    private Set<Page> pageSet = new HashSet<>();
    Map<String, Integer> frequencyOfLemmas = new HashMap<>();
    private IndexSitesService indexSitesService = new IndexSitesServiceImpl(new SitesList());

    @Override
    public void run() {
        try {
            if (siteRepository.findByUrl(site.getUrl()) != null) {
                rewriteAllIntoSql();
            } else {
                writeAllIntoSql();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        IndexSitesServiceImpl.setCount(IndexSitesServiceImpl.getCount() + 1);
    }

    // Метод для записи данных в sql, если таблицы не заполнены.
    private synchronized void writeAllIntoSql() {
        Site siteTable;
        siteTable = new Site(SiteStatus.INDEXING
                , LocalDateTime.now()
                , null, site.getUrl()
                , site.getName());
        siteRepository.save(siteTable);
        indexSitesService.interruptThread();
        parsingOfSiteAndWritingIntoSql(siteTable);
    }

    // Метод для записи данных в sql, если таблицы уже заполнены.
    private synchronized void rewriteAllIntoSql() {
        Site siteTable;
        Site old = siteRepository.findByUrl(site.getUrl());
        siteTable = new Site(SiteStatus.INDEXING
                , LocalDateTime.now()
                , null, old.getUrl()
                , old.getName());
        siteRepository.delete(old);
        siteRepository.save(siteTable);
        indexSitesService.interruptThread();
        copyLinks.clear();
        parsingOfSiteAndWritingIntoSql(siteTable);
    }

    private void parsingOfSiteAndWritingIntoSql(Site siteTable) {
        String error = null;
        SiteParser siteParser = new SiteParser(site.getUrl(), siteTable, copyLinks, pageRepository, lemmaRepository
                , searchIndexRepository, frequencyOfLemmas);
        pageSet.addAll(new ForkJoinPool().invoke(siteParser));
        pageRepository.saveAll(pageSet);
        ContentHandling.writeLemmasAndSearchIndexIntoSql(pageSet, siteTable, lemmaRepository, searchIndexRepository
                , frequencyOfLemmas);
        try {
            siteParser.document(site.getUrl()).connection().response().statusCode();
        } catch (Exception e) {
            error = "Произошла ошибка. Причина:" + "\n" + e.getMessage();
        }
        indexSitesService.interruptThread();
        Site before = siteRepository.findByUrl(site.getUrl());
        Site after = new Site(error != null
                ? SiteStatus.FAILED
                : SiteStatus.INDEXED
                , LocalDateTime.now()
                , error
                , before.getUrl()
                , before.getName());
        after.setId(before.getId());
        siteRepository.save(after);
    }
}