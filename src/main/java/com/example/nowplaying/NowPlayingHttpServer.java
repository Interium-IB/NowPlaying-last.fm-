package com.example.nowplaying;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minimal local HTTP server that accepts now-playing updates.
 * Endpoints:
 *   - POST /nowplaying  with JSON {"title":"...","artist":"...","source":"YouTube Music"}
 *   - POST /clear       to clear current state
 *   - GET  /nowplaying  returns JSON of current state
 */
public final class NowPlayingHttpServer {
    private static volatile HttpServer server;

    private NowPlayingHttpServer() {}

    public static synchronized void startIfNotRunning(int port) throws IOException {
        if (server != null) return;
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        HttpServer httpServer = HttpServer.create(address, 0);
        httpServer.createContext("/nowplaying", new NowPlayingHandler());
        httpServer.createContext("/clear", new ClearHandler());
        httpServer.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
        httpServer.start();
        server = httpServer;
    }

    private static final class NowPlayingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                if ("POST".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = Json.parseFlatStringMap(body);
                    String title = map.getOrDefault("title", "");
                    String artist = map.getOrDefault("artist", "");
                    String source = map.getOrDefault("source", "");
                    NowPlayingService.update(title, artist, source);
                    respondJson(exchange, 200, "{\"status\":\"ok\"}");
                } else if ("GET".equalsIgnoreCase(method)) {
                    String json = Json.stringify(Map.of(
                        "title", Json.escape(NowPlayingService.getCurrentTitle()),
                        "artist", Json.escape(NowPlayingService.getCurrentArtist()),
                        "source", Json.escape(NowPlayingService.getCurrentSource())
                    ));
                    respondJson(exchange, 200, json);
                } else {
                    respondJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                }
            } catch (Exception e) {
                respondJson(exchange, 500, "{\"error\":\"" + Json.escape(e.getMessage()) + "\"}");
            }
        }
    }

    private static final class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            NowPlayingService.clear();
            respondJson(exchange, 200, "{\"status\":\"cleared\"}");
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // No separate state holder; we use NowPlayingService getters.

    /** Tiny, no-deps JSON helpers for our simple flat string map use case */
    private static final class Json {
        static Map<String, String> parseFlatStringMap(String json) {
            if (json == null) return Map.of();
            String trimmed = json.trim();
            if (trimmed.isEmpty() || trimmed.equals("{}")) return Map.of();
            if (trimmed.charAt(0) != '{' || trimmed.charAt(trimmed.length()-1) != '}') return Map.of();
            String inner = trimmed.substring(1, trimmed.length()-1);
            if (inner.isEmpty()) return Map.of();
            return java.util.Arrays.stream(inner.split(","))
                .map(String::trim)
                .map(pair -> pair.split(":", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                    arr -> unquote(arr[0].trim()),
                    arr -> unquote(arr[1].trim())
                ));
        }

        static String unquote(String s) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length()-1).replace("\\\"", "\"");
            }
            return s;
        }

        static String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        static String stringify(Map<String, String> map) {
            String body = map.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
                .collect(Collectors.joining(","));
            return "{" + body + "}";
        }
    }
}


