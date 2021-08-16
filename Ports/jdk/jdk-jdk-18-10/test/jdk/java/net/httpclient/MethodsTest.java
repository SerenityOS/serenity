/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import static java.net.http.HttpClient.Builder.NO_PROXY;

/**
 * @test
 * @bug 8199135
 * @modules java.net.http/jdk.internal.net.http.common
 * @summary Basic test for method names
 */
public class MethodsTest {

    static final URI TEST_URI = URI.create("http://www.foo.com/");
    static final String FORBIDDEN = "()<>@,;:\\\"/[]?={} \t\r\n";
    static final HttpClient client = HttpClient.newBuilder().proxy(NO_PROXY).build();

    static void bad(String name) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.method(name, HttpRequest.BodyPublishers.noBody());
            throw new RuntimeException("Expected IAE for method:" + name);
        } catch (IllegalArgumentException expected) {
            System.out.println("Got expected IAE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return name;
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    return HttpHeaders.of(Map.of(), (x, y) -> true);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected IAE for method:" + name);
        } catch (IllegalArgumentException expected) {
            System.out.println("Got expected IAE: " + expected);
        }
    }

    static void good(String name) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.method(name, HttpRequest.BodyPublishers.noBody());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unexpected IAE for header:" + name);
        }
    }

    public static void main(String[] args) throws Exception {
        bad("bad:method");
        bad("Foo\n");
        good("X-Foo!");
        good("Bar~");
        good("x");
        bad(" ");
        bad("x y");
        bad("x\t");
        bad("Bar\r\n");
        good("Hello#world");
        good("Qwer#ert");
        bad("m\u00e9thode");
        for (char c =0; c < 256 ; c++) {
            if (c < 32 || FORBIDDEN.indexOf(c) > -1 || c >= 127) {
                bad("me" + c + "thod");
                bad(c + "thod");
                bad("me" + c);
            } else {
                good("me" + c + "thod");
                good(c + "thod");
                good("me" + c);
            }
        }
    }
}
