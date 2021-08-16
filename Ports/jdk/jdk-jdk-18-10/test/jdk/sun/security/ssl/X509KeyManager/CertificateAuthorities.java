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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8206925
 * @summary Support the "certificate_authorities" extension
 * @library /javax/net/ssl/templates
 * @run main/othervm CertificateAuthorities
 * @run main/othervm -Djdk.tls.client.enableCAExtension=false
 *     CertificateAuthorities
 * @run main/othervm -Djdk.tls.client.enableCAExtension=true
 *     CertificateAuthorities
 *
 * @run main/othervm CertificateAuthorities NEED_CLIENT_AUTH
 * @run main/othervm -Djdk.tls.client.enableCAExtension=false
 *     CertificateAuthorities NEED_CLIENT_AUTH
 * @run main/othervm -Djdk.tls.client.enableCAExtension=true
 *     CertificateAuthorities NEED_CLIENT_AUTH
 *
 * @run main/othervm CertificateAuthorities WANT_CLIENT_AUTH
 * @run main/othervm -Djdk.tls.client.enableCAExtension=false
 *     CertificateAuthorities WANT_CLIENT_AUTH
 * @run main/othervm -Djdk.tls.client.enableCAExtension=true
 *     CertificateAuthorities WANT_CLIENT_AUTH
 */

import javax.net.ssl.SSLServerSocket;

public final class CertificateAuthorities extends SSLSocketTemplate {
    final ClientAuthMode clientAuthMode;

    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        CertificateAuthorities testCase;
        if (args.length != 0) {
            testCase = new CertificateAuthorities(
                    ClientAuthMode.valueOf(args[0]));
        } else {
            testCase = new CertificateAuthorities(
                    ClientAuthMode.NO_CLIENT_AUTH);
        }

        testCase.run();
    }

    CertificateAuthorities(ClientAuthMode mode) {
        this.clientAuthMode = mode;
    }

    @Override
    protected void configureServerSocket(SSLServerSocket socket) {
        if (clientAuthMode == ClientAuthMode.NEED_CLIENT_AUTH) {
            socket.setNeedClientAuth(true);
        } else if (clientAuthMode == ClientAuthMode.WANT_CLIENT_AUTH) {
            socket.setWantClientAuth(true);
        }
    }

    private static enum ClientAuthMode {
        NEED_CLIENT_AUTH,
        WANT_CLIENT_AUTH,
        NO_CLIENT_AUTH
    }
}
