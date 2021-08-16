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

/*
 * @test
 * @summary Basic test for ofFileDownload
 * @bug 8196965
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @build jdk.test.lib.Platform
 * @build jdk.test.lib.util.FileUtils
 * @run testng/othervm AsFileDownloadTest
 * @run testng/othervm/java.security.policy=AsFileDownloadTest.policy AsFileDownloadTest
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.util.FileUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.net.http.HttpResponse.BodyHandlers.ofFileDownload;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AsFileDownloadTest {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    Path tempDir;

    static final String[][] contentDispositionValues = new String[][] {
          // URI query     Content-Type header value         Expected filename
            { "001", "Attachment; filename=example001.html", "example001.html" },
            { "002", "attachment; filename=example002.html", "example002.html" },
            { "003", "ATTACHMENT; filename=example003.html", "example003.html" },
            { "004", "attAChment; filename=example004.html", "example004.html" },
            { "005", "attachmeNt; filename=example005.html", "example005.html" },

            { "006", "attachment; Filename=example006.html", "example006.html" },
            { "007", "attachment; FILENAME=example007.html", "example007.html" },
            { "008", "attachment; fileName=example008.html", "example008.html" },
            { "009", "attachment; fIlEnAMe=example009.html", "example009.html" },

            { "010", "attachment; filename=Example010.html", "Example010.html" },
            { "011", "attachment; filename=EXAMPLE011.html", "EXAMPLE011.html" },
            { "012", "attachment; filename=eXample012.html", "eXample012.html" },
            { "013", "attachment; filename=example013.HTML", "example013.HTML" },
            { "014", "attachment; filename  =eXaMpLe014.HtMl", "eXaMpLe014.HtMl"},

            { "015", "attachment; filename=a",               "a"               },
            { "016", "attachment; filename= b",              "b"               },
            { "017", "attachment; filename=  c",             "c"               },
            { "018", "attachment; filename=    d",           "d"               },
            { "019", "attachment; filename=e  ; filename*=utf-8''eee.txt",  "e"},
            { "020", "attachment; filename*=utf-8''fff.txt; filename=f",    "f"},
            { "021", "attachment;  filename=g",              "g"               },
            { "022", "attachment;   filename= h",            "h"               },

            { "023", "attachment; filename=\"space name\"",                       "space name" },
            { "024", "attachment; filename=me.txt; filename*=utf-8''you.txt",     "me.txt"     },
            { "025", "attachment; filename=\"m y.txt\"; filename*=utf-8''you.txt", "m y.txt"   },

            { "030", "attachment; filename=foo/file1.txt",        "file1.txt" },
            { "031", "attachment; filename=foo/bar/file2.txt",    "file2.txt" },
            { "032", "attachment; filename=baz\\file3.txt",       "file3.txt" },
            { "033", "attachment; filename=baz\\bar\\file4.txt",  "file4.txt" },
            { "034", "attachment; filename=x/y\\file5.txt",       "file5.txt" },
            { "035", "attachment; filename=x/y\\file6.txt",       "file6.txt" },
            { "036", "attachment; filename=x/y\\z/file7.txt",     "file7.txt" },
            { "037", "attachment; filename=x/y\\z/\\x/file8.txt", "file8.txt" },
            { "038", "attachment; filename=/root/file9.txt",      "file9.txt" },
            { "039", "attachment; filename=../file10.txt",        "file10.txt" },
            { "040", "attachment; filename=..\\file11.txt",       "file11.txt" },
            { "041", "attachment; filename=foo/../../file12.txt", "file12.txt" },
    };

    @DataProvider(name = "positive")
    public Object[][] positive() {
        List<Object[]> list = new ArrayList<>();

        Arrays.asList(contentDispositionValues).stream()
                .map(e -> new Object[] {httpURI +  "?" + e[0], e[1], e[2]})
                .forEach(list::add);
        Arrays.asList(contentDispositionValues).stream()
                .map(e -> new Object[] {httpsURI +  "?" + e[0], e[1], e[2]})
                .forEach(list::add);
        Arrays.asList(contentDispositionValues).stream()
                .map(e -> new Object[] {http2URI +  "?" + e[0], e[1], e[2]})
                .forEach(list::add);
        Arrays.asList(contentDispositionValues).stream()
                .map(e -> new Object[] {https2URI +  "?" + e[0], e[1], e[2]})
                .forEach(list::add);

        return list.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "positive")
    void test(String uriString, String contentDispositionValue, String expectedFilename)
        throws Exception
    {
        out.printf("test(%s, %s, %s): starting", uriString, contentDispositionValue, expectedFilename);
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(BodyPublishers.ofString("May the luck of the Irish be with you!"))
                .build();

        BodyHandler bh = ofFileDownload(tempDir.resolve(uri.getPath().substring(1)),
                                        CREATE, TRUNCATE_EXISTING, WRITE);
        HttpResponse<Path> response = client.send(request, bh);

        out.println("Got response: " + response);
        out.println("Got body Path: " + response.body());
        String fileContents = new String(Files.readAllBytes(response.body()), UTF_8);
        out.println("Got body: " + fileContents);

        assertEquals(response.statusCode(),200);
        assertEquals(response.body().getFileName().toString(), expectedFilename);
        assertTrue(response.headers().firstValue("Content-Disposition").isPresent());
        assertEquals(response.headers().firstValue("Content-Disposition").get(),
                     contentDispositionValue);
        assertEquals(fileContents, "May the luck of the Irish be with you!");

        // additional checks unrelated to file download
        caseInsensitivityOfHeaders(request.headers());
        caseInsensitivityOfHeaders(response.headers());
    }

    // --- Negative

    static final String[][] contentDispositionBADValues = new String[][] {
            // URI query     Content-Type header value
            { "100", ""                                    },  // empty
            { "101", "filename=example.html"               },  // no attachment
            { "102", "attachment; filename=space name"     },  // unquoted with space
            { "103", "attachment; filename="               },  // empty filename param
            { "104", "attachment; filename=\""             },  // single quote
            { "105", "attachment; filename=\"\""           },  // empty quoted
            { "106", "attachment; filename=."              },  // dot
            { "107", "attachment; filename=.."             },  // dot dot
            { "108", "attachment; filename=\".."           },  // badly quoted dot dot
            { "109", "attachment; filename=\"..\""         },  // quoted dot dot
            { "110", "attachment; filename=\"bad"          },  // badly quoted
            { "111", "attachment; filename=\"bad;"         },  // badly quoted with ';'
            { "112", "attachment; filename=\"bad ;"        },  // badly quoted with ' ;'
            { "113", "attachment; filename*=utf-8''xx.txt "},  // no "filename" param

            { "120", "<<NOT_PRESENT>>"                     },  // header not present

    };

    @DataProvider(name = "negative")
    public Object[][] negative() {
        List<Object[]> list = new ArrayList<>();

        Arrays.asList(contentDispositionBADValues).stream()
                .map(e -> new Object[] {httpURI +  "?" + e[0], e[1]})
                .forEach(list::add);
        Arrays.asList(contentDispositionBADValues).stream()
                .map(e -> new Object[] {httpsURI +  "?" + e[0], e[1]})
                .forEach(list::add);
        Arrays.asList(contentDispositionBADValues).stream()
                .map(e -> new Object[] {http2URI +  "?" + e[0], e[1]})
                .forEach(list::add);
        Arrays.asList(contentDispositionBADValues).stream()
                .map(e -> new Object[] {https2URI +  "?" + e[0], e[1]})
                .forEach(list::add);

        return list.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "negative")
    void negativeTest(String uriString, String contentDispositionValue)
            throws Exception
    {
        out.printf("negativeTest(%s, %s): starting", uriString, contentDispositionValue);
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(BodyPublishers.ofString("Does not matter"))
                .build();

        BodyHandler bh = ofFileDownload(tempDir, CREATE, TRUNCATE_EXISTING, WRITE);
        try {
            HttpResponse<Path> response = client.send(request, bh);
            fail("UNEXPECTED response: " + response + ", path:" + response.body());
        } catch (UncheckedIOException | IOException ioe) {
            System.out.println("Caught expected: " + ioe);
        }
    }

    // -- Infrastructure

    static String serverAuthority(HttpServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        tempDir = Paths.get("asFileDownloadTest.tmp.dir");
        if (Files.exists(tempDir))
            throw new AssertionError("Unexpected test work dir existence: " + tempDir.toString());

        Files.createDirectory(tempDir);
        // Unique dirs per test run, based on the URI path
        Files.createDirectories(tempDir.resolve("http1/afdt/"));
        Files.createDirectories(tempDir.resolve("https1/afdt/"));
        Files.createDirectories(tempDir.resolve("http2/afdt/"));
        Files.createDirectories(tempDir.resolve("https2/afdt/"));

        // HTTP/1.1 server logging in case of security exceptions ( uncomment if needed )
        //Logger logger = Logger.getLogger("com.sun.net.httpserver");
        //ConsoleHandler ch = new ConsoleHandler();
        //logger.setLevel(Level.ALL);
        //ch.setLevel(Level.ALL);
        //logger.addHandler(ch);

        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.createContext("/http1/afdt", new Http1FileDispoHandler());
        httpURI = "http://" + serverAuthority(httpTestServer) + "/http1/afdt";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1/afdt", new Http1FileDispoHandler());
        httpsURI = "https://" + serverAuthority(httpsTestServer) + "/https1/afdt";

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(new Http2FileDispoHandler(), "/http2/afdt");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/afdt";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(new Http2FileDispoHandler(), "/https2/afdt");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/afdt";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        http2TestServer.stop();
        https2TestServer.stop();

        if (System.getSecurityManager() == null && Files.exists(tempDir)) {
            // clean up before next run with security manager
            FileUtils.deleteFileTreeWithRetry(tempDir);
        }
    }

    static String contentDispositionValueFromURI(URI uri) {
        String queryIndex = uri.getQuery();
        String[][] values;
        if (queryIndex.startsWith("0"))  // positive tests start with '0'
            values = contentDispositionValues;
        else if (queryIndex.startsWith("1"))  // negative tests start with '1'
            values = contentDispositionBADValues;
        else
            throw new AssertionError("SERVER: UNEXPECTED query:" + queryIndex);

        return Arrays.asList(values).stream()
                .filter(e -> e[0].equals(queryIndex))
                .map(e -> e[1])
                .findFirst()
                .orElseThrow();
    }

    static class Http1FileDispoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();

                String value = contentDispositionValueFromURI(t.getRequestURI());
                if (!value.equals("<<NOT_PRESENT>>"))
                    t.getResponseHeaders().set("Content-Disposition", value);

                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    static class Http2FileDispoHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();

                String value = contentDispositionValueFromURI(t.getRequestURI());
                if (!value.equals("<<NOT_PRESENT>>"))
                    t.getResponseHeaders().addHeader("Content-Disposition", value);

                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    // ---

    // Asserts case-insensitivity of headers (nothing to do with file
    // download, just convenient as we have a couple of header instances. )
    static void caseInsensitivityOfHeaders(HttpHeaders headers) {
        try {
            for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
                String headerName = entry.getKey();
                List<String> headerValue = entry.getValue();

                for (String name : List.of(headerName.toUpperCase(Locale.ROOT),
                                           headerName.toLowerCase(Locale.ROOT))) {
                    assertTrue(headers.firstValue(name).isPresent());
                    assertEquals(headers.firstValue(name).get(), headerValue.get(0));
                    assertEquals(headers.allValues(name).size(), headerValue.size());
                    assertEquals(headers.allValues(name), headerValue);
                    assertEquals(headers.map().get(name).size(), headerValue.size());
                    assertEquals(headers.map().get(name), headerValue);
                }
            }
        } catch (Throwable t) {
            System.out.println("failure in caseInsensitivityOfHeaders with:" + headers);
            throw t;
        }
    }
}
