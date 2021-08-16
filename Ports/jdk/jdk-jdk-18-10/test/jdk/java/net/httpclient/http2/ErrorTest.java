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
 * @bug 8157105
 * @library /test/lib server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.security.jgss
 * @run testng/othervm/timeout=60 -Djavax.net.debug=ssl -Djdk.httpclient.HttpClient.log=all ErrorTest
 * @summary check exception thrown when bad TLS parameters selected
 */

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jdk.test.lib.net.SimpleSSLContext;
import static java.net.http.HttpClient.Version.HTTP_2;

import org.testng.annotations.Test;

/**
 * When selecting an unacceptable cipher suite the TLS handshake will fail.
 * But, the exception that was thrown was not being returned up to application
 * causing hang problems
 */
public class ErrorTest {

    static final String[] CIPHER_SUITES = new String[]{ "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384" };

    static final String SIMPLE_STRING = "Hello world Goodbye world";

    //@Test(timeOut=5000)
    @Test
    public void test() throws Exception {
        SSLContext sslContext = (new SimpleSSLContext()).get();
        ExecutorService exec = Executors.newCachedThreadPool();
        HttpClient client = HttpClient.newBuilder()
                                      .executor(exec)
                                      .sslContext(sslContext)
                                      .sslParameters(new SSLParameters(CIPHER_SUITES))
                                      .version(HTTP_2)
                                      .build();

        Http2TestServer httpsServer = null;
        try {
            SSLContext serverContext = (new SimpleSSLContext()).get();
            SSLParameters p = serverContext.getSupportedSSLParameters();
            p.setApplicationProtocols(new String[]{"h2"});
            httpsServer = new Http2TestServer(true,
                                              0,
                                              exec,
                                              serverContext);
            httpsServer.addHandler(new Http2EchoHandler(), "/");
            int httpsPort = httpsServer.getAddress().getPort();
            String httpsURIString = "https://localhost:" + httpsPort + "/bar/";

            httpsServer.start();
            URI uri = URI.create(httpsURIString);
            System.err.println("Request to " + uri);

            HttpRequest req = HttpRequest.newBuilder(uri)
                                    .POST(BodyPublishers.ofString(SIMPLE_STRING))
                                    .build();
            HttpResponse response;
            try {
                response = client.send(req, BodyHandlers.discarding());
                throw new RuntimeException("Unexpected response: " + response);
            } catch (IOException e) {
                System.err.println("Caught Expected IOException: " + e);
            }
            System.err.println("DONE");
        } finally {
            if (httpsServer != null )  { httpsServer.stop(); }
            exec.shutdownNow();
        }
    }
}
