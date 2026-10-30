package util;

import model.Order;
import model.OrderItem;
import model.Product;
import model.User;

import java.time.LocalDateTime;
import java.util.*;

public class JsonUtil {
    private JsonUtil() {
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        switch (o) {
            case null -> sb.append("null");
            case String s -> writeString(sb, s);
            case Object v when v instanceof Boolean || v instanceof Number -> sb.append(v);
            case Enum<?> e -> writeString(sb, e.name());
            case LocalDateTime t -> writeString(sb, t.toString());
            case User u -> write(sb, userMap(u));
            case Product p -> write(sb, productMap(p));
            case Order ord -> write(sb, orderMap(ord));
            case OrderItem i -> write(sb, itemMap(i));
            case Map<?, ?> m -> {
                sb.append("{");
                boolean first = true;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (!first) sb.append(",");
                    first = false;
                    writeString(sb, String.valueOf(e.getKey()));
                    sb.append(":");
                    write(sb, e.getValue());
                }
                sb.append("}");
            }
            case Collection<?> c -> {
                sb.append("[");
                boolean first = true;
                for (Object item : c) {
                    if (!first) sb.append(",");
                    first = false;
                    write(sb, item);
                }
                sb.append("]");
            }
            default -> writeString(sb, o.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static Map<String, Object> userMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.id());
        map.put("username", user.username());
        map.put("email", user.email());
        map.put("createdAt", user.createdAt());
        return map;
    }

    private static Map<String, Object> productMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.id());
        map.put("name", product.name());
        map.put("description", product.description());
        map.put("price", product.price());
        map.put("stock", product.stock());
        map.put("category", product.category());
        map.put("createdAt", product.createdAt());
        return map;
    }

    private static Map<String, Object> orderMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("totalAmount", order.getTotalAmount());
        map.put("status", order.getStatus());
        map.put("createdAt", order.getCreatedAt());
        map.put("items", order.getItems());
        return map;
    }

    private static Map<String, Object> itemMap(OrderItem orderItem) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", orderItem.id());
        map.put("productId", orderItem.productId());
        map.put("quantity", orderItem.quantity());
        map.put("unitPrice", orderItem.unitPrice());
        map.put("totalPrice", orderItem.getTotalPrice());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object v = new Parser(json).parseDocument();
        if (!(v instanceof Map)) throw new IllegalArgumentException("Invalid json: expecting an object");
        return (Map<String, Object>) v;
    }

    private static final class Parser {
        private static final int MAX_DEPTH = 64;
        private final String s;
        private int i;
        private int depth;

        Parser(String s) {
            this.s = s;
        }

        Object parseDocument() {
            Object v = value();
            ws();
            if (i != s.length()) throw new IllegalArgumentException("unexpected end of input");
            return v;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private IllegalArgumentException err(String msg) {
            return new IllegalArgumentException("Invalid json " + msg + " at index " + i);
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private void expect(char c) {
            if (peek() != c) {
                throw err("Expected " + c + "'");
            }
        }

        private Object value() {
            ws();
            if (i >= s.length()) throw new IllegalArgumentException("unexpected end of input");
            return switch (s.charAt(i)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Object literal(String word, Object v) {
            if (!s.startsWith(word, i)) throw err("unexpexted token");
            i += word.length();
            return v;
        }

        private Map<String, Object> object() {
            if (++depth > MAX_DEPTH) throw err("nesting too deep");
            expect('{');
            Map<String, Object> map = new HashMap<>();
            ws();
            if (peek() == '}') {
                i++;
                depth--;
                return map;
            }
            while (true) {
                ws();
                if (peek() != '"') throw err("expected string key");
                String key = string();
                ws();
                expect(':');
                map.put(key, value());
                ws();
                if (peek() == ',') {
                    i++;
                    continue;
                }
                expect('}');
                break;
            }
            return map;
        }

        private List<Object> array() {
            if (++depth > MAX_DEPTH) throw err("nesting too deep");
            expect('[');
            List<Object> list = new ArrayList<>();
            ws();
            if (peek() == ']') {
                i++;
                depth--;
                return list;
            }
            while (true) {
                list.add(value());
                ws();
                if (peek() == ',') {
                    i++;
                    continue;
                }
                expect(']');
                break;
            }
            depth--;
            return list;
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw err("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (i >= s.length()) throw err("unterminated escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw err("bad unicode escape");
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            } catch (NumberFormatException ex) {
                                throw err("bad unicode escape");
                            }
                            i += 4;
                        }
                        default -> throw err("bad escape");
                    }
                } else if (c < 0x20) {
                    throw err("control character in string");
                } else {
                    sb.append(c);
                }
            }
        }

        private Double number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw err("unexpected character '" + s.charAt(i) + "'");
            try {
                return Double.parseDouble(s.substring(start, i));
            } catch (NumberFormatException e) {
                i = start;
                throw err("bad number");
            }
        }
    }
}

