/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387882
 * @summary Need to revisit the javadocs for JSSE, especially the
 *      promoted classes
 * @author Brad Wetmore
 */

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;


/*
 * Tests that the null argument changes made it in ok.
 */

public class KMTMGetNothing {

    KMTMGetNothing() throws Exception {
        char[] passphrase = "none".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        X509KeyManager km = (X509KeyManager) kmf.getKeyManagers()[0];

        if (km.getCertificateChain(null) != null) {
            throw new Exception("km.getCertificateChain(null) != null");
        }

        if (km.getCertificateChain("fubar") != null) {
            throw new Exception("km.getCertificateChain(\"fubar\") != null");
        }

        if (km.getPrivateKey(null) != null) {
            throw new Exception("km.getPrivateKey(null) != null");
        }

        if (km.getPrivateKey("fubar") != null) {
            throw new Exception("km.getPrivateKey(\"fubar\") != null");
        }
        System.out.println("KM TESTS PASSED");

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        X509TrustManager tm = (X509TrustManager) tmf.getTrustManagers()[0];

        if ((tm.getAcceptedIssuers() == null) ||
                (tm.getAcceptedIssuers().length != 0)) {
            throw new Exception("tm.getAcceptedIssuers() != null");
        }
        System.out.println("TM TESTS PASSED");

        System.out.println("ALL TESTS PASSED");
    }

    public static void main(String[] args) throws Exception {
        new KMTMGetNothing();
    }
}
