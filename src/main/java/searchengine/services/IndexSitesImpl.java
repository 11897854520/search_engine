package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.MultiInsertRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.stream.Collectors;

// Класс для индексации сайтов.
@Service
@RequiredArgsConstructor
public class IndexSitesImpl implements IndexSites {

    private final SitesList sites;
    private final ContentHandling contentHandling;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private MultiInsertRepository multiInsertRepository;

    private static int count = 0;
    private static volatile boolean interruptIt;
    private static boolean sitesContainsUrl;

    private Thread thread;

    // Метод для индексации или переиндексации всех сайтов из списка файла "Application.yaml"
    @Override
    public void index() {

        if (interruptIt) {

            interruptIt = false;
            multiInsertRepository.multiInsert("UPDATE site SET status = 'INDEXING', last_error = null" +
                    " WHERE status = 'FAILED' AND " +
                    "last_error = 'Произошла ошибка. Причина: индексация остановлена пользователем'");

        } else {

            interruptIt = false;

            siteRepository.findAllUrl().forEach(s -> {

                if (!sites.getSites().stream().map(Site::getUrl).toList().contains(s)) {

                    siteRepository.delete(siteRepository.findByUrl(s));

                }

            });

            sites.getSites().forEach(site -> {

                thread = new Thread(new Task(site, siteRepository, pageRepository, contentHandling));
                thread.start();

            });

        }

    }

    // Метод для осуществления индексации или переиндексации одного сайта из списка файла "Application.yaml"
    @Override
    public void indexSingleSite(String url) {

        sitesContainsUrl = false;

        sites.getSites().forEach(site -> {

            if (site.getUrl().equals(url)) {

                sitesContainsUrl = true;

                if (interruptIt) {

                    interruptIt = false;
                    multiInsertRepository.multiInsert("UPDATE site SET status = 'INDEXING', last_error = null" +
                            " WHERE status = 'FAILED' AND " +
                            "last_error = 'Произошла ошибка. Причина: индексация остановлена пользователем'");

                } else {

                    interruptIt = false;
                    thread = new Thread(new Task(site, siteRepository, pageRepository, contentHandling));
                    thread.start();

                }

            }

        });

    }

    // Метод, возвращающий информацию заверщилась индексация, или нет.
    public boolean isIndexed() {

        if (count != 0) {

            return count % sites.getSites().size() == 0;

        }

        return true;

    }

    // Метод для остановки индексации.
    public void stopIndexing() {


        if (thread != null) {

            interruptIt = true;
            multiInsertRepository.multiInsert("UPDATE site SET status = 'FAILED'," +
                    " last_error = 'Произошла ошибка." +
                    " Причина: индексация остановлена пользователем' WHERE status = 'INDEXING'");

        }

    }

    // Метод, который возвращает информацию прервана индексация или нет.
    public boolean isInterrupted() {

        if (thread != null) {

            return interruptIt;

        }

        return true;

    }

    // Метод содержащий информацию о количестве проиндексированных сайтов.
    public static int getCount() {

        return count;

    }

    // Метод для возврата значения переменной, которая используется для остановки индексации (потока).
    public static boolean isInterruptIt() {

        return interruptIt;

    }

    // Метод, возвращающий информацию о наличии адреса, указанного в запросе в списке файла "Application.yaml"
    public static boolean isSitesContainsUrl() {

        return sitesContainsUrl;

    }

    protected static void setCount(int count) {

        IndexSitesImpl.count = count;

    }

    // Метод для прерывания индексации (потока).
    public  void interruptThread() {

        while (IndexSitesImpl.isInterruptIt()) {

            try {

                Thread.sleep(1);

            } catch (InterruptedException e) {

                System.out.println(e.getMessage());

            }
        }

    }
}
