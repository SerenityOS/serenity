/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * THE CONTENTS OF THIS FILE HAVE TO BE IN SYNC WITH THE EXAMPLES USED IN THE
 * JAVADOC.
 *
 * @test
 * @compile JavadocExamples.java
 */
public class JavadocExamples {
    HttpRequest request = null;
    HttpClient client = null;
    Pattern p = null;

    void fromHttpClientClasslevelDescription() throws Exception {
        //Synchronous Example
        HttpClient client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
                .authenticator(Authenticator.getDefault())
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());

        //Asynchronous Example
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://foo.com/"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofFile(Paths.get("file.json")))
                .build();
        client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println);
    }

    void fromHttpRequest() throws Exception {
        // HttpRequest class-level description
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://foo.com/"))
                .build();
        client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();

        // HttpRequest.BodyPublishers class-level description
        // Request body from a String
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("https://foo.com/"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(BodyPublishers.ofString("some body text"))
                .build();

        // Request body from a File
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("https://foo.com/"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofFile(Paths.get("file.json")))
                .build();

        // Request body from a byte array
        HttpRequest request3 = HttpRequest.newBuilder()
                .uri(URI.create("https://foo.com/"))
                .POST(BodyPublishers.ofByteArray(new byte[] { /*...*/ }))
                .build();

        // HttpRequest.Builder
        // API note - newBuilder(HttpRequest, BiPredicate<String, String>)
        // Retain all headers:
        HttpRequest.newBuilder(request, (n, v) -> true);

        //Remove all headers:
        HttpRequest.newBuilder(request, (n, v) -> false);

        // Remove a particular header (e.g. Foo-Bar):
        HttpRequest.newBuilder(request, (name, value) ->
                !name.equalsIgnoreCase("Foo-Bar"));
    }

    void fromHttpResponse() throws Exception {
        // HttpResponse class-level description
        HttpResponse<String> response = client
                .send(request, BodyHandlers.ofString());

        // HttpResponse.BodyHandler class-level description
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://www.foo.com/"))
                .build();
        client.sendAsync(request, BodyHandlers.ofFile(Paths.get("/tmp/f")))
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://www.foo.com/"))
                .build();
        BodyHandler<Path> bodyHandler = (info) -> info.statusCode() == 200
                ? BodySubscribers.ofFile(Paths.get("/tmp/f"))
                : BodySubscribers.replacing(Paths.get("/NULL"));
        client.sendAsync(request, bodyHandler)
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println);


        // HttpResponse.BodyHandlers class-level description
        // Receives the response body as a String
        HttpResponse<String> response1 = client
                .send(request, BodyHandlers.ofString());

        // Receives the response body as a file
        HttpResponse<Path> response2 = client
                .send(request, BodyHandlers.ofFile(Paths.get("example.html")));

        // Receives the response body as an InputStream
        HttpResponse<InputStream> respons3 = client
                .send(request, BodyHandlers.ofInputStream());

        // Discards the response body
        HttpResponse<Void> respons4 = client
                .send(request, BodyHandlers.discarding());


        // HttpResponse.BodySubscribers class-level description
        // Streams the response body to a File
        HttpResponse<Path> response5 = client
                .send(request, responseInfo -> BodySubscribers.ofFile(Paths.get("example.html")));

        // Accumulates the response body and returns it as a byte[]
        HttpResponse<byte[]> response6 = client
                .send(request, responseInfo -> BodySubscribers.ofByteArray());

        // Discards the response body
        HttpResponse<Void> response7 = client
                .send(request, responseInfo -> BodySubscribers.discarding());

        // Accumulates the response body as a String then maps it to its bytes
        HttpResponse<byte[]> response8 = client
                .send(request, responseInfo ->
                        BodySubscribers.mapping(BodySubscribers.ofString(UTF_8), String::getBytes));

        // Maps an InputStream to a Foo object.
        HttpResponse<Supplier<Foo>> response9 = client.send(request,
                (resp) -> FromMappingSubscriber.asJSON(Foo.class));
        String resp = response9.body().get().asString();

    }

    /**
     * @apiNote This method can be used as an adapter between a {@code
     * BodySubscriber} and a text based {@code Flow.Subscriber} that parses
     * text line by line.
     *
     * <p> For example:
     * <pre> {@code  // A PrintSubscriber that implements Flow.Subscriber<String>
     *  // and print lines received by onNext() on System.out
     *  PrintSubscriber subscriber = new PrintSubscriber(System.out);
     *  client.sendAsync(request, BodyHandlers.fromLineSubscriber(subscriber))
     *      .thenApply(HttpResponse::statusCode)
     *      .thenAccept((status) -> {
     *          if (status != 200) {
     *              System.err.printf("ERROR: %d status received%n", status);
     *          }
     *      }); }</pre>
     */
    void fromLineSubscriber1() {
         // A PrintSubscriber that implements Flow.Subscriber<String>
         // and print lines received by onNext() on System.out
         PrintSubscriber subscriber = new PrintSubscriber(System.out);
         client.sendAsync(request, BodyHandlers.fromLineSubscriber(subscriber))
                 .thenApply(HttpResponse::statusCode)
                 .thenAccept((status) -> {
                     if (status != 200) {
                         System.err.printf("ERROR: %d status received%n", status);
                     }
                 });
    }

    /**
     * @apiNote This method can be used as an adapter between a {@code
     * BodySubscriber} and a text based {@code Flow.Subscriber} that parses
     * text line by line.
     *
     * <p> For example:
     * <pre> {@code  // A LineParserSubscriber that implements Flow.Subscriber<String>
     *  // and accumulates lines that match a particular pattern
     *  Pattern pattern = ...;
     *  LineParserSubscriber subscriber = new LineParserSubscriber(pattern);
     *  HttpResponse<List<String>> response = client.send(request,
     *      BodyHandlers.fromLineSubscriber(subscriber, s -> s.getMatchingLines(), "\n"));
     *  if (response.statusCode() != 200) {
     *      System.err.printf("ERROR: %d status received%n", response.statusCode());
     *  } }</pre>
     *
     */
    void fromLineSubscriber2() throws IOException, InterruptedException {
        // A LineParserSubscriber that implements Flow.Subscriber<String>
        // and accumulates lines that match a particular pattern
        Pattern pattern = p;
        LineParserSubscriber subscriber = new LineParserSubscriber(pattern);
        HttpResponse<List<String>> response = client.send(request,
                BodyHandlers.fromLineSubscriber(subscriber, s -> s.getMatchingLines(), "\n"));
        if (response.statusCode() != 200) {
            System.err.printf("ERROR: %d status received%n", response.statusCode());
        }
    }

    static final class PrintSubscriber implements Flow.Subscriber<String> {
        final PrintStream out;
        PrintSubscriber(PrintStream out) {
            this.out = out;
        }
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        @Override
        public void onNext(String item) {
            out.println(item);
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }
        @Override
        public void onComplete() {
        }
    }

    static final class LineParserSubscriber implements Flow.Subscriber<String> {
        final Pattern pattern;
        final CopyOnWriteArrayList<String> matches = new CopyOnWriteArrayList<>();
        LineParserSubscriber(Pattern pattern) {
            this.pattern = pattern;
        }
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        @Override
        public void onNext(String item) {
            if (pattern.matcher(item).matches()) {
                matches.add(item);
            }
        }
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }
        @Override
        public void onComplete() {
        }

        public List<String> getMatchingLines() {
            return Collections.unmodifiableList(matches);
        }
    }

    public static class Foo {
        byte[] bytes;
        public Foo(byte[] bytes) {
            this.bytes = bytes;
        }
        public String asString() {
            return new String(bytes, UTF_8);
        }
    }

    static class ObjectMapper {
        <W> W readValue(InputStream is, Class<W> targetType)
            throws IOException
        {
                byte[] bytes = is.readAllBytes();
                return map(bytes, targetType);
        }

        static <W> W map(byte[] bytes, Class<W> targetType) {
            try {
                return targetType.getConstructor(byte[].class).newInstance(bytes);
            } catch (RuntimeException | Error x) {
                throw x;
            } catch (Exception x) {
                throw new UndeclaredThrowableException(x);
            }
        }
    }

    static class FromMappingSubscriber {
        public static <W> BodySubscriber<Supplier<W>> asJSON(Class<W> targetType) {
            BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();

            BodySubscriber<Supplier<W>> downstream = BodySubscribers.mapping(
                    upstream, (InputStream is) -> () -> {
                        try (InputStream stream = is) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            return objectMapper.readValue(stream, targetType);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            return downstream;
        }

    }

}
