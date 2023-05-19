package searchengine.parser;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.IndexSitesService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

// Класс для рекурсивного обхода сайтов при помощи forkJoin.
@RequiredArgsConstructor
public class SiteParser extends RecursiveTask<Set<Page>> {
    private final String url;
    private final Site site;
    private Set<Page> pageSet = new HashSet<>();
    private final Set<String> copyLinks;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final Map<String, Integer> frequencyOfLemmas;
    private final IndexSitesService indexSitesService;


    @Override
    protected Set<Page> compute() {
        try {
            for (SiteParser parser : parse()) {
                pageSet.addAll(parser.join());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return pageSet;
    }

    protected Document document(String page) throws IOException {
        return Jsoup.connect(page).ignoreContentType(true)
                .userAgent("Yandex").get();
    }

    private Set<SiteParser> parse() throws InterruptedException, IOException {
        Set<SiteParser> tasks = new HashSet<>();
        Thread.sleep(150);
        Elements elements = document(url).select("a");
        elements.forEach(element -> {
            String link = element.attr("abs:href");
            String siteUrl = site.getUrl();
            String path = link.replace(siteUrl, "");
            int code;
            String content = "";
            if (!link.contains("pdf") && !link.isEmpty() && !link.contains("#") && !link.endsWith("jpg")
                    && !copyLinks.contains(link) && !link.equals(url) && link.startsWith(siteUrl)
                    && !link.contains("?") && !link.endsWith("mp4")
                    && !link.endsWith("JPG") && !link.endsWith("jpeg") && !link.endsWith("PDF")
                    && !link.equals(siteUrl.concat("/")) && !link.endsWith("png")) {
                try {
                    content = document(link).outerHtml();
                    code = document(link).connection().response().statusCode();
                } catch (Exception e) {
                    String errorStatus = e.getMessage().replaceAll("\\D+", "");
                    code = errorStatus.length() > 3 ? Integer.parseInt(errorStatus.substring(0, 3)) : 404;
                }
                System.out.println(link);
                SiteParser parser = new SiteParser(link, site, copyLinks, pageRepository, lemmaRepository
                        , searchIndexRepository, frequencyOfLemmas, indexSitesService);
                parser.fork();
                tasks.add(parser);
                copyLinks.add(link);
                pageSet.add(new Page(path, code, content, site));
                if (pageSet.size() > 1000) {
                    try {
                        pageRepository.saveAll(pageSet);
                        ContentHandling.writeLemmasAndSearchIndexIntoSql(pageSet, site, lemmaRepository
                                , searchIndexRepository, frequencyOfLemmas);
                        pageSet.clear();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            indexSitesService.interruptThread();
        });
        return tasks;
    }
}





