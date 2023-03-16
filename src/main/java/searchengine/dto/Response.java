package searchengine.dto;

// Рекород-класс для вывода информации о результатах вызова методов API.
public record Response(boolean result, String error) {
}
