/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Testing SSLEngines do not enable RC4 ciphers by default.
 */
public class NotEnabledRC4Test {

    public static void main(String[] s) throws Exception {
        SSLContext context = SSLEngineTestCase.getContext();
        SSLEngine clientEngine = context.createSSLEngine();
        clientEngine.setUseClientMode(true);
        SSLEngine serverEngine = context.createSSLEngine();
        serverEngine.setUseClientMode(false);
        String[] cliEnabledCiphers = clientEngine.getEnabledCipherSuites();
        rc4Test(cliEnabledCiphers, true);
        String[] srvEnabledCiphers = serverEngine.getEnabledCipherSuites();
        rc4Test(srvEnabledCiphers, false);
    }

    private static void rc4Test(String[] ciphers, boolean isClient) {
        String mode = isClient ? "client" : "server";
        for (String cipher : ciphers) {
            if (cipher.contains("RC4")) {
                throw new AssertionError("RC4 cipher " + cipher + " is enabled"
                        + " by default on " + mode + " SSLEngine,"
                        + " but it should not!");
            }
        }
    }
}
