package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigurationForJsoupConnection;
import searchengine.entities.SiteStatus;
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
    private final ConfigurationForJsoupConnection jsoupConnection;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SearchIndexRepository searchIndexRepository;
    private static boolean interruptIt;
    private boolean sitesContainsUrl;
    private boolean threadsAreRunning;

    // Метод для индексации или переиндексации всех сайтов из списка файла "Application.yaml"
    private void indexAllSites() {
        threadsAreRunning = true;
        interruptIt = false;
        deleteSiteFromDataBaseIfDoesNotExist();
        sites.getSites().forEach(site -> new Thread(new SqlWriter(site, siteRepository, pageRepository
                , lemmaRepository, searchIndexRepository, jsoupConnection)).start());
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
        interruptIt = false;
        sites.getSites().forEach(site -> {
            if (site.getUrl().equals(url)) {
                sitesContainsUrl = true;
                new Thread(new SqlWriter(site, siteRepository, pageRepository
                        , lemmaRepository, searchIndexRepository, jsoupConnection)).start();
            }
        });
        if (SqlWriter.count == 1) {
            threadsAreRunning = false;
            SqlWriter.count = 0;
        }
    }

    // Метод для остановки индексации.
    private void stopIndexing() {
        if (threadsAreRunning) {
            interruptIt = true;
            String error = "Произошла ошибка. Причина: индексация остановлена пользователем";
            updateSite(error);
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
    private void updateSite(String lastErrorAfter) {
        siteRepository.findAllByStatusAndLastError(SiteStatus.INDEXING, null).forEach(site1 -> {
            site1.setStatus(SiteStatus.FAILED);
            site1.setLastError(lastErrorAfter);
            siteRepository.save(site1);
        });
    }

    public boolean isInterruptIt() {
        return interruptIt;
    }

    // Метод для вызова индексации всех сайтов из API-контроллера
    public Response startIndexingSitesInController() {
        String errorResponse = "Индексация уже запущена";
        if (!isInterrupted()) {
            return new Response(false, errorResponse);
        }
        indexAllSites();
        return new Response(true, null);
    }

    // Метод для остановки индексации в API-контроллере.
    public Response stopIndexingInController() {
        String errorResponse = "Индексация не запущена";
        if (isInterrupted()) {
            return new Response(false, errorResponse);
        }
        stopIndexing();
        return new Response(true, null);
    }

    // Метод для вызова индексации одного сайта в API-контроллере
    public Response startIndexingSingleSiteInController(String url) {
        String errorResponse = "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле";
        indexSingleSite(url);
        return sitesContainsUrl ? new Response(true, null)
                : new Response(false, errorResponse);
    }
}
