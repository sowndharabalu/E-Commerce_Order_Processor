package server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApiServer {
    private final HttpServer server;

    public ApiServer(int port, HttpHandler userHandler, HttpHandler productHandler,
                     HttpHandler orderHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/users", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.matches("/api/users/\\d+/orders")) {
                orderHandler.handle(exchange);
            } else {
                userHandler.handle(exchange);
            }
        });
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"UP\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.createContext("/api/products", productHandler);
        server.createContext("/api/orders", orderHandler);

        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }
}