package searchengine.dto;

// рекорд-класс содержащий информацию о наличии слов, указанных в запросе для каждой страницы.
public record InformationAboutLemmas(String site, String siteName,
                                     String url, String title, String snippet, Float relevance) {

}
