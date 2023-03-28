package searchengine.parser;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.IndexSitesService;
import searchengine.services.IndexSitesServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

// Класс для рекурсивного обхода сайтов при помощи forkJoin.
@RequiredArgsConstructor
public class SiteParser extends RecursiveTask<List<Page>> {

    private final String url;
    protected static final Set<String> COPY_LINKS = new HashSet<>();
    private List<Page> pageList = new ArrayList<>();
    private final Site site;
    private IndexSitesService indexSitesService = new IndexSitesServiceImpl(new SitesList());

    @Override
    protected List<Page> compute() {

        try {

            for (SiteParser parser : parse()) {

                pageList.addAll(parser.join());

            }

        } catch (InterruptedException e) {

            System.out.println(e.getMessage());

        } catch (IOException e) {

            System.out.println(e.getMessage());


        } catch (NumberFormatException e) {

            System.out.println(e.getMessage());

        }

        return pageList;

    }

    protected Document document(String page) throws IOException {

        Document document = Jsoup.connect(page).ignoreContentType(true)
                .userAgent("Yandex").get();

        return document;

    }

    private Set<SiteParser> parse() throws InterruptedException, IOException {

        Set<SiteParser> task = new HashSet<>();
        Thread.sleep(150);
        Elements elements = document(url).select("a");

        elements.forEach(element -> {

            String links = element.attr("abs:href");
            String path = links.contains(url) ? links.replace(url, "") : links;
            int code;
            String content = "";


            if (!links.contains("pdf") && !links.isEmpty() && !links.contains("#")
                    && !COPY_LINKS.contains(links) && links.contains(url)
                    && !links.equals(url)) {

                try {

                    content = document(links).outerHtml();
                    code = document(links).connection().response().statusCode();


                } catch (IOException e) {

                    code = Integer.parseInt(e.getMessage().replaceAll("\\D+", ""));

                }

                SiteParser parser = new SiteParser(links, site);
                parser.fork();
                task.add(parser);
                COPY_LINKS.add(links);
                pageList.add(new Page(path, code, content, site));
                indexSitesService.interruptThread();

            }

        });

        return task;

    }
}





