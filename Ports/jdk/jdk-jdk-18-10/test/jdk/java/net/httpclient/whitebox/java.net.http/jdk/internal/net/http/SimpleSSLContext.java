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

package jdk.internal.net.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Creates a simple usable SSLContext for SSLSocketFactory
 * or a HttpsServer using a default keystore in the test tree.
 * <p>
 * Using this class with a security manager requires the following
 * permissions to be granted:
 * <p>
 * permission "java.util.PropertyPermission" "test.src.path", "read";
 * permission java.io.FilePermission "/path/to/test/lib/jdk/test/lib/testkeys", "read";
 * The exact path above depends on the location of the test.
 */
public class SimpleSSLContext {

    private final SSLContext ssl;

    /**
     * Loads default keystore from SimpleSSLContext source directory
     */
    public SimpleSSLContext() throws IOException {
        String paths = System.getProperty("test.src.path");
        StringTokenizer st = new StringTokenizer(paths, File.pathSeparator);
        boolean securityExceptions = false;
        SSLContext sslContext = null;
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            try {
                File f = new File(path, "../../../../../lib/jdk/test/lib/net/testkeys");
                if (f.exists()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        sslContext = init(fis);
                        break;
                    }
                }
            } catch (SecurityException e) {
                // catch and ignore because permission only required
                // for one entry on path (at most)
                securityExceptions = true;
            }
        }
        if (securityExceptions) {
            System.out.println("SecurityExceptions thrown on loading testkeys");
        }
        ssl = sslContext;
    }

    private SSLContext init(InputStream i) throws IOException {
        try {
            char[] passphrase = "passphrase".toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(i, passphrase);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ks);

            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return ssl;
        } catch (KeyManagementException | KeyStoreException |
                UnrecoverableKeyException | CertificateException |
                NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public SSLContext get() {
        return ssl;
    }
}