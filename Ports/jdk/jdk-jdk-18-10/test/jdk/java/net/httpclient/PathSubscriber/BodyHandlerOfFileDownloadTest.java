/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237470
 * @summary Confirm HttpResponse.BodySubscribers#ofFileDownload(Path)
 *          works only with the default file system
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          jdk.httpserver
 * @library /test/lib ../http2/server
 * @build Http2TestServer Http2TestServerConnection Http2TestExchange
 *        Http2Handler OutgoingPushPromise Queue
 * @build jdk.test.lib.net.SimpleSSLContext
 * @build jdk.test.lib.Platform
 * @build jdk.test.lib.util.FileUtils
 * @compile ../HttpServerAdapters.java
 * @run testng/othervm BodyHandlerOfFileDownloadTest
 * @run testng/othervm/java.security.policy=ofFileDownload.policy BodyHandlerOfFileDownloadTest
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.util.FileUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BodyHandlerOfFileDownloadTest implements HttpServerAdapters {
    static final String MSG = "msg";
    static final String contentDispositionValue = "attachment; filename=example.html";

    SSLContext sslContext;
    HttpServerAdapters.HttpTestServer httpTestServer;    // HTTP/1.1      [ 4 servers ]
    HttpServerAdapters.HttpTestServer httpsTestServer;   // HTTPS/1.1
    HttpServerAdapters.HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpServerAdapters.HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    FileSystem zipFs;
    Path defaultFsPath;
    Path zipFsPath;

    // Default file system

    static Path defaultFsDir() throws Exception {
        var dir = Path.of("defaultDir");
        if (Files.notExists(dir)) {
            Files.createDirectory(dir);
        }
        return dir;
    }

    @DataProvider(name = "defaultFsData")
    public Object[][] defaultFsData() {
        return new Object[][]{
                {  httpURI,    defaultFsPath,  MSG,  true   },
                {  httpsURI,   defaultFsPath,  MSG,  true   },
                {  http2URI,   defaultFsPath,  MSG,  true   },
                {  https2URI,  defaultFsPath,  MSG,  true   },
                {  httpURI,    defaultFsPath,  MSG,  false  },
                {  httpsURI,   defaultFsPath,  MSG,  false  },
                {  http2URI,   defaultFsPath,  MSG,  false  },
                {  https2URI,  defaultFsPath,  MSG,  false  },
        };
    }

    @Test(dataProvider = "defaultFsData")
    public void testDefaultFs(String uriString,
                              Path path,
                              String expectedMsg,
                              boolean sameClient) throws Exception {
        out.printf("\n\n--- testDefaultFs(%s, %s, \"%s\", %b): starting\n",
                uriString, path, expectedMsg, sameClient);
        receive(uriString, path, expectedMsg, sameClient);
    }

    private static final int ITERATION_COUNT = 3;

    private void receive(String uriString,
                      Path path,
                      String expectedMsg,
                      boolean sameClient) throws Exception {
        HttpClient client = null;

        for (int i = 0; i < ITERATION_COUNT; i++) {
            if (!sameClient || client == null) {
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();
            }
            var req = HttpRequest.newBuilder(URI.create(uriString))
                .POST(BodyPublishers.noBody())
                .build();
            var resp = client.send(req, BodyHandlers.ofFileDownload(path, CREATE, TRUNCATE_EXISTING, WRITE));
            String msg = Files.readString(resp.body());
            out.printf("Resp code: %s\n", resp.statusCode());
            out.println("Resp body Path: " + resp.body());
            out.printf("Resp body written to file: %s\n", msg);
            assertEquals(resp.statusCode(), 200);
            assertEquals(msg, expectedMsg);
            assertTrue(resp.headers().firstValue("Content-Disposition").isPresent());
            assertEquals(resp.headers().firstValue("Content-Disposition").get(), contentDispositionValue);
        }
    }

    // Zip file system

    static FileSystem newZipFs() throws Exception {
        Path zipFile = Path.of("file.zip");
        return FileSystems.newFileSystem(zipFile, Map.of("create", "true"));
    }

    static Path zipFsDir(FileSystem fs) throws Exception {
        var dir = fs.getPath("zipDir");
        if (Files.notExists(dir)) {
            Files.createDirectory(dir);
        }
        return dir;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testZipFs() {
        out.printf("\n\n--- testZipFs(): starting\n");
        BodyHandlers.ofFileDownload(zipFsPath, CREATE, TRUNCATE_EXISTING, WRITE);
    }


    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        defaultFsPath = defaultFsDir();
        zipFs = newZipFs();
        zipFsPath = zipFsDir(zipFs);

        InetSocketAddress sa =
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpServerAdapters.HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(new HttpEchoHandler(), "/http1/echo");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/echo";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpServerAdapters.HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new HttpEchoHandler(), "/https1/echo");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/echo";

        http2TestServer = HttpServerAdapters.HttpTestServer.of(
                new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new HttpEchoHandler(), "/http2/echo");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/echo";

        https2TestServer = HttpServerAdapters.HttpTestServer.of(
                new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new HttpEchoHandler(), "/https2/echo");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/echo";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        if (Files.exists(zipFsPath))
            FileUtils.deleteFileTreeWithRetry(zipFsPath);
        if (Files.exists(defaultFsPath))
            FileUtils.deleteFileTreeWithRetry(defaultFsPath);

        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
        zipFs.close();
    }

    static class HttpEchoHandler implements HttpServerAdapters.HttpTestHandler {
        @Override
        public void handle(HttpServerAdapters.HttpTestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                t.getResponseHeaders().addHeader("Content-Disposition",
                                "attachment; filename=example.html");
                t.sendResponseHeaders(200, MSG.getBytes().length);
                os.write(MSG.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
