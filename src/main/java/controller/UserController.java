package controller;

import com.sun.net.httpserver.HttpExchange;
import model.User;
import service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UserController extends BaseController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void route(HttpExchange ex, List<String> seg) throws IOException {
        String method = ex.getRequestMethod();

        if (seg.size() == 3 && method.equals("POST") && seg.get(2).equals("register")) {
            Map<String, Object> body = readBody(ex);
            User user = userService.register(
                    requireString(body, "username"),
                    requireString(body, "email"),
                    requireString(body, "password"));
            send(ex, 201, Map.of("id", user.id(), "username", user.username(), "email", user.email(), "createdAt", user.createdAt()));

        } else if (seg.size() == 3 && method.equals("POST") && seg.get(2).equals("login")) {
            Map<String, Object> body = readBody(ex);
            User user = userService.login(requireString(body, "username"), requireString(body, "password"));
            send(ex, 200, Map.of("message", "Login successful", "user", user));

        } else if (seg.size() == 3 && method.equals("GET")) {
            send(ex, 200, userService.getUser(parseId(seg.get(2))));

        } else {
            throw routeNotFound();
        }
    }
}