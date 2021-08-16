/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5070632
 * @summary Default SSLSockeFactory override createSocket() now
 * @run main/othervm Fix5070632
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Weijun Wang
 */

import javax.net.ssl.SSLSocketFactory;
import java.net.SocketException;
import javax.net.SocketFactory;
import java.security.*;

public class Fix5070632 {
    public static void main(String[] args) throws Exception {
        // reserve the security properties
        String reservedSFacProvider =
            Security.getProperty("ssl.SocketFactory.provider");

        // use a non-existing provider so that the DefaultSSLSocketFactory
        // will be used, and then test against it.

        Security.setProperty("ssl.SocketFactory.provider", "foo.NonExistant");
        SSLSocketFactory fac = (SSLSocketFactory)SSLSocketFactory.getDefault();
        try {
            fac.createSocket();
        } catch(SocketException se) {
            // if exception caught, then it's ok
            System.out.println("Throw SocketException");
            se.printStackTrace();
            return;
        } finally {
            // restore the security properties
            if (reservedSFacProvider == null) {
                reservedSFacProvider = "";
            }
            Security.setProperty("ssl.SocketFactory.provider",
                                                reservedSFacProvider);
        }

        // if not caught, or other exception caught, then it's error
        throw new Exception("should throw SocketException");
    }
}
