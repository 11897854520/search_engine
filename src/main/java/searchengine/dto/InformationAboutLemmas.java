package searchengine.dto;

public record InformationAboutLemmas(String site, String siteName, String uri, String title, String snippet
        , Float relevance) {
}
