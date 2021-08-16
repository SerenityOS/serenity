/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6449579
 * @summary DefaultSSLServerSocketFactory does not override createServerSocket()
 * @run main/othervm DefaultSSLServSocketFac
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */
import java.security.Security;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class DefaultSSLServSocketFac {
    public static void main(String[] args) throws Exception {
        // reserve the security properties
        String reservedSSFacProvider =
            Security.getProperty("ssl.ServerSocketFactory.provider");

        try {
            Security.setProperty("ssl.ServerSocketFactory.provider", "oops");
            ServerSocketFactory ssocketFactory =
                        SSLServerSocketFactory.getDefault();
            SSLServerSocket sslServerSocket =
                        (SSLServerSocket)ssocketFactory.createServerSocket();
        } catch (Exception e) {
            if (!(e.getCause() instanceof ClassNotFoundException)) {
                throw e;
            }
            // get the expected exception
        } finally {
            // restore the security properties
            if (reservedSSFacProvider == null) {
                reservedSSFacProvider = "";
            }
            Security.setProperty("ssl.ServerSocketFactory.provider",
                                                    reservedSSFacProvider);
        }
    }
}
