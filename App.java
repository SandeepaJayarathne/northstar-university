package edu.northstar;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small Java web server backed by PostgreSQL. */
public class App {
    private static final Path PUBLIC_DIR = Paths.get(".").toAbsolutePath().normalize();
    private static final Pattern FIELD = Pattern.compile("\\\"(name|email|interest)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final String DB_URL = databaseUrl();

    public static void main(String[] args) throws IOException {
        if (DB_URL == null || DB_URL.isBlank()) {
            throw new IllegalStateException("Set DATABASE_URL to your PostgreSQL connection URL before starting the app.");
        }
        initializeDatabase();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/applications", App::handleApplication);
        server.createContext("/", App::serveFile);
        server.setExecutor(null);
        server.start();
        System.out.println("Northstar University is running on port " + port);
    }

    private static String databaseUrl() {
        String url = System.getenv("DATABASE_URL");
        if (url == null) return null;
        if (url.startsWith("postgres://")) return "jdbc:" + url.replaceFirst("^postgres://", "postgresql://");
        if (url.startsWith("postgresql://")) return "jdbc:" + url;
        return url;
    }

    private static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS admissions (" +
                "id BIGSERIAL PRIMARY KEY, submitted_at TIMESTAMPTZ NOT NULL, " +
                "full_name TEXT NOT NULL, email TEXT NOT NULL, study_interest TEXT NOT NULL)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            System.out.println("PostgreSQL connection ready; admissions table is available.");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to PostgreSQL. Check DATABASE_URL and database status.", e);
        }
    }

    private static void handleApplication(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"POST required\"}", "application/json; charset=utf-8"); return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String[] fields = new String[3];
        Matcher matcher = FIELD.matcher(body);
        while (matcher.find()) {
            int index = switch (matcher.group(1)) { case "name" -> 0; case "email" -> 1; default -> 2; };
            fields[index] = unescape(matcher.group(2)).trim();
        }
        if (fields[0] == null || fields[0].isBlank() || fields[1] == null || !fields[1].contains("@") || fields[2] == null) {
            send(exchange, 400, "{\"error\":\"Name, valid email and study area are required\"}", "application/json; charset=utf-8"); return;
        }
        String sql = "INSERT INTO admissions (submitted_at, full_name, email, study_interest) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setString(2, fields[0]);
            statement.setString(3, fields[1]);
            statement.setString(4, fields[2]);
            statement.executeUpdate();
            send(exchange, 201, "{\"message\":\"Application interest saved\"}", "application/json; charset=utf-8");
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Could not save application\"}", "application/json; charset=utf-8");
        }
    }

    private static void serveFile(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method not allowed", "text/plain; charset=utf-8"); return;
        }
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) requestPath = "/index.html";
        Path file = PUBLIC_DIR.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(PUBLIC_DIR) || !Files.isRegularFile(file)) {
            send(exchange, 404, "Not found", "text/plain; charset=utf-8"); return;
        }
        String type = file.toString().endsWith(".css") ? "text/css; charset=utf-8" :
                file.toString().endsWith(".js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8";
        byte[] content = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : content.length);
        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) exchange.getResponseBody().write(content);
        exchange.close();
    }

    private static String unescape(String value) { return value.replace("\\\"", "\"").replace("\\\\", "\\"); }
    private static void send(HttpExchange exchange, int status, String body, String type) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
