package jdk.internal.net.http.websocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * This is the client side of the test invoked from WebSocketAndHttpTest:
 *
 * The two args are the addresses of a (local) Websocket and Http server
 *
 * The test first sends a request to the WS server and in the listener
 * which handles the response, it tries to send a request to the http
 * server. This hangs if the listener was invoked from the selector
 * manager thread. If invoked from a different thread then the http
 * response is received and the response string is mapped to string
 * "succeeded"
 */
public class WebSocketAndHttpClient {

    public static void main(String[] args) throws InterruptedException {

        ExecutorService executorService = Executors.newCachedThreadPool();
        HttpClient httpClient = HttpClient.newBuilder().executor(executorService).build();

        WebSocketTest wsTest = new WebSocketTest(httpClient, args[0]);
        HttpTest httpTest = new HttpTest(httpClient, args[1]);

        final CompletableFuture<String> result = new CompletableFuture<>();

        wsTest.listen(message -> {
            try {
                String r = httpTest.getData(message);
                result.complete(r);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        wsTest.sendData("TEST_DATA");

        System.out.println("Wait for result");
        try {
            result.join();
            System.out.println("Result: success");
        } finally {
            executorService.shutdownNow();
        }
    }

    static class WebSocketTest {
        final HttpClient httpClient;
        final String server;
        volatile WebSocket webSocket;

        WebSocketTest(HttpClient httpClient, String server) {
            this.httpClient = httpClient;
            this.server = server;
        }

        public void listen(Consumer<String> consumer) {
            URI uri = URI.create(server);
            System.out.println("WS API client - Connecting to " + uri.toString());
            CompletableFuture<WebSocket> cf = httpClient.newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println("WS API client - received data: " + data);
                        consumer.accept(data.toString());
                        return null;
                    }
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.out.println("WS API client - error");
                        error.printStackTrace();
                    }
                });
            System.out.println("CF created");
            webSocket = cf.join();
            System.out.println("Websocket created");
        }

        void sendData(String data) {
            System.out.println("WS API client - sending data via WebSocket: {}" + data);
            webSocket.sendText(data, true).join();
        }
    }

    static class HttpTest {
        final HttpClient httpClient;
        final String baseUrl;

        HttpTest(HttpClient httpClient, String baseUrl) {
            this.httpClient = httpClient;
            this.baseUrl = baseUrl;
        }

        private String getData(String data) throws Exception {
            URI uri = URI.create(baseUrl + "?param=" + data);
            HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
            System.out.println("Http API Client - send HTTP GET request with parameter {}" + data);
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return send.body();
        }
    }
}
