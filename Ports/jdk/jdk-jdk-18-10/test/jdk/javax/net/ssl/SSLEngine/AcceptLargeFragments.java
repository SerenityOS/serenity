/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6388456
 * @summary Need adjustable TLS max record size for interoperability
 *      with non-compliant stacks
 * @run main/othervm AcceptLargeFragments
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 *
 * Check the system property "jsse.SSLEngine.acceptLargeFragments"
 *
 * @author xuelei fan
 */

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

public class AcceptLargeFragments {
    public static void main (String[] args) throws Exception {
        SSLContext context = SSLContext.getDefault();

        // set the property before initialization SSLEngine.
        System.setProperty("jsse.SSLEngine.acceptLargeFragments", "true");

        SSLEngine cliEngine = context.createSSLEngine();
        cliEngine.setUseClientMode(true);

        SSLEngine srvEngine = context.createSSLEngine();
        srvEngine.setUseClientMode(false);

        SSLSession cliSession = cliEngine.getSession();
        SSLSession srvSession = srvEngine.getSession();

        // check packet buffer sizes.
        if (cliSession.getPacketBufferSize() < 33049 ||
            srvSession.getPacketBufferSize() < 33049) {
                throw new Exception("Don't accept large SSL/TLS fragments");
        }

        // check application data buffer sizes.
        if (cliSession.getApplicationBufferSize() < 32768 ||
            srvSession.getApplicationBufferSize() < 32768) {
                throw new Exception(
                        "Don't accept large SSL/TLS application data ");
        }
    }
}
