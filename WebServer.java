import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

// ============================================================
// WebServer.java
// Lightweight HTTP server that wraps the existing backend
// and serves the HTML frontend on localhost:8080
// ============================================================

public class WebServer {

    private static TransportService service;
    private static int demandArrivalCounter = 1;

    public static void main(String[] args) throws Exception {

        // Initialize the shared backend service
        service = new TransportService();
        service.loadFromDatabase();
        if (service.getStops().isEmpty() && service.getBuses().isEmpty()) {
            loadSampleData();
        }

        // Create HTTP server on port 8080
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        // Serve frontend
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/stops",     new StopsHandler());
        server.createContext("/api/buses",     new BusesHandler());
        server.createContext("/api/demands",   new DemandsHandler());
        server.createContext("/api/allocate",  new AllocateHandler());
        server.createContext("/api/complete",  new CompleteHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("===========================================");
        System.out.println("  Server running at http://localhost:8080");
        System.out.println("===========================================");
        System.out.println("  Open the above URL in your browser.");
        System.out.println("  Press Ctrl+C to stop the server.");
        System.out.println("===========================================");
    }

    // ---------------------------------------------------------
    // SAMPLE DATA (same as Main.java)
    // ---------------------------------------------------------
    private static void loadSampleData() {

        service.addStop(new Stop(1, "Central Bus Stand"));
        service.addStop(new Stop(2, "Market"));
        service.addStop(new Stop(3, "College"));
        service.addStop(new Stop(4, "Hospital"));
        service.addStop(new Stop(5, "Railway Station"));

        service.addBus(new MiniBus(101, 15));
        service.addBus(new StandardBus(102, 30));
        service.addBus(new StandardBus(103, 40));

        service.addDemand(new DemandRequest(
            1, service.getStop(1), service.getStop(3), 20, demandArrivalCounter++
        ));
        service.addDemand(new DemandRequest(
            2, service.getStop(2), service.getStop(5), 10, demandArrivalCounter++
        ));
    }

    // ---------------------------------------------------------
    // HELPER: Send JSON response
    // ---------------------------------------------------------
    private static void sendJson(HttpExchange ex, int code, String json)
            throws IOException {

        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // ---------------------------------------------------------
    // HELPER: Read request body
    // ---------------------------------------------------------
    private static String readBody(HttpExchange ex) throws IOException {

        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1) buf.write(b);
        return buf.toString("UTF-8");
    }

    // ---------------------------------------------------------
    // HELPER: Parse simple JSON key-value (no library needed)
    // ---------------------------------------------------------
    private static Map<String, String> parseJson(String json) {

        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    // ---------------------------------------------------------
    // HELPER: Capture console output
    // ---------------------------------------------------------
    private static String captureOutput(Runnable action) {

        PrintStream orig = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try { action.run(); }
        finally { System.setOut(orig); }
        return baos.toString().trim();
    }

    // =========================================================
    // STATIC FILE HANDLER (serves index.html)
    // =========================================================
    static class StaticFileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File("." + path);

            if (file.exists() && file.isFile()) {
                String contentType = "text/html";
                if (path.endsWith(".css"))  contentType = "text/css";
                if (path.endsWith(".js"))   contentType = "application/javascript";
                if (path.endsWith(".png"))  contentType = "image/png";
                if (path.endsWith(".jpg"))  contentType = "image/jpeg";

                ex.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(file.toPath());
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
                ex.getResponseBody().close();
            } else {
                String msg = "404 Not Found";
                ex.sendResponseHeaders(404, msg.length());
                ex.getResponseBody().write(msg.getBytes());
                ex.getResponseBody().close();
            }
        }
    }

    // =========================================================
    // DASHBOARD HANDLER
    // =========================================================
    static class DashboardHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            int totalStops = service.getStops().size();
            int totalBuses = service.getBuses().size();
            int availBuses = 0;
            for (Bus b : service.getBuses().values()) {
                if (b.isAvailable()) availBuses++;
            }
            int waitingDemands = service.getDemandQueue().size();

            String json = "{" +
                "\"totalStops\":" + totalStops + "," +
                "\"totalBuses\":" + totalBuses + "," +
                "\"availableBuses\":" + availBuses + "," +
                "\"waitingDemands\":" + waitingDemands +
                "}";

            sendJson(ex, 200, json);
        }
    }

    // =========================================================
    // STOPS HANDLER
    // =========================================================
    static class StopsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            if (ex.getRequestMethod().equalsIgnoreCase("GET")) {

                StringBuilder sb = new StringBuilder("[");
                List<Stop> sorted = new ArrayList<>(service.getStops().values());
                sorted.sort(Comparator.comparingInt(Stop::getStopId));

                for (int i = 0; i < sorted.size(); i++) {
                    Stop s = sorted.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"id\":").append(s.getStopId())
                      .append(",\"name\":\"").append(escapeJson(s.getStopName()))
                      .append("\"}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());

            } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

                Map<String, String> data = parseJson(readBody(ex));
                try {
                    int id = Integer.parseInt(data.get("id"));
                    String name = data.get("name");

                    if (name == null || name.isEmpty()) {
                        sendJson(ex, 400, "{\"error\":\"Stop name is required.\"}");
                        return;
                    }
                    if (service.getStop(id) != null) {
                        sendJson(ex, 400, "{\"error\":\"Stop ID already exists.\"}");
                        return;
                    }

                    service.addStop(new Stop(id, name));
                    sendJson(ex, 200, "{\"message\":\"Stop added successfully.\"}");

                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Invalid stop ID.\"}");
                }
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    // =========================================================
    // BUSES HANDLER
    // =========================================================
    static class BusesHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            if (ex.getRequestMethod().equalsIgnoreCase("GET")) {

                StringBuilder sb = new StringBuilder("[");
                List<Bus> sorted = new ArrayList<>(service.getBuses().values());
                sorted.sort(Comparator.comparingInt(Bus::getBusId));

                for (int i = 0; i < sorted.size(); i++) {
                    Bus b = sorted.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"id\":").append(b.getBusId())
                      .append(",\"type\":\"").append(b.getBusType())
                      .append("\",\"capacity\":").append(b.getCapacity())
                      .append(",\"available\":").append(b.isAvailable())
                      .append("}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());

            } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

                Map<String, String> data = parseJson(readBody(ex));
                try {
                    int id       = Integer.parseInt(data.get("id"));
                    int capacity = Integer.parseInt(data.get("capacity"));
                    String type  = data.get("type");

                    if (capacity <= 0) {
                        sendJson(ex, 400, "{\"error\":\"Capacity must be positive.\"}");
                        return;
                    }
                    if (service.getBuses().containsKey(id)) {
                        sendJson(ex, 400, "{\"error\":\"Bus ID already exists.\"}");
                        return;
                    }

                    Bus bus;
                    if ("MiniBus".equals(type)) {
                        bus = new MiniBus(id, capacity);
                    } else {
                        bus = new StandardBus(id, capacity);
                    }

                    service.addBus(bus);
                    sendJson(ex, 200, "{\"message\":\"Bus added successfully.\"}");

                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Invalid bus ID or capacity.\"}");
                }
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    // =========================================================
    // DEMANDS HANDLER
    // =========================================================
    static class DemandsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            if (ex.getRequestMethod().equalsIgnoreCase("GET")) {

                StringBuilder sb = new StringBuilder("[");
                PriorityQueue<DemandRequest> copy =
                    new PriorityQueue<>(service.getDemandQueue());

                int priority = 1;
                boolean first = true;
                while (!copy.isEmpty()) {
                    DemandRequest d = copy.poll();
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"requestId\":").append(d.getRequestId())
                      .append(",\"pickup\":\"").append(escapeJson(d.getPickupStop().getStopName()))
                      .append("\",\"destination\":\"").append(escapeJson(d.getDestination().getStopName()))
                      .append("\",\"passengers\":").append(d.getPassengers())
                      .append(",\"priority\":").append(priority++)
                      .append("}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());

            } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

                Map<String, String> data = parseJson(readBody(ex));
                try {
                    int requestId  = Integer.parseInt(data.get("requestId"));
                    int pickupId   = Integer.parseInt(data.get("pickupId"));
                    int destId     = Integer.parseInt(data.get("destId"));
                    int passengers = Integer.parseInt(data.get("passengers"));

                    if (passengers <= 0) {
                        sendJson(ex, 400,
                            "{\"error\":\"Passenger count must be positive.\"}");
                        return;
                    }
                    if (service.getDemands().containsKey(requestId)) {
                        sendJson(ex, 400,
                            "{\"error\":\"Request ID already exists.\"}");
                        return;
                    }

                    Stop pickup = service.getStop(pickupId);
                    Stop dest   = service.getStop(destId);

                    if (pickup == null || dest == null) {
                        sendJson(ex, 400, "{\"error\":\"Invalid stop ID.\"}");
                        return;
                    }
                    if (pickupId == destId) {
                        sendJson(ex, 400,
                            "{\"error\":\"Pickup and destination cannot be the same.\"}");
                        return;
                    }

                    DemandRequest demand = new DemandRequest(
                        requestId, pickup, dest, passengers,
                        demandArrivalCounter++
                    );
                    service.addDemand(demand);
                    sendJson(ex, 200,
                        "{\"message\":\"Demand added successfully.\"}");

                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Invalid input values.\"}");
                }
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    // =========================================================
    // ALLOCATE HANDLER
    // =========================================================
    static class AllocateHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

                String output = captureOutput(() -> service.allocateBus());
                String escaped = escapeJson(output);
                sendJson(ex, 200, "{\"message\":\"" + escaped + "\"}");

            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    // =========================================================
    // COMPLETE TRIP HANDLER
    // =========================================================
    static class CompleteHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

                Map<String, String> data = parseJson(readBody(ex));
                try {
                    int busId = Integer.parseInt(data.get("busId"));
                    String output = captureOutput(
                        () -> service.completeBusTrip(busId)
                    );
                    String escaped = escapeJson(output);
                    sendJson(ex, 200, "{\"message\":\"" + escaped + "\"}");

                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Invalid bus ID.\"}");
                }
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    // ---------------------------------------------------------
    // HELPER: Escape JSON string
    // ---------------------------------------------------------
    private static String escapeJson(String s) {

        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
