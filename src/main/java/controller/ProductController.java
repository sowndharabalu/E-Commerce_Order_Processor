package controller;

import com.sun.net.httpserver.HttpExchange;
import model.Enums.Category;
import service.ProductService;
import util.ApiException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProductController extends BaseController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private static Category parseCategory(String text) {
        try {
            return Category.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid category: " + text);
        }
    }

    @Override
    protected void route(HttpExchange ex, List<String> seg) throws IOException {
        String method = ex.getRequestMethod();

        if (seg.size() == 2 && method.equals("GET")) {
            send(ex, 200, productService.getAllProducts());

        } else if (seg.size() == 2 && method.equals("POST")) {
            Map<String, Object> body = readBody(ex);
            String description = body.get("description") instanceof String d ? d : null;
            send(ex, 201, productService.addProduct(
                    requireString(body, "name"),
                    description,
                    requireDouble(body, "price"),
                    requireInt(body, "stock"),
                    parseCategory(requireString(body, "category"))));

        } else if (seg.size() == 4 && method.equals("GET") && seg.get(2).equals("category")) {
            send(ex, 200, productService.getByCategory(parseCategory(seg.get(3))));

        } else if (seg.size() == 3 && method.equals("GET")) {
            send(ex, 200, productService.getProduct(parseId(seg.get(2))));

        } else if (seg.size() == 4 && method.equals("PUT") && seg.get(3).equals("stock")) {
            Map<String, Object> body = readBody(ex);
            send(ex, 200, productService.updateStock(parseId(seg.get(2)), requireInt(body, "stock")));

        } else {
            throw routeNotFound();
        }
    }
}