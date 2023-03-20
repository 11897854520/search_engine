package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.auxiliary.ContentHandling;
import searchengine.auxiliary.Task;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.Response;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.insertRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

// Класс для индексации сайтов.
@Service
@RequiredArgsConstructor
public class IndexSitesServiceImpl implements IndexSitesService {

    private final SitesList sites;
    private final ContentHandling contentHandling;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private insertRepository insertRepository;

    private static int count = 0;
    private static volatile boolean interruptIt;
    private static boolean sitesContainsUrl;

    private Thread thread;

    // Метод для индексации или переиндексации всех сайтов из списка файла "Application.yaml"
    private void index() {

        if (interruptIt) {

            interruptIt = false;
            insertRepository.insert("UPDATE site SET status = 'INDEXING', last_error = null" +
                    " WHERE status = 'FAILED' AND " +
                    "last_error = 'Произошла ошибка. Причина: индексация остановлена пользователем'");

        } else {

            interruptIt = false;

            siteRepository.findAll().forEach(s -> {

                if (!sites.getSites().stream().map(Site::getUrl).toList().contains(s.getUrl())) {

                    siteRepository.delete(siteRepository.findByUrl(s.getUrl()));

                }

            });

            sites.getSites().forEach(site -> {

                thread = new Thread(new Task(site, siteRepository, pageRepository, contentHandling));
                thread.start();

            });

        }

    }

    // Метод для осуществления индексации или переиндексации одного сайта из списка файла "Application.yaml"

    private void indexSingleSite(String url) {

        sitesContainsUrl = false;

        sites.getSites().forEach(site -> {

            if (site.getUrl().equals(url)) {

                sitesContainsUrl = true;

                if (interruptIt) {

                    interruptIt = false;
                    insertRepository.insert("UPDATE site SET status = 'INDEXING', last_error = null" +
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
    private boolean isIndexed() {

        if (count != 0) {

            return count % sites.getSites().size() == 0;

        }

        return true;

    }

    // Метод для остановки индексации.
    private void stopIndexing() {


        if (thread != null) {

            interruptIt = true;
            insertRepository.insert("UPDATE site SET status = 'FAILED'," +
                    " last_error = 'Произошла ошибка." +
                    " Причина: индексация остановлена пользователем' WHERE status = 'INDEXING'");

        }

    }

    // Метод, который возвращает информацию прервана индексация или нет.
    private boolean isInterrupted() {

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
    private static boolean isInterruptIt() {

        return interruptIt;

    }

    // Метод, возвращающий информацию о наличии адреса, указанного в запросе в списке файла "Application.yaml"
    private static boolean isSitesContainsUrl() {

        return sitesContainsUrl;

    }

    public static void setCount(int count) {

        IndexSitesServiceImpl.count = count;

    }

    // Метод для прерывания индексации (потока).
    public  void interruptThread() {

        while (IndexSitesServiceImpl.isInterruptIt()) {

            try {

                Thread.sleep(1);

            } catch (InterruptedException e) {

                System.out.println(e.getMessage());

            }
        }

    }

    // Метод для вызова индексации всех сайтов из API-контроллера
    public ResponseEntity<Response> startIndexingSitesInController() {
        String errorResponse = "Индексация уже запущена";

        if (!isIndexed() && !IndexSitesServiceImpl.isInterruptIt()) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

        }

        index();
        return ResponseEntity.ok(new Response(true, null));

    }

    // Метод для остановки индексации в API-контроллере.
    public ResponseEntity<Response> stopIndexingInController() {

        String errorResponse = "Индексация не запущена";

        if (isInterrupted()) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

        }

        stopIndexing();
        return ResponseEntity.ok(new Response(true, null));

    }

    // Метод для вызова индексации одного сайта в API-контроллере
    public ResponseEntity<Response> startIndexingSingleSiteInController(String url) {

        String errorResponse = "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле";

        indexSingleSite(url);

        return IndexSitesServiceImpl.isSitesContainsUrl() ? ResponseEntity.ok(new Response(true, null))
                :  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

    }
}
