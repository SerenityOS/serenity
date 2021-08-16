/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7142172
 * @summary Custom TrustManagers that return null for getAcceptedIssuers
 *          will NPE.
 *     SunJSSE does not support dynamic system properties, no way to
 *     re-use system properties in samevm/agentvm mode.
 * @run main/othervm NullGetAcceptedIssuers
 */

import javax.net.ssl.*;

public class NullGetAcceptedIssuers {

    public static void main(String[] args) throws Exception {
        SSLContext sslContext;

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts =
            new TrustManager[] {new X509TrustManager() {

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType) {
                }

                // API says empty array, but some custom TMs are
                // returning null.
                public java.security.cert.X509Certificate[]
                        getAcceptedIssuers() {
                    return null;
                }
            }};

        sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, null);
    }
}
