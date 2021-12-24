package io.github.gcdd1993.casdemo.app1.cas;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class URLUtil {
    public static Map<String, List<String>> parseQuery(String query) {
        return Arrays.stream(query.split("&"))
                .filter(s -> !s.isEmpty())
                .map(pair -> pair.split("="))
                .collect(Collectors.groupingBy(
                        pair -> urlDecode(pair[0]),
                        HashMap::new,
                        Collectors.mapping(
                                pair -> pair.length > 1 ? urlDecode(pair[1]) : "",
                                Collectors.toList()
                        )
                ));
    }

    public static String urlEncode(String content) {
        try {
            return URLEncoder.encode(content, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Non-existing encoding UTF-8, which shouldn't happen", e);
        }
    }

    public static String urlDecode(String content) {
        try {
            return URLDecoder.decode(content, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Non-existing encoding UTF-8, which shouldn't happen", e);
        }
    }

    private URLUtil() {
    }
}
