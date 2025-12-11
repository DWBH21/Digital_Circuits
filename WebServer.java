import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/solve", new SolveHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class SolveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS headers for all responses (preflight and actual)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Preflight support
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                // Read request body
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // Parse JSON (order-agnostic, minimal)
                Map<String, Object> params = parseJson(requestBody);
                int noVars = ((Number) params.get("noVars")).intValue();
                @SuppressWarnings("unchecked")
                List<Integer> onSet = ((List<Number>) params.get("onSet"))
                        .stream().map(Number::intValue).collect(Collectors.toList());
                @SuppressWarnings("unchecked")
                List<Integer> dontCares = ((List<Number>) params.get("dontCares"))
                        .stream().map(Number::intValue).collect(Collectors.toList());

                // Build initial minterms (on-set + DCs) and compute PIs
                List<Minterm> initial = new ArrayList<>();
                for (int m : onSet) initial.add(new Minterm(m, noVars));
                for (int d : dontCares) initial.add(new Minterm(d, noVars));
                List<Minterm> primeImplicants = QM_Minimization.getPrimeImplicants(initial);
                Set<Integer> chosenPIIdx = QM_Minimization.getMinExpression(primeImplicants, onSet, noVars);

                List<Integer> zeroSet = computeZeros(onSet, dontCares, noVars);
                List<Minterm> zeroInitial = new ArrayList<>();
                
                for (int m : zeroSet) zeroInitial.add(new Minterm(m, noVars));
                for (int d : dontCares) zeroInitial.add(new Minterm(d, noVars));
                List<Minterm> zeroPrimeImplicants = QM_Minimization.getPrimeImplicants(zeroInitial);
                Set<Integer> zeroChosenPIIdx = QM_Minimization.getMinExpression(zeroPrimeImplicants, zeroSet, noVars);

                // Build SOP and POS and grouping info
                char[] vars = "ABCDE".toCharArray();
                String sop = toSOP(chosenPIIdx, primeImplicants, vars);
                String pos = toPOS(zeroChosenPIIdx, zeroPrimeImplicants, vars);
                String groupsJson = toGroupsJson(chosenPIIdx, primeImplicants);

                String response = "{\"sop\":\"" + escapeJson(sop) + "\",\"pos\":\"" + escapeJson(pos) + "\",\"groups\":" + groupsJson + "}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String msg = "{\"error\":\"" + escapeJson(e.getMessage() == null ? "Server error" : e.getMessage()) + "\"}";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        List<Integer> computeZeros(List<Integer> onSet, List<Integer> dontCares, int noVars) {
            Set<Integer> allMinterms = new HashSet<>();
            int totalMinterms = 1 << noVars; 
            for (int i = 0; i < totalMinterms; i++) {
                allMinterms.add(i);
            }
            
            allMinterms.removeAll(onSet);
            allMinterms.removeAll(dontCares);
            
            return new ArrayList<>(allMinterms);
        }

        // Minimal JSON parsing for {"noVars":N,"onSet":[...],"dontCares":[...]}
        private Map<String, Object> parseJson(String json) {
            Map<String, Object> map = new HashMap<>();
            String s = json.trim();
            if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length() - 1);

            Integer noVars = extractIntField(s, "\"noVars\"");
            List<Integer> onSet = extractIntArrayField(s, "\"onSet\"");
            List<Integer> dontCares = extractIntArrayField(s, "\"dontCares\"");

            if (noVars == null || onSet == null || dontCares == null) {
                throw new IllegalArgumentException("Invalid JSON payload");
            }
            map.put("noVars", noVars);
            map.put("onSet", onSet);
            map.put("dontCares", dontCares);
            return map;
        }

        private Integer extractIntField(String s, String key) {
            int k = s.indexOf(key);
            if (k < 0) return null;
            int colon = s.indexOf(':', k + key.length());
            if (colon < 0) return null;
            int i = colon + 1;
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            int j = i;
            while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '-')) j++;
            try {
                return Integer.parseInt(s.substring(i, j));
            } catch (Exception e) {
                return null;
            }
        }

        private List<Integer> extractIntArrayField(String s, String key) {
            int k = s.indexOf(key);
            if (k < 0) return null;
            int colon = s.indexOf(':', k + key.length());
            if (colon < 0) return null;
            int lb = s.indexOf('[', colon);
            if (lb < 0) return null;
            int rb = findMatchingBracket(s, lb);
            if (rb < 0) return null;
            String body = s.substring(lb + 1, rb).trim();
            List<Integer> list = new ArrayList<>();
            if (body.isEmpty()) return list;
            for (String tok : body.split(",")) {
                tok = tok.trim();
                if (!tok.isEmpty()) list.add(Integer.parseInt(tok));
            }
            return list;
        }

        private int findMatchingBracket(String s, int lb) {
            int depth = 0;
            for (int i = lb; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
            return -1;
        }

        private String toSOP(Set<Integer> rows, List<Minterm> pis, char[] varNames) {
            List<String> terms = new ArrayList<>();
            for (int r : rows) {
                String p = pis.get(r).s;
                StringBuilder term = new StringBuilder();
                for (int i = 0; i < p.length(); i++) {
                    if (p.charAt(i) == '-') continue;
                    term.append(varNames[i]);
                    if (p.charAt(i) == '0') term.append("'");
                }
                terms.add(term.length() == 0 ? "1" : term.toString());
            }
            return terms.isEmpty() ? "0" : String.join(" + ", terms);
        }

        private String toPOS(Set<Integer> rows, List<Minterm> pis, char[] varNames) {
            List<String> terms = new ArrayList<>();
            for (int r : rows) {
                String p = pis.get(r).s;
                StringBuilder term = new StringBuilder();
                boolean hasVars = false;
                
                for (int i = 0; i < p.length(); i++) {
                    if (p.charAt(i) == '-') continue;
                    
                    if (hasVars) term.append(" + ");
                    
                    if (p.charAt(i) == '0') {
                        term.append(varNames[i]);
                    } else {
                        term.append(varNames[i]).append("'");
                    }
                    hasVars = true;
                }
                
                if (!hasVars) {
                    terms.add("(0)");
                } else {
                    terms.add("(" + term.toString() + ")");
                }
            }
            return terms.isEmpty() ? "1" : String.join(" · ", terms);
        }

        private String toGroupsJson(Set<Integer> rows, List<Minterm> pis) {
            // Return an object mapping pattern -> [original minterms covered]
            List<String> parts = new ArrayList<>();
            for (int r : rows) {
                Minterm pi = pis.get(r);
                String key = "\"" + escapeJson(pi.s) + "\"";
                String vals = pi.mintermNos.stream().sorted()
                        .map(String::valueOf).collect(Collectors.joining(","));
                parts.add(key + ":[" + vals + "]");
            }
            return "{" + String.join(",", parts) + "}";
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}