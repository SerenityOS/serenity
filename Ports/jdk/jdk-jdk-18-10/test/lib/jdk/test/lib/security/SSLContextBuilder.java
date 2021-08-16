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

package jdk.test.lib.security;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/*
 * SSL context builder.
 */
public class SSLContextBuilder {

    // Trust store
    private KeyStore trustStore = null;

    // Key store
    private KeyStore keyStore = null;

    // Trust manager factory algorithm
    private String tmfAlgo = TrustManagerFactory.getDefaultAlgorithm();

    // Key manager factory algorithm
    private String kmfAlgo = KeyManagerFactory.getDefaultAlgorithm();

    // Key manager factory passphrase
    private String kmfPassphrase = null;

    // Context protocol
    private String protocol = "TLS";

    private SecureRandom random = null;

    public SSLContextBuilder trustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public SSLContextBuilder keyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    public SSLContextBuilder tmfAlgo(String tmfAlgo) {
        this.tmfAlgo = tmfAlgo;
        return this;
    }

    public SSLContextBuilder kmfAlgo(String kmfAlgo) {
        this.kmfAlgo = kmfAlgo;
        return this;
    }

    public SSLContextBuilder kmfPassphrase(String kmfPassphrase) {
        this.kmfPassphrase = kmfPassphrase;
        return this;
    }

    public SSLContextBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public SSLContextBuilder random(SecureRandom random) {
        this.random = random;
        return this;
    }

    public SSLContext build() throws Exception {
        return buildSSLContext(
                trustStore, keyStore,
                tmfAlgo, kmfAlgo, kmfPassphrase,
                protocol, random);
    }

    public static SSLContextBuilder builder() {
        return new SSLContextBuilder();
    }

    /**
     * The default TLS context.
     */
    public static SSLContext defaultTLSContext() throws Exception {
        return builder()
                .trustStore(KeyStoreUtils.defaultTrustStore())
                .keyStore(KeyStoreUtils.defaultKeyStore())
                .build();
    }

    /**
     * The default DTLS context.
     */
    public static SSLContext defaultDTLSContext() throws Exception {
        return builder()
                .trustStore(KeyStoreUtils.defaultTrustStore())
                .keyStore(KeyStoreUtils.defaultKeyStore())
                .protocol("DTLS")
                .build();
    }

    private static SSLContext buildSSLContext(
            KeyStore trustStore, KeyStore keyStore,
            String tmfAlgo, String kmfAlgo, String kmfPassphrase,
            String protocol, SecureRandom random) throws Exception {
        TrustManagerFactory tmf = null;
        if (trustStore != null) {
            tmf = TrustManagerFactory.getInstance(tmfAlgo);
            tmf.init(trustStore);
        }

        KeyManagerFactory kmf = null;
        if (keyStore != null) {
            kmf = KeyManagerFactory.getInstance(kmfAlgo);
            kmf.init(keyStore,
                    kmfPassphrase == null ? null : kmfPassphrase.toCharArray());
        }

        SSLContext context = SSLContext.getInstance(protocol);
        context.init(
                kmf == null ? null : kmf.getKeyManagers(),
                tmf == null ? null : tmf.getTrustManagers(),
                random);
        return context;
    }
}
