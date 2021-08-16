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

import static java.net.http.HttpClient.Builder.NO_PROXY;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import jdk.test.lib.security.KeyEntry;
import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.SSLContextBuilder;

/*
 * @test
 * @library /test/lib
 * @build Server CertificateTest
 * @run main/othervm CertificateTest GOOD_CERT expectSuccess
 * @run main/othervm CertificateTest BAD_CERT expectFailure
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification
 *       CertificateTest BAD_CERT expectSuccess
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification=true
 *       CertificateTest BAD_CERT expectSuccess
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification=false
 *       CertificateTest BAD_CERT expectFailure
 * @run main/othervm
 *      -Djdk.internal.httpclient.disableHostnameVerification=xxyyzz
 *       CertificateTest BAD_CERT expectFailure
 * @run main/othervm CertificateTest LOOPBACK_CERT expectSuccess
 */

/**
 * The test runs a number of times. In all cases it uses a valid self-signed certificate
 * that is installed in the trust store (so is trusted) and the same cert is supplied
 * by the server for its own identity. Two servers on two different ports are used
 * on the remote end.
 *
 * The GOOD_CERT cert contains the correct hostname of the target server
 * and therefore should be accepted by the cert checking code in the client.
 * The BAD_CERT cert contains an invalid hostname, and should be rejected.
 * The LOOPBACK_CERT cert contains an invalid hostname, but it also contains a
 * subject alternative name for IP address 127.0.0.1, so it should be accepted
 * for this address.
 */
public class CertificateTest {

    private static Cert cert;
    static boolean expectSuccess;
    static Server server;
    static int port;

    public static void main(String[] args) throws Exception
    {
        try {
            String certName = args[0];
            String passOrFail = args[1];

            if (passOrFail.equals("expectSuccess")) {
                expectSuccess = true;
            } else {
                expectSuccess = false;
            }

            cert = Cert.valueOf(certName);
            server = new Server(getSSLContext(cert));
            port = server.getPort();
            test(cert);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private static SSLContext getSSLContext(Cert cert) throws Exception {
        SSLContextBuilder builder = SSLContextBuilder.builder();
        builder.trustStore(
                KeyStoreUtils.createTrustStore(new String[] { cert.certStr }));
        builder.keyStore(KeyStoreUtils.createKeyStore(
                new KeyEntry[] { new KeyEntry(cert.keyAlgo,
                        cert.keyStr, new String[] { cert.certStr }) }));
        return builder.build();
    }

    static void test(Cert cert) throws Exception
    {
        String uri_s;
        if (cert == Cert.LOOPBACK_CERT)
            uri_s = "https://127.0.0.1:" + Integer.toString(port) + "/foo";
        else
            uri_s = "https://localhost:" + Integer.toString(port) + "/foo";
        String error = null;
        Exception exception = null;
        System.out.println("Making request to " + uri_s);

        SSLContext ctx = getSSLContext(cert);
        HttpClient client = HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .sslContext(ctx)
                .sslParameters(ctx.getDefaultSSLParameters())
                .build();

        HttpRequest request = HttpRequest.newBuilder(new URI(uri_s))
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.printf("Status code %d received\n", response.statusCode());
            if (expectSuccess && response.statusCode() != 200)
                error = "Test failed: good: status should be 200";
            else if (!expectSuccess)
                error = "Test failed: bad: status should not be 200";
        } catch (IOException e) {
            // there must be an SSLException as the exception or cause
            checkExceptionOrCause(SSLException.class, e);
            System.err.println("Caught Exception " + e + ". expectSuccess = " + expectSuccess);
            exception = e;
            if (expectSuccess)
                error = "Test failed: expectSuccess:true, but got unexpected exception";
        }
        if (error != null)
            throw new RuntimeException(error, exception);
    }

    static void checkExceptionOrCause(Class<? extends Throwable> clazz, Throwable t) {
        final Throwable original = t;
        do {
            if (clazz.isInstance(t)) {
                System.out.println("Found expected exception/cause: " + t);
                return; // found
            }
        } while ((t = t.getCause()) != null);
        original.printStackTrace(System.out);
        throw new RuntimeException("Expected " + clazz + "in " + original);
    }
}
