/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049432 8069038 8234723 8202343
 * @summary New tests for TLS property jdk.tls.client.protocols
 * @summary javax/net/ssl/TLS/TLSClientPropertyTest.java needs to be
 *     updated for JDK-8061210
 * @modules java.security.jgss
 *          java.security.jgss/sun.security.jgss.krb5
 *          java.security.jgss/sun.security.krb5:+open
 *          java.security.jgss/sun.security.krb5.internal:+open
 *          java.security.jgss/sun.security.krb5.internal.ccache
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.ktab
 *          java.base/sun.security.util
 * @run main/othervm TLSClientPropertyTest NoProperty
 * @run main/othervm TLSClientPropertyTest SSLv3
 * @run main/othervm TLSClientPropertyTest TLSv1
 * @run main/othervm TLSClientPropertyTest TLSv11
 * @run main/othervm TLSClientPropertyTest TLSv12
 * @run main/othervm TLSClientPropertyTest TLSv13
 * @run main/othervm TLSClientPropertyTest TLS
 * @run main/othervm TLSClientPropertyTest WrongProperty
 */

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;

/**
 * Sets the property jdk.tls.client.protocols to one of this protocols:
 * SSLv3,TLSv1,TLSv1.1,TLSv1.2 and TLSV(invalid) or removes this
 * property (if any),then validates the default, supported and current
 * protocols in the SSLContext.
 */
public class TLSClientPropertyTest {
    private final String[] expectedSupportedProtos = new String[] {
            "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"
    };

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            throw new RuntimeException(
                    "Incorrect arguments,expected arguments: testCase");
        }

        String[] expectedDefaultProtos;
        String testCase = args[0];
        String contextProtocol;
        switch (testCase) {
        case "NoProperty":
            if (System.getProperty("jdk.tls.client.protocols") != null) {
                System.getProperties().remove("jdk.tls.client.protocols");
            }
            contextProtocol = null;
            expectedDefaultProtos = new String[] {
                    "TLSv1.2", "TLSv1.3"
            };
            break;
        case "SSLv3":
            contextProtocol = "SSLv3";
            expectedDefaultProtos = new String[] {
            };
            break;
        case "TLSv1":
            contextProtocol = "TLSv1";
            expectedDefaultProtos = new String[] {
            };
            break;
        case "TLSv11":
            contextProtocol = "TLSv1.1";
            expectedDefaultProtos = new String[] {
            };
            break;
        case "TLSv12":
            contextProtocol = "TLSv1.2";
            expectedDefaultProtos = new String[] {
                    "TLSv1.2"
            };
            break;
        case "TLSv13":
        case "TLS":
            contextProtocol = "TLSv1.3";
            expectedDefaultProtos = new String[] {
                    "TLSv1.2", "TLSv1.3"
            };
            break;
        case "WrongProperty":
            expectedDefaultProtos = new String[] {};
            contextProtocol = "TLSV";
            break;
        default:
            throw new RuntimeException("test case is wrong");
        }
        if (contextProtocol != null) {
            System.setProperty("jdk.tls.client.protocols", contextProtocol);
        }
        try {
            TLSClientPropertyTest test = new TLSClientPropertyTest();
            test.test(contextProtocol, expectedDefaultProtos);
            if (testCase.equals("WrongProperty")) {
                throw new RuntimeException(
                        "Test failed: NoSuchAlgorithmException " +
                        "is expected when input wrong protocol");
            } else {
                System.out.println("Test " + contextProtocol + " passed");
            }
        } catch (NoSuchAlgorithmException nsae) {
            if (testCase.equals("WrongProperty")) {
                System.out.println("NoSuchAlgorithmException is expected,"
                        + contextProtocol + " test passed");
            } else {
                throw nsae;
            }
        }

    }

    /**
     * The parameter passed is the user enforced protocol. Does not catch
     * NoSuchAlgorithmException, WrongProperty test will use it.
     */
    public void test(String expectedContextProto,
            String[] expectedDefaultProtos) throws NoSuchAlgorithmException {

        SSLContext context = null;
        try {
            if (expectedContextProto != null) {
                context = SSLContext.getInstance(expectedContextProto);
                context.init(null, null, null);
            } else {
                context = SSLContext.getDefault();
            }
            printContextDetails(context);
        } catch (KeyManagementException ex) {
            error(null, ex);
        }

        validateContext(expectedContextProto, expectedDefaultProtos, context);
    }

    /**
     * Simple print utility for SSLContext's protocol details.
     */
    private void printContextDetails(SSLContext context) {
        System.out.println("Default   Protocols: "
                + Arrays.toString(context.getDefaultSSLParameters()
                        .getProtocols()));
        System.out.println("Supported Protocols: "
                + Arrays.toString(context.getSupportedSSLParameters()
                        .getProtocols()));
        System.out.println("Current   Protocol : " + context.getProtocol());

    }

    /**
     * Error handler.
     */
    private void error(String msg, Throwable tble) {
        String finalMsg = "FAILED " + (msg != null ? msg : "");
        if (tble != null) {
            throw new RuntimeException(finalMsg, tble);
        }
        throw new RuntimeException(finalMsg);
    }

    /**
     * Validates the SSLContext's protocols against the user enforced protocol.
     */
    private void validateContext(String expectedProto,
            String[] expectedDefaultProtos, SSLContext context) {
        if (expectedProto == null) {
            expectedProto = "Default";
        }
        if (!context.getProtocol().equals(expectedProto)) {
            error("Invalid current protocol: " + context.getProtocol()
                    + ", Expected:" + expectedProto, null);
        }
        List<String> actualDefaultProtos = Arrays.asList(context
                .getDefaultSSLParameters().getProtocols());
        for (String p : expectedDefaultProtos) {
            if (!actualDefaultProtos.contains(p)) {
                error("Default protocol " + p + "missing", null);
            }
        }
        List<String> actualSupportedProtos = Arrays.asList(context
                .getSupportedSSLParameters().getProtocols());

        for (String p : expectedSupportedProtos) {
            if (!actualSupportedProtos.contains(p)) {
                error("Expected to support protocol:" + p, null);
            }
        }
    }
}
