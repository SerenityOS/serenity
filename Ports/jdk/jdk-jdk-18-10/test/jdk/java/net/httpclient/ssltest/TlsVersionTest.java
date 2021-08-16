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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.URIBuilder;
import jdk.test.lib.security.KeyEntry;
import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.SSLContextBuilder;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.net.http.HttpClient.Builder.NO_PROXY;

/*
 * @test
 * @bug 8239594 8239595
 * @library /test/lib
 * @build Server TlsVersionTest
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification
 *       TlsVersionTest false
 *
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification
 *      -Djdk.tls.client.protocols="TLSv1.2"
 *       TlsVersionTest true
 */

/**
 * The test uses a valid self-signed certificate
 * that is installed in the trust store (so is trusted) and the same cert
 * is supplied by the server for its own identity.
 *
 * The test sets the context TLS Version 1.2 for client and 1.3
 * for the server, as per the bug, the server checks for the
 * negotiated protocol version, it should adhere to the protocol
 * version value used in SSL Context for client and the server should
 * be able to downgrade to 1.2 during the handshake. The test would
 * fail if the negotiated protocol version is different from the one set in
 * SSL Context for the client as it is the lower version.
 */

public class TlsVersionTest {

    private static Cert cert;
    // parameter to distinguish scenarios where system property is set and unset
    static String tlsVersion;
    static Server server;
    static int port;

    public static void main(String[] args) throws Exception {
        try {
            tlsVersion = args[0];
            // certificate name set to be accepted by dummy server
            cert = Cert.valueOf("LOOPBACK_CERT");
            server = new Server(getServerSSLContext(cert));
            port = server.getPort();
            test(cert);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private static SSLContext getServerSSLContext(Cert cert) throws Exception {
        SSLContextBuilder builder = SSLContextBuilder.builder();
        builder.trustStore(
                KeyStoreUtils.createTrustStore(new String[] { cert.certStr }));
        builder.keyStore(KeyStoreUtils.createKeyStore(
                new KeyEntry[] { new KeyEntry(cert.keyAlgo,
                        cert.keyStr, new String[] { cert.certStr }) }));
            builder.protocol("TLSv1.3");
        return builder.build();
    }

    private static SSLContext getClientSSLContext(Cert cert) throws Exception {
        SSLContextBuilder builder = SSLContextBuilder.builder();
        builder.trustStore(
                KeyStoreUtils.createTrustStore(new String[] { cert.certStr }));
        builder.keyStore(KeyStoreUtils.createKeyStore(
                new KeyEntry[] { new KeyEntry(cert.keyAlgo,
                        cert.keyStr, new String[] { cert.certStr }) }));
        if(tlsVersion.equals("false"))
            builder.protocol("TLSv1.2");
        return builder.build();
    }

    static void test(Cert cert) throws Exception {
        URI serverURI = URIBuilder.newBuilder()
                                  .scheme("https")
                                  .loopback()
                                  .port(server.getPort())
                                  .path("/foo")
                                  .build();
        String error = null;
        System.out.println("Making request to " + serverURI.getPath());
        SSLContext ctx = getClientSSLContext(cert);
        HttpClient client = HttpClient.newBuilder()
                                      .proxy(NO_PROXY)
                                      .sslContext(ctx)
                                      .build();

        for (var version : List.of(HttpClient.Version.HTTP_2, HttpClient.Version.HTTP_1_1)) {
            HttpRequest request = HttpRequest.newBuilder(serverURI)
                                             .version(version)
                                             .GET()
                                             .build();
            System.out.println("Using version: " + version);
            try {
                HttpResponse<String> response = client.send(request, ofString());
                String protocol = response.sslSession().get().getProtocol();
                System.out.println("TLS version negotiated is: " + protocol);
                if (!(protocol.equals("TLSv1.2"))) {
                    error = "Test failed : TLS version should be " + "TLSv1.2";
                    throw new RuntimeException(error);
                }
            } catch (IOException e) {
                System.out.println("Caught Exception " + e);
                throw e;
            }
        }
    }
}
