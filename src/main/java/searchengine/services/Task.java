package searchengine.services;

import lombok.RequiredArgsConstructor;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.model.SiteStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class Task implements Runnable {

    private final searchengine.config.Site site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ContentHandling contentHandling;

    private IndexSites indexSites = new IndexSitesImpl(new SitesList(), new ContentHandlingImpl());


    @Override
    public void run() {

        try {

            if (siteRepository.findByUrl(site.getUrl()) != null) {

                rewriteInSql();

            } else {

                writeInSql();

            }

        } catch (IOException e) {

            throw new RuntimeException();

        }

        IndexSitesImpl.setCount(IndexSitesImpl.getCount() + 1);

    }

    // Метод для записи данных в sql, если таблицы не заполнены.
    private synchronized void writeInSql() throws IOException {

        String error = null;
        Site siteTable;

        siteTable = new Site(SiteStatus.INDEXING
                , LocalDateTime.now()
                , null, site.getUrl()
                , site.getName());
        siteRepository.save(siteTable);
        indexSites.interruptThread();

        SiteParser siteParser = new SiteParser(site.getUrl(), siteTable);
        List<Page> pageList = new ForkJoinPool().invoke(siteParser);
        pageRepository.saveAll(pageList);
        contentHandling.writeLemmasInSql(pageList, siteTable);

        try {

            siteParser.document(site.getUrl()).connection().response().statusCode();

        } catch (Exception e) {

            error = "Произошла ошибка. Причина:" + "\n" + e.getMessage();

        }

        indexSites.interruptThread();
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

    // Метод для записи данных в sql, если таблицы уже заполнены.
    private synchronized void rewriteInSql() throws IOException {

        String error = null;
        Site siteTable;

        Site old = siteRepository.findByUrl(site.getUrl());
        siteTable = new Site(SiteStatus.INDEXING
                , LocalDateTime.now()
                , null, old.getUrl()
                , old.getName());
        siteRepository.delete(old);
        siteRepository.save(siteTable);
        indexSites.interruptThread();

        SiteParser.COPY_LINKS.clear();
        SiteParser siteParser = new SiteParser(site.getUrl(), siteTable);
        List<Page> pageList = new ForkJoinPool().invoke(siteParser);
        pageRepository.saveAll(pageList);
        contentHandling.writeLemmasInSql(pageList, siteTable);

        try {

            siteParser.document(site.getUrl()).connection().response().statusCode();

        } catch (Exception e) {

            error = "Произошла ошибка. Причина:" + "\n" + e.getMessage();

        }

        indexSites.interruptThread();
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