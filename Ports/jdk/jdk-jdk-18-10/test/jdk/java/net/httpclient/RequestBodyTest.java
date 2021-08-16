/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8087112
 * @modules java.net.http
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib
 * @compile ../../../com/sun/net/httpserver/LogFilter.java
 * @compile ../../../com/sun/net/httpserver/EchoHandler.java
 * @compile ../../../com/sun/net/httpserver/FileServerHandler.java
 * @build jdk.test.lib.net.SimpleSSLContext
 * @build LightWeightHttpServer
 * @build jdk.test.lib.Platform
 * @build jdk.test.lib.util.FileUtils
 * @run testng/othervm RequestBodyTest
 * @run testng/othervm/java.security.policy=RequestBodyTest.policy RequestBodyTest
 */

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import jdk.test.lib.util.FileUtils;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.StandardOpenOption.*;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestBodyTest {

    static final String fileroot = System.getProperty("test.src", ".") + "/docs";
    static final String midSizedFilename = "/files/notsobigfile.txt";
    static final String smallFilename = "/files/smallfile.txt";
    final ConcurrentHashMap<String,Throwable> failures = new ConcurrentHashMap<>();

    HttpClient client;
    String httpURI;
    String httpsURI;

    enum RequestBody {
        BYTE_ARRAY,
        BYTE_ARRAY_OFFSET,
        BYTE_ARRAYS,
        FILE,
        INPUTSTREAM,
        STRING,
        STRING_WITH_CHARSET
    }

    enum ResponseBody {
        BYTE_ARRAY,
        BYTE_ARRAY_CONSUMER,
        DISCARD,
        FILE,
        FILE_WITH_OPTION,
        STRING,
        STRING_WITH_CHARSET,
    }

    @BeforeTest
    public void setup() throws Exception {
        LightWeightHttpServer.initServer();
        httpURI = LightWeightHttpServer.httproot + "echo/foo";
        httpsURI = LightWeightHttpServer.httpsroot + "echo/foo";

        SSLContext ctx = LightWeightHttpServer.ctx;
        client = HttpClient.newBuilder()
                           .sslContext(ctx)
                           .version(HttpClient.Version.HTTP_1_1)
                           .followRedirects(HttpClient.Redirect.ALWAYS)
                           .build();
    }

    @AfterTest
    public void teardown() throws Exception {
        try {
            LightWeightHttpServer.stop();
        } finally {
            System.out.println("RequestBodyTest: " + failures.size() + " failures");
            int i = 0;
            for (String key: failures.keySet()) {
                System.out.println("test" + key + " failed: " + failures.get(key));
                failures.get(key).printStackTrace(System.out);
                if (i++ > 3) {
                   System.out.println("..... other failures not printed ....");
                   break;
                }
            }
        }
    }

    @DataProvider
    public Object[][] exchanges() throws Exception {
        List<Object[]> values = new ArrayList<>();

        for (boolean async : new boolean[] { false, true })
            for (String uri : new String[] { httpURI, httpsURI })
                for (String file : new String[] { smallFilename, midSizedFilename })
                    for (RequestBody requestBodyType : RequestBody.values())
                        for (ResponseBody responseBodyType : ResponseBody.values())
                            for (boolean bufferResponseBody : new boolean[] { false, true })
                                values.add(new Object[]
                                    {uri, requestBodyType, responseBodyType, file, async, bufferResponseBody});

        return values.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "exchanges")
    void exchange(String target,
                  RequestBody requestBodyType,
                  ResponseBody responseBodyType,
                  String file,
                  boolean async,
                  boolean bufferResponseBody)
        throws Exception
    {
        try {
            Path filePath = Paths.get(fileroot + file);
            URI uri = new URI(target);

            HttpRequest request = createRequest(uri, requestBodyType, filePath);

            checkResponse(client, request, requestBodyType, responseBodyType, filePath, async, bufferResponseBody);
        } catch (Exception | Error x) {
            Object[] params = new Object[] {
                target, requestBodyType, responseBodyType,
                file, "async=" + async, "buffer=" + bufferResponseBody
            };
            failures.put(java.util.Arrays.toString(params), x);
            throw x;
        }
    }

    static final int DEFAULT_OFFSET = 10;
    static final int DEFAULT_LENGTH_FACTOR = 4/5;

    HttpRequest createRequest(URI uri,
                              RequestBody requestBodyType,
                              Path file)
        throws IOException
    {
        HttpRequest.Builder rb =  HttpRequest.newBuilder(uri);

        String filename = file.toFile().getAbsolutePath();
        byte[] fileAsBytes = getFileBytes(filename);
        String fileAsString = new String(fileAsBytes, UTF_8);

        switch (requestBodyType) {
            case BYTE_ARRAY:
                rb.POST(BodyPublishers.ofByteArray(fileAsBytes));
                break;
            case BYTE_ARRAY_OFFSET:
                rb.POST(BodyPublishers.ofByteArray(fileAsBytes,
                        DEFAULT_OFFSET,
                        fileAsBytes.length * DEFAULT_LENGTH_FACTOR));
                break;
            case BYTE_ARRAYS:
                Iterable<byte[]> iterable = Arrays.asList(fileAsBytes);
                rb.POST(BodyPublishers.ofByteArrays(iterable));
                break;
            case FILE:
                rb.POST(BodyPublishers.ofFile(file));
                break;
            case INPUTSTREAM:
                rb.POST(BodyPublishers.ofInputStream(fileInputStreamSupplier(file)));
                break;
            case STRING:
                rb.POST(BodyPublishers.ofString(fileAsString));
                break;
            case STRING_WITH_CHARSET:
                rb.POST(BodyPublishers.ofString(new String(fileAsBytes), Charset.defaultCharset()));
                break;
            default:
                throw new AssertionError("Unknown request body:" + requestBodyType);
        }
        return rb.build();
    }

    void checkResponse(HttpClient client,
                       HttpRequest request,
                       RequestBody requestBodyType,
                       ResponseBody responseBodyType,
                       Path file,
                       boolean async,
                       boolean bufferResponseBody)
        throws InterruptedException, IOException
    {
        String filename = file.toFile().getAbsolutePath();
        byte[] fileAsBytes = getFileBytes(filename);
        if (requestBodyType == RequestBody.BYTE_ARRAY_OFFSET) {
            // Truncate the expected response body, if only a portion was sent
            int length = DEFAULT_OFFSET + (fileAsBytes.length * DEFAULT_LENGTH_FACTOR);
            fileAsBytes = Arrays.copyOfRange(fileAsBytes, DEFAULT_OFFSET, length);
        }
        String fileAsString = new String(fileAsBytes, UTF_8);
        Path tempFile = Paths.get("RequestBodyTest.tmp");
        FileUtils.deleteFileIfExistsWithRetry(tempFile);

        switch (responseBodyType) {
            case BYTE_ARRAY:
                BodyHandler<byte[]> bh = BodyHandlers.ofByteArray();
                if (bufferResponseBody) bh = BodyHandlers.buffering(bh, 50);
                HttpResponse<byte[]> bar = getResponse(client, request, bh, async);
                assertEquals(bar.statusCode(), 200);
                assertEquals(bar.body(), fileAsBytes);
                break;
            case BYTE_ARRAY_CONSUMER:
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Consumer<Optional<byte[]>> consumer = o -> consumerBytes(o, baos);
                BodyHandler<Void> bh1 = BodyHandlers.ofByteArrayConsumer(consumer);
                if (bufferResponseBody) bh1 = BodyHandlers.buffering(bh1, 49);
                HttpResponse<Void> v = getResponse(client, request, bh1, async);
                byte[] ba = baos.toByteArray();
                assertEquals(v.statusCode(), 200);
                assertEquals(ba, fileAsBytes);
                break;
            case DISCARD:
                Object o = new Object();
                BodyHandler<Object> bh2 = BodyHandlers.replacing(o);
                if (bufferResponseBody) bh2 = BodyHandlers.buffering(bh2, 51);
                HttpResponse<Object> or = getResponse(client, request, bh2, async);
                assertEquals(or.statusCode(), 200);
                assertSame(or.body(), o);
                break;
            case FILE:
                BodyHandler<Path> bh3 = BodyHandlers.ofFile(tempFile);
                if (bufferResponseBody) bh3 = BodyHandlers.buffering(bh3, 48);
                HttpResponse<Path> fr = getResponse(client, request, bh3, async);
                assertEquals(fr.statusCode(), 200);
                assertEquals(Files.size(tempFile), fileAsString.length());
                assertEquals(Files.readAllBytes(tempFile), fileAsBytes);
                break;
            case FILE_WITH_OPTION:
                BodyHandler<Path> bh4 = BodyHandlers.ofFile(tempFile, CREATE_NEW, WRITE);
                if (bufferResponseBody) bh4 = BodyHandlers.buffering(bh4, 52);
                fr = getResponse(client, request, bh4, async);
                assertEquals(fr.statusCode(), 200);
                assertEquals(Files.size(tempFile), fileAsString.length());
                assertEquals(Files.readAllBytes(tempFile), fileAsBytes);
                break;
            case STRING:
                BodyHandler<String> bh5 = BodyHandlers.ofString();
                if(bufferResponseBody) bh5 = BodyHandlers.buffering(bh5, 47);
                HttpResponse<String> sr = getResponse(client, request, bh5, async);
                assertEquals(sr.statusCode(), 200);
                assertEquals(sr.body(), fileAsString);
                break;
            case STRING_WITH_CHARSET:
                BodyHandler<String> bh6 = BodyHandlers.ofString(StandardCharsets.UTF_8);
                if (bufferResponseBody) bh6 = BodyHandlers.buffering(bh6, 53);
                HttpResponse<String> r = getResponse(client, request, bh6, async);
                assertEquals(r.statusCode(), 200);
                assertEquals(r.body(), fileAsString);
                break;
            default:
                throw new AssertionError("Unknown response body:" + responseBodyType);
        }
    }

    static <T> HttpResponse<T> getResponse(HttpClient client,
                                           HttpRequest request,
                                           HttpResponse.BodyHandler<T> handler,
                                           boolean async)
        throws InterruptedException, IOException
    {
        if (!async)
            return client.send(request, handler);
        else
            return client.sendAsync(request, handler).join();
    }

    static byte[] getFileBytes(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bis.transferTo(baos);
            return baos.toByteArray();
        }
    }

    static Supplier<FileInputStream> fileInputStreamSupplier(Path f) {
        return new Supplier<>() {
            Path file = f;
            @Override
            public FileInputStream get() {
                try {
                    PrivilegedExceptionAction<FileInputStream> pa =
                            () -> new FileInputStream(file.toFile());
                    return AccessController.doPrivileged(pa);
                } catch (PrivilegedActionException x) {
                    throw new UncheckedIOException((IOException)x.getCause());
                }
            }
        };
    }

    static void consumerBytes(Optional<byte[]> bytes, ByteArrayOutputStream baos) {
        try {
            if (bytes.isPresent())
                baos.write(bytes.get());
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    // ---

    /* Main entry point for standalone testing of the main functional test. */
    public static void main(String... args) throws Exception {
        RequestBodyTest t = new RequestBodyTest();
        t.setup();
        int count = 0;
        try {
            for (Object[] objs : t.exchanges()) {
                count++;
                out.printf("********* iteration: %d %s %s %s %s %s %s *********%n",
                           count, objs[0], objs[1], objs[2], objs[3], objs[4], objs[5]);
                t.exchange((String) objs[0],
                           (RequestBody) objs[1],
                           (ResponseBody) objs[2],
                           (String) objs[3],
                           (boolean) objs[4],
                           (boolean) objs[5]);
            }
        } finally {
            t.teardown();
        }
    }
}
