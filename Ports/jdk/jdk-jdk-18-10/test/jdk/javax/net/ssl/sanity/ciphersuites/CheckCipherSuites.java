/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4750141 4895631 8217579 8163326
 * @summary Check enabled and supported ciphersuites are correct
 * @run main/othervm CheckCipherSuites default
 * @run main/othervm CheckCipherSuites limited
 */

import java.util.*;
import java.security.Security;
import javax.net.ssl.*;

public class CheckCipherSuites {

    // List of enabled cipher suites when the "crypto.policy" security
    // property is set to "unlimited" (the default value).
    private final static String[] ENABLED_DEFAULT = {
        // TLS 1.3 cipher suites
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256",
        "TLS_CHACHA20_POLY1305_SHA256",

        // Suite B compliant cipher suites
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",

        // Not suite B, but we want it to position the suite early
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",

        // AES_256(GCM) - ECDHE - forward screcy
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",

        // AES_128(GCM) - ECDHE - forward screcy
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",

        // AES_256(GCM) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",

        // AES_128(GCM) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",

        // AES_256(CBC) - ECDHE - forward screcy
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",

        // AES_256(CBC) - ECDHE - forward screcy
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",

        // AES_256(CBC) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",

        // AES_128(CBC) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",

        // AES_256(GCM) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",

        // AES_128(GCM) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",

        // AES_256(CBC) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",

        // AES_128(CBC) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",

        // AES_256(CBC) - ECDHE - using SHA
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - ECDHE - using SHA
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",

        // AES_256(CBC) - DHE - using SHA
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - DHE - using SHA
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",

        // AES_256(CBC) - using SHA, not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - using SHA, not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",

        // deprecated
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    };

    // List of enabled cipher suites when the "crypto.policy" security
    // property is set to "limited".
    private final static String[] ENABLED_LIMITED = {
        "TLS_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    };

    // List of supported cipher suites when the "crypto.policy" security
    // property is set to "unlimited" (the default value).
    private final static String[] SUPPORTED_DEFAULT = {
        // TLS 1.3 cipher suites
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256",
        "TLS_CHACHA20_POLY1305_SHA256",

        // Suite B compliant cipher suites
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",

        // Not suite B, but we want it to position the suite early
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",

        // AES_256(GCM) - ECDHE - forward screcy
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",

        // AES_128(GCM) - ECDHE - forward screcy
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",

        // AES_256(GCM) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",

        // AES_128(GCM) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",

        // AES_256(CBC) - ECDHE - forward screcy
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",

        // AES_256(CBC) - ECDHE - forward screcy
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",

        // AES_256(CBC) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",

        // AES_128(CBC) - DHE - forward screcy
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",

        // AES_256(GCM) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",

        // AES_128(GCM) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",

        // AES_256(CBC) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",

        // AES_128(CBC) - not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",

        // AES_256(CBC) - ECDHE - using SHA
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - ECDHE - using SHA
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",

        // AES_256(CBC) - DHE - using SHA
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - DHE - using SHA
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",

        // AES_256(CBC) - using SHA, not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",

        // AES_128(CBC) - using SHA, not forward screcy
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",

        // deprecated
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    };

    // List of supported cipher suites when the "crypto.policy" security
    // property is set to "limited".
    private final static String[] SUPPORTED_LIMITED = {
        "TLS_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    };

    private static void showSuites(String[] suites) {
        if ((suites == null) || (suites.length == 0)) {
            System.out.println("<none>");
        }
        for (int i = 0; i < suites.length; i++) {
            System.out.println("  " + suites[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        if (args.length != 1) {
            throw new Exception("One arg required");
        }

        String[] ENABLED;
        String[] SUPPORTED;
        if (args[0].equals("default")) {
            ENABLED = ENABLED_DEFAULT;
            SUPPORTED = SUPPORTED_DEFAULT;
        } else if (args[0].equals("limited")) {
            Security.setProperty("crypto.policy", "limited");
            ENABLED = ENABLED_LIMITED;
            SUPPORTED = SUPPORTED_LIMITED;
        } else {
            throw new Exception("Illegal argument");
        }

        SSLSocketFactory factory =
                (SSLSocketFactory)SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket)factory.createSocket();
        String[] enabled = socket.getEnabledCipherSuites();

        System.out.println("Default enabled ciphersuites:");
        showSuites(enabled);

        if (Arrays.equals(ENABLED, enabled) == false) {
            System.out.println("*** MISMATCH, should be ***");
            showSuites(ENABLED);
            throw new Exception("Enabled ciphersuite mismatch");
        }
        System.out.println("OK");
        System.out.println();

        String[] supported = socket.getSupportedCipherSuites();
        System.out.println("Supported ciphersuites:");
        showSuites(supported);

        if (Arrays.equals(SUPPORTED, supported) == false) {
            System.out.println("*** MISMATCH, should be ***");
            showSuites(SUPPORTED);
            throw new Exception("Supported ciphersuite mismatch");
        }
        System.out.println("OK");

        long end = System.currentTimeMillis();
        System.out.println("Done (" + (end - start) + " ms).");
    }
}
