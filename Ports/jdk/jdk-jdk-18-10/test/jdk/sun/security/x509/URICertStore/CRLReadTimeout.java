/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191808
 * @summary check that CRL download is interrupted if it takes too long
 * @library /test/lib
 * @run main/othervm -Dcom.sun.security.crl.readtimeout=1 CRLReadTimeout
 */

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import static java.security.cert.PKIXRevocationChecker.Option.*;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.sun.net.httpserver.HttpServer;

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

public class CRLReadTimeout {

    public static void main(String[] args) throws Exception {

        String timeout = System.getProperty("com.sun.security.crl.readtimeout");
        if (timeout == null) {
            timeout = "15";
        }
        System.out.println("Testing timeout of " + timeout + " seconds");

        CrlHttpServer crlServer = new CrlHttpServer(Integer.parseInt(timeout));
        try {
            crlServer.start();
            testTimeout(crlServer.getPort());
        } finally {
            crlServer.stop();
        }
    }

    private static void testTimeout(int port) throws Exception {

        // create certificate chain with two certs, root and end-entity
        keytool("-alias duke -dname CN=duke -genkey -keyalg RSA");
        keytool("-alias root -dname CN=root -genkey -keyalg RSA");
        keytool("-certreq -alias duke -file duke.req");
        // set CRL URI to local server
        keytool("-gencert -infile duke.req -alias root -rfc -outfile duke.cert "
                + "-ext crl=uri:http://localhost:" + port + "/crl");
        keytool("-importcert -file duke.cert -alias duke");

        KeyStore ks = KeyStore.getInstance(new File("ks"),
                                           "changeit".toCharArray());
        X509Certificate cert = (X509Certificate)ks.getCertificate("duke");
        X509Certificate root = (X509Certificate)ks.getCertificate("root");

        // validate chain
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        PKIXRevocationChecker prc =
            (PKIXRevocationChecker)cpv.getRevocationChecker();
        prc.setOptions(EnumSet.of(PREFER_CRLS, NO_FALLBACK, SOFT_FAIL));
        PKIXParameters params =
            new PKIXParameters(Set.of(new TrustAnchor(root, null)));
        params.addCertPathChecker(prc);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath cp = cf.generateCertPath(List.of(cert));
        cpv.validate(cp, params);

        // unwrap soft fail exceptions and check for SocketTimeoutException
        boolean expected = false;
        for (CertPathValidatorException softFail:prc.getSoftFailExceptions()) {
            Throwable cause = softFail.getCause();
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    expected = true;
                    break;
                }
                cause = cause.getCause();
            }
            if (expected) {
                break;
            }
        }
        if (!expected) {
            throw new Exception("SocketTimeoutException not thrown");
        }
    }

    private static OutputAnalyzer keytool(String cmd) throws Exception {
        return SecurityTools.keytool("-storepass changeit "
                + "-keystore ks " + cmd);
    }

    private static class CrlHttpServer {

        private final HttpServer server;
        private final int timeout;

        public CrlHttpServer(int timeout) throws IOException {
            server = HttpServer.create();
            this.timeout = timeout;
        }

        public void start() throws IOException {
            server.bind(new InetSocketAddress(0), 0);
            server.createContext("/", t -> {
                try (InputStream is = t.getRequestBody()) {
                    is.readAllBytes();
                }
                try {
                    // sleep for 2 seconds longer to force timeout
                    Thread.sleep((timeout + 2)*1000);
                } catch (InterruptedException ie) {
                    throw new IOException(ie);
                }
            });
            server.setExecutor(null);
            server.start();
        }

        public void stop() {
            server.stop(0);
        }

        int getPort() {
            return server.getAddress().getPort();
        }
    }
}
