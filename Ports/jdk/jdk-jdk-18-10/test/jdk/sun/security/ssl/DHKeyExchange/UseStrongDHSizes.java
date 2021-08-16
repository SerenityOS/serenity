/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8140436
 * @modules jdk.crypto.ec
 * @library /javax/net/ssl/templates
 * @summary Negotiated Finite Field Diffie-Hellman Ephemeral Parameters for TLS
 * @run main/othervm UseStrongDHSizes 2048
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe2048 UseStrongDHSizes 2048
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe3072 UseStrongDHSizes 2048
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe4096 UseStrongDHSizes 2048
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe6144 UseStrongDHSizes 2048
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe8192 UseStrongDHSizes 2048
 * @run main/othervm UseStrongDHSizes 3072
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe3072 UseStrongDHSizes 3072
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe4096 UseStrongDHSizes 3072
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe6144 UseStrongDHSizes 3072
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe8192 UseStrongDHSizes 3072
 * @run main/othervm UseStrongDHSizes 4096
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe4096 UseStrongDHSizes 4096
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe6144 UseStrongDHSizes 4096
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe8192 UseStrongDHSizes 4096
 * @run main/othervm UseStrongDHSizes 6144
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe6144 UseStrongDHSizes 6144
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe8192 UseStrongDHSizes 6144
 * @run main/othervm UseStrongDHSizes 8192
 * @run main/othervm -Djdk.tls.namedGroups=ffdhe8192 UseStrongDHSizes 8192
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import javax.net.ssl.SSLSocket;

public class UseStrongDHSizes extends SSLSocketTemplate {
    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled unexpectedly.
        String constraint = "DH keySize < " + Integer.valueOf(args[0]);
        Security.setProperty("jdk.tls.disabledAlgorithms", constraint);
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        (new UseStrongDHSizes()).run();
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        String ciphers[] = {
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA"};

        socket.setEnabledCipherSuites(ciphers);
        socket.setWantClientAuth(true);

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        String ciphers[] = {
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA"};
        socket.setEnabledCipherSuites(ciphers);
        socket.setUseClientMode(true);

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();
    }
}
