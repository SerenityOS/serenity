/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import static java.lang.System.err;

/*
 * @test
 * @bug 8187503
 * @summary An example on how to read a response body with InputStream.
 * @run main/othervm/manual -Dtest.debug=true BodyProcessorInputStreamTest
 * @author daniel fuchs
 */
public class BodyProcessorInputStreamTest {

    public static boolean DEBUG = Boolean.getBoolean("test.debug");

    /**
     * Examine the response headers to figure out the charset used to
     * encode the body content.
     * If the content type is not textual, returns an empty Optional.
     * Otherwise, returns the body content's charset, defaulting to
     * ISO-8859-1 if none is explicitly specified.
     * @param headers The response headers.
     * @return The charset to use for decoding the response body, if
     *         the response body content is text/...
     */
    public static Optional<Charset> getCharset(HttpHeaders headers) {
        Optional<String> contentType = headers.firstValue("Content-Type");
        Optional<Charset> charset = Optional.empty();
        if (contentType.isPresent()) {
            final String[] values = contentType.get().split(";");
            if (values[0].startsWith("text/")) {
                charset = Optional.of(Stream.of(values)
                    .map(x -> x.toLowerCase(Locale.ROOT))
                    .map(String::trim)
                    .filter(x -> x.startsWith("charset="))
                    .map(x -> x.substring("charset=".length()))
                    .findFirst()
                    .orElse("ISO-8859-1"))
                    .map(Charset::forName);
            }
        }
        return charset;
    }

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
            .newBuilder(new URI("http://hg.openjdk.java.net/jdk9/sandbox/jdk/shortlog/http-client-branch/"))
            .GET()
            .build();

        // This example shows how to return an InputStream that can be used to
        // start reading the response body before the response is fully received.
        // In comparison, the snipet below (which uses
        // HttpResponse.BodyHandlers.ofString()) obviously will not return before the
        // response body is fully read:
        //
        // System.out.println(
        //    client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body());

        CompletableFuture<HttpResponse<InputStream>> handle =
            client.sendAsync(request, BodyHandlers.ofInputStream());
        if (DEBUG) err.println("Request sent");

        HttpResponse<InputStream> pending = handle.get();

        // At this point, the response headers have been received, but the
        // response body may not have arrived yet. This comes from
        // the implementation of HttpResponseInputStream::getBody above,
        // which returns an already completed completion stage, without
        // waiting for any data.
        // We can therefore access the headers - and the body, which
        // is our live InputStream, without waiting...
        HttpHeaders responseHeaders = pending.headers();

        // Get the charset declared in the response headers.
        // The optional will be empty if the content type is not
        // of type text/...
        Optional<Charset> charset = getCharset(responseHeaders);

        try (InputStream is = pending.body();
            // We assume a textual content type. Construct an InputStream
            // Reader with the appropriate Charset.
            // charset.get() will throw NPE if the content is not textual.
            Reader r = new InputStreamReader(is, charset.get())) {

            char[] buff = new char[32];
            int off=0, n=0;
            if (DEBUG) err.println("Start receiving response body");
            if (DEBUG) err.println("Charset: " + charset.get());

            // Start consuming the InputStream as the data arrives.
            // Will block until there is something to read...
            while ((n = r.read(buff, off, buff.length - off)) > 0) {
                assert (buff.length - off) > 0;
                assert n <= (buff.length - off);
                if (n == (buff.length - off)) {
                    System.out.print(buff);
                    off = 0;
                } else {
                    off += n;
                }
                assert off < buff.length;
            }

            // last call to read may not have filled 'buff' completely.
            // flush out the remaining characters.
            assert off >= 0 && off < buff.length;
            for (int i=0; i < off; i++) {
                System.out.print(buff[i]);
            }

            // We're done!
            System.out.println("Done!");
        }
    }

}
