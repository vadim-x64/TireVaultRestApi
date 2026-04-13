package course.project.ua.tirevault.Services;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service("searchService")
public class SearchService {
    public String highlight(String text, String query) {
        if (text == null || text.isBlank() || query == null || query.isBlank()) {
            return text != null ? truncate(text, 120) : "";
        }

        String truncated = truncate(text, 300);
        return truncated.replaceAll("(?i)(" + Pattern.quote(query.trim()) + ")", "<mark class=\"bg-warning px-0\">$1</mark>");
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "…";
    }
}