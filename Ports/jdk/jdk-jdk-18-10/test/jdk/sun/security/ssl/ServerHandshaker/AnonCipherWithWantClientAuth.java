/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4392475
 * @modules jdk.crypto.ec
 * @library /javax/net/ssl/templates
 * @summary Calling setWantClientAuth(true) disables anonymous suites
 * @run main/othervm -Djavax.net.debug=all AnonCipherWithWantClientAuth
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import javax.net.ssl.SSLSocket;

public class AnonCipherWithWantClientAuth extends SSLSocketTemplate {
    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        (new AnonCipherWithWantClientAuth()).run();
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        String ciphers[] = {
                "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" };
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
                "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5" };
        socket.setEnabledCipherSuites(ciphers);
        socket.setUseClientMode(true);

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();
    }
}
