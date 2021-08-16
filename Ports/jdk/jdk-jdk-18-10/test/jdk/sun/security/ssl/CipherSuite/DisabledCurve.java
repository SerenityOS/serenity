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

/*
 * @test
 * @bug 8246330
 * @library /javax/net/ssl/templates /test/lib
 * @run main/othervm -Djdk.tls.namedGroups="secp384r1"
        DisabledCurve DISABLE_NONE PASS
 * @run main/othervm -Djdk.tls.namedGroups="secp384r1"
        DisabledCurve secp384r1 FAIL
*/
import java.security.Security;
import java.util.Arrays;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import jdk.test.lib.security.SecurityUtils;

public class DisabledCurve extends SSLSocketTemplate {

    private static volatile int index;
    private static final String[][][] protocols = {
            { { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" }, { "TLSv1.2" } },
            { { "TLSv1.2" }, { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" } },
            { { "TLSv1.2" }, { "TLSv1.2" } }, { { "TLSv1.1" }, { "TLSv1.1" } },
            { { "TLSv1" }, { "TLSv1" } } };

    protected SSLContext createClientSSLContext() throws Exception {
        return createSSLContext(
                new SSLSocketTemplate.Cert[] {
                        SSLSocketTemplate.Cert.CA_ECDSA_SECP384R1 },
                new SSLSocketTemplate.Cert[] {
                        SSLSocketTemplate.Cert.EE_ECDSA_SECP384R1 },
                getClientContextParameters());
    }

    protected SSLContext createServerSSLContext() throws Exception {
        return createSSLContext(
                new SSLSocketTemplate.Cert[] {
                        SSLSocketTemplate.Cert.CA_ECDSA_SECP384R1 },
                new SSLSocketTemplate.Cert[] {
                        SSLSocketTemplate.Cert.EE_ECDSA_SECP384R1 },
                getServerContextParameters());
    }

    @Override
    protected void configureClientSocket(SSLSocket socket) {
        String[] ps = protocols[index][0];

        System.out.print("Setting client protocol(s): ");
        Arrays.stream(ps).forEachOrdered(System.out::print);
        System.out.println();

        socket.setEnabledProtocols(ps);
    }

    @Override
    protected void configureServerSocket(SSLServerSocket serverSocket) {
        String[] ps = protocols[index][1];

        System.out.print("Setting server protocol(s): ");
        Arrays.stream(ps).forEachOrdered(System.out::print);
        System.out.println();

        serverSocket.setEnabledProtocols(ps);
    }

    public static void main(String[] args) throws Exception {
        String expected = args[1];
        String disabledName = ("DISABLE_NONE".equals(args[0]) ? "" : args[0]);
        boolean disabled = false;
        if (disabledName.equals("")) {
            Security.setProperty("jdk.disabled.namedCurves", "");
        } else {
            disabled = true;
            Security.setProperty("jdk.certpath.disabledAlgorithms", "secp384r1");
        }

        // Re-enable TLSv1 and TLSv1.1 since test depends on it.
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1", "TLSv1.1");

        for (index = 0; index < protocols.length; index++) {
            try {
                (new DisabledCurve()).run();
                if (expected.equals("FAIL")) {
                    throw new RuntimeException(
                            "Expected test to fail, but it passed");
                }
            } catch (SSLException | IllegalStateException ssle) {
                if (expected.equals("FAIL") && disabled) {
                    System.out.println(
                            "Expected exception was thrown: TEST PASSED");
                } else {
                    throw new RuntimeException(ssle);
                }

            }

        }
    }
}
