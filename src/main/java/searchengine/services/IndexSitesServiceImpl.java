package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.SiteStatus;
import searchengine.parser.SqlWriter;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.Response;
import searchengine.repositories.*;

// Класс для индексации сайтов.
@Service
@RequiredArgsConstructor
public class IndexSitesServiceImpl implements IndexSitesService {
    private final SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SearchIndexRepository searchIndexRepository;
    private static volatile boolean interruptIt;
    private boolean sitesContainsUrl;
    private boolean threadsAreRunning;
    private String error = "Произошла ошибка. Причина: индексация остановлена пользователем";

    // Метод для индексации или переиндексации всех сайтов из списка файла "Application.yaml"
    private void indexAllSites() {
        threadsAreRunning = true;
        if (interruptIt) {
            interruptIt = false;
            updateSite(SiteStatus.FAILED, SiteStatus.INDEXING, error, null);
        } else {
            interruptIt = false;
            deleteSiteFromDataBaseIfDoesNotExist();
            sites.getSites().forEach(site -> new Thread(new SqlWriter(site, siteRepository, pageRepository
                    , lemmaRepository, searchIndexRepository)).start());
        }
        if (SqlWriter.count % sites.getSites().size() == 0 && SqlWriter.count != 0) {
            SqlWriter.count = 0;
        }
    }

    //Метод для удаления сайта из базы данных если его нет в файле конфигурации.
    private void deleteSiteFromDataBaseIfDoesNotExist() {
        siteRepository.findAll().forEach(s -> {
            if (!sites.getSites().stream().map(Site::getUrl).toList().contains(s.getUrl())) {
                siteRepository.delete(siteRepository.findByUrl(s.getUrl()));
            }
        });
    }

    // Метод для осуществления индексации или переиндексации одного сайта из списка файла "Application.yaml"
    private void indexSingleSite(String url) {
        threadsAreRunning = true;
        sitesContainsUrl = false;
        sites.getSites().forEach(site -> {
            if (site.getUrl().equals(url)) {
                sitesContainsUrl = true;
                changeSiteInDataBaseOrStartIndexingOfSingleSite(site);
            }
        });
        if (SqlWriter.count == 1) {
            threadsAreRunning = false;
            SqlWriter.count = 0;
        }
    }

    private void changeSiteInDataBaseOrStartIndexingOfSingleSite(Site site) {
        if (interruptIt) {
            interruptIt = false;
            updateSite(SiteStatus.FAILED, SiteStatus.INDEXING, error, null);
        } else {
            interruptIt = false;
            new Thread(new SqlWriter(site, siteRepository, pageRepository
                    , lemmaRepository, searchIndexRepository)).start();
        }
    }

    // Метод для остановки индексации.
    private void stopIndexing() {
        if (threadsAreRunning) {
            interruptIt = true;
            updateSite(SiteStatus.INDEXING, SiteStatus.FAILED, null, error);
        }
    }

    // Метод, который возвращает информацию прервана индексация или нет.
    private boolean isInterrupted() {
        if (threadsAreRunning) {
            return interruptIt;
        }
        return true;
    }

    /*Метод для изменения статуса сайта и строки lastError при остановке индексации либо при запуске после
 после остановки.*/
    private void updateSite(SiteStatus before
            , SiteStatus after, String lastErrorBefore, String lastErrorAfter) {
        siteRepository.findAllByStatusAndLastError(before, lastErrorBefore).forEach(site1 -> {
            site1.setStatus(after);
            site1.setLastError(lastErrorAfter);
            siteRepository.save(site1);
        });
    }

    // Метод для прерывания индексации (потока).
    public void interruptThread() {
        while (interruptIt) {
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
        if (!isInterrupted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));
        }
        indexAllSites();
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
        return sitesContainsUrl ? ResponseEntity.ok(new Response(true, null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));
    }
}
