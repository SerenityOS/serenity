/*
 * Copyright (c) 2018 Google Inc. All rights reserved.
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

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
 * @test
 * @bug 8194960
 * @summary Sanity check trust manager defaults/cacerts.
 */

/**
 * Explores the set of root certificates.
 * Also useful as a standalone program.
 *
 * Prior to JEP 319, stock openjdk fails this because no root
 * certificates were checked into the repo.
 */
public class CacertsExplorer {
    public static void main(String[] args) throws Throwable {
        String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        if (!defaultAlgorithm.equals("PKIX")) {
            throw new AssertionError(
                "Expected default algorithm PKIX, got " + defaultAlgorithm);
        }

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(defaultAlgorithm);
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1) {
            throw new AssertionError(
                "Expected exactly one TrustManager, got "
                + Arrays.toString(trustManagers));
        }
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();
        if (acceptedIssuers.length == 0) {
            throw new AssertionError(
                "no accepted issuers - cacerts file configuration problem?");
        }
        Arrays.stream(acceptedIssuers)
            .map(X509Certificate::getIssuerX500Principal)
            .forEach(System.out::println);
    }
}
