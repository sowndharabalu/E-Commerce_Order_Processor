package controller;

import com.sun.net.httpserver.HttpExchange;
import service.OrderService;
import util.ApiException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderController extends BaseController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void route(HttpExchange ex, List<String> seg) throws IOException {
        String method = ex.getRequestMethod();
        String root = seg.get(1);

        if (root.equals("users")) {
            if (seg.size() == 4 && method.equals("GET") && seg.get(3).equals("orders")) {
                send(ex, 200, orderService.getUserOrders(parseId(seg.get(2))));
                return;
            }
            throw routeNotFound();
        }

        if (seg.size() == 2 && method.equals("POST")) {
            Map<String, Object> body = readBody(ex);
            int userId = requireInt(body, "userId");
            if (!(body.get("items") instanceof List<?> list) || list.isEmpty())
                throw ApiException.badRequest("'items' must be a non-empty array");

            Map<Integer, Integer> quantities = new LinkedHashMap<>();
            list.forEach(o -> {
                if (!(o instanceof Map<?, ?>))
                    throw ApiException.badRequest("Each item must be an object {productId, quantity}");
                Map<String, Object> item = (Map<String, Object>) o;
                quantities.merge(requireInt(item, "productId"), requireInt(item, "quantity"), Integer::sum);
            });
            send(ex, 201, orderService.createOrder(userId, quantities));

        } else if (seg.size() == 3 && method.equals("GET")) {
            send(ex, 200, orderService.getOrder(parseId(seg.get(2))));

        } else if (seg.size() == 4 && method.equals("PUT") && seg.get(3).equals("confirm")) {
            send(ex, 200, orderService.confirmOrder(parseId(seg.get(2))));

        } else if (seg.size() == 4 && method.equals("PUT") && seg.get(3).equals("cancel")) {
            send(ex, 200, orderService.cancelOrder(parseId(seg.get(2))));

        } else {
            throw routeNotFound();
        }
    }
}