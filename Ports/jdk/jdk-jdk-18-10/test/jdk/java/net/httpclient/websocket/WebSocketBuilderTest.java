/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8159053
 * @run testng/othervm WebSocketBuilderTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertThrows;

/*
 * In some places in this test a new String is created out of a string literal.
 * The idea is to make sure the code under test relies on something better than
 * the reference equality ( == ) for string equality checks.
 */
public final class WebSocketBuilderTest {

    private final static URI VALID_URI = URI.create("ws://websocket.example.com");

    @Test
    public void nullArguments() {
        HttpClient c = HttpClient.newHttpClient();

        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .buildAsync(null, listener()));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .buildAsync(VALID_URI, null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .buildAsync(null, null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .header(null, "value"));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .header("name", null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .header(null, null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols(null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols(null, "sub2.example.com"));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols("sub1.example.com", (String) null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols("sub1.example.com", (String[]) null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols("sub1.example.com", "sub2.example.com", null));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .subprotocols("sub1.example.com", null, "sub3.example.com"));
        assertThrows(NullPointerException.class,
                     () -> c.newWebSocketBuilder()
                             .connectTimeout(null));
    }

    @Test(dataProvider = "badURIs")
    void illegalURI(URI uri) {
        WebSocket.Builder b = HttpClient.newHttpClient().newWebSocketBuilder();
        assertFails(IllegalArgumentException.class,
                    b.buildAsync(uri, listener()));
    }

    @Test
    public void illegalHeaders() {
        List<String> headers =
                List.of("Sec-WebSocket-Accept",
                        "Sec-WebSocket-Extensions",
                        "Sec-WebSocket-Key",
                        "Sec-WebSocket-Protocol",
                        "Sec-WebSocket-Version")
                        .stream()
                        .flatMap(s -> Stream.of(s, new String(s))) // a string and a copy of it
                        .collect(Collectors.toList());

        Function<String, CompletionStage<?>> f =
                header -> HttpClient.newHttpClient()
                        .newWebSocketBuilder()
                        .header(header, "value")
                        .buildAsync(VALID_URI, listener());

        headers.forEach(h -> assertFails(IllegalArgumentException.class, f.apply(h)));
    }

    // TODO: test for bad syntax headers
    // TODO: test for overwrites (subprotocols) and additions (headers)

    @Test(dataProvider = "badSubprotocols")
    public void illegalSubprotocolsSyntax(String s) {
        WebSocket.Builder b = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .subprotocols(s);
        assertFails(IllegalArgumentException.class,
                    b.buildAsync(VALID_URI, listener()));
    }

    @Test(dataProvider = "duplicatingSubprotocols")
    public void illegalSubprotocolsDuplicates(String mostPreferred,
                                              String[] lesserPreferred) {
        WebSocket.Builder b = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .subprotocols(mostPreferred, lesserPreferred);
        assertFails(IllegalArgumentException.class,
                    b.buildAsync(VALID_URI, listener()));
    }

    @Test(dataProvider = "badConnectTimeouts")
    public void illegalConnectTimeout(Duration d) {
        WebSocket.Builder b = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(d);
        assertFails(IllegalArgumentException.class,
                    b.buildAsync(VALID_URI, listener()));
    }

    @DataProvider
    public Object[][] badURIs() {
        return new Object[][]{
                {URI.create("http://example.com")},
                {URI.create("ftp://example.com")},
                {URI.create("wss://websocket.example.com/hello#fragment")},
                {URI.create("ws://websocket.example.com/hello#fragment")},
        };
    }

    @DataProvider
    public Object[][] badConnectTimeouts() {
        return new Object[][]{
                {Duration.ofDays(0)},
                {Duration.ofDays(-1)},
                {Duration.ofHours(0)},
                {Duration.ofHours(-1)},
                {Duration.ofMinutes(0)},
                {Duration.ofMinutes(-1)},
                {Duration.ofSeconds(0)},
                {Duration.ofSeconds(-1)},
                {Duration.ofMillis(0)},
                {Duration.ofMillis(-1)},
                {Duration.ofNanos(0)},
                {Duration.ofNanos(-1)},
                {Duration.ZERO},
        };
    }

    // https://tools.ietf.org/html/rfc7230#section-3.2.6
    // https://tools.ietf.org/html/rfc20
    @DataProvider
    public static Object[][] badSubprotocols() {
        return new Object[][]{
                {""},
                {new String("")},
                {"round-brackets("},
                {"round-brackets)"},
                {"comma,"},
                {"slash/"},
                {"colon:"},
                {"semicolon;"},
                {"angle-brackets<"},
                {"angle-brackets>"},
                {"equals="},
                {"question-mark?"},
                {"at@"},
                {"brackets["},
                {"backslash\\"},
                {"brackets]"},
                {"curly-brackets{"},
                {"curly-brackets}"},
                {"space "},
                {"non-printable-character " + Character.toString((char) 31)},
                {"non-printable-character " + Character.toString((char) 127)},
        };
    }

    @DataProvider
    public static Object[][] duplicatingSubprotocols() {
        return new Object[][]{
                {"a.b.c", new String[]{"a.b.c"}},
                {"a.b.c", new String[]{"x.y.z", "p.q.r", "x.y.z"}},
                {"a.b.c", new String[]{new String("a.b.c")}},
        };
    }

    private static WebSocket.Listener listener() {
        return new WebSocket.Listener() { };
    }

    /* shortcut */
    public static void assertFails(Class<? extends Throwable> clazz,
                                   CompletionStage<?> stage) {
        Support.assertCompletesExceptionally(clazz, stage);
    }
}
