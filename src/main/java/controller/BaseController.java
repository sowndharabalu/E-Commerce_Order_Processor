package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.ApiException;
import util.DataAccessException;
import util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseController implements HttpHandler {
    private static final int MAX_BODY_BYTES = 1 << 20;
    private static final Logger logger = Logger.getLogger(BaseController.class.getName());

    protected abstract void route(HttpExchange ex, List<String> segments) throws IOException;

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            System.out.println(ex.getRequestMethod() + ":" + ex.getRequestURI());
            List<String> segments = Arrays.stream(ex.getRequestURI().getPath().split("/"))
                    .filter(s -> !s.isEmpty()).toList();
            route(ex, segments);
        } catch (ApiException e) {
            send(ex, e.getStatus(), Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(ex, 400, Map.of("error", e.getMessage()));
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Database error", e);
            send(ex, 500, Map.of("error", "Database error"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown error", e);
            send(ex, 500, Map.of("error", "Internal server error"));
        } finally {
            ex.close();
        }
    }


    protected void send(HttpExchange ex, int status, Object body) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", " Content-Type, Authorization");
        byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected static ApiException routeNotFound() {
        return ApiException.notFound("Route not found");
    }

    protected Map<String, Object> readBody(HttpExchange ex) throws IOException {
        String raw = new String(ex.getRequestBody().readNBytes(MAX_BODY_BYTES), StandardCharsets.UTF_8);
        if (raw.isBlank()) throw ApiException.badRequest("Request body is required");
        return JsonUtil.parseObject(raw);
    }

    protected static int parseId(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid id: " + text);
        }
    }

    protected static String requireString(Map<String, Object> body, String key) {
        if (body.get(key) instanceof String s && !s.isBlank()) return s;
        throw ApiException.badRequest("'" + key + "' is required and must be a non-empty string");
    }

    protected static double requireDouble(Map<String, Object> body, String key) {
        if (body.get(key) instanceof Number n) return n.doubleValue();
        throw ApiException.badRequest("'" + key + "' is required and must be a number");
    }

    protected static int requireInt(Map<String, Object> body, String key) {
        if (body.get(key) instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) return (int) d;
        }
        throw ApiException.badRequest("'" + key + "' is required and must be a whole number");
    }
}