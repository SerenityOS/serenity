/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4635454 6208022 8130181
 * @summary Check pluggability of SSLContext class.
 */
import java.security.*;
import java.net.*;
import javax.net.ssl.*;

public class CheckSSLContextExport extends Provider {
    private static String info = "test provider for JSSE pluggability";

    public CheckSSLContextExport(String protocols[]) {
        super("TestJSSEPluggability", "1.0", info);
        for (int i=0; i<protocols.length; i++) {
            put("SSLContext." + protocols[i], "MySSLContextImpl");
        }
    }

    public static void test(String protocol) throws Exception {
        SSLContext mySSLContext = SSLContext.getInstance(protocol);

        String providerName = mySSLContext.getProvider().getName();
        if (!providerName.equals("TestJSSEPluggability")) {
            System.out.println(providerName + "'s SSLContext is used");
            throw new Exception("...used the wrong provider: " + providerName);
        }
        for (int i = 0; i < 2; i++) {
            boolean standardCiphers = true;

            switch (i) {
            case 0:
                // non-standard cipher suites
                standardCiphers = false;
                MySSLContextImpl.useCustomCipherSuites();
                break;
            case 1:
                standardCiphers = true;
                MySSLContextImpl.useStandardCipherSuites();
                break;
            default:
                throw new Exception("Internal Test Error!");
            }
            System.out.println("Testing with " +
                (standardCiphers ? "standard" : "custom") + " cipher suites");
            for (int j = 0; j < 4; j++) {
                String clsName = null;
                try {
                    switch (j) {
                    case 0:
                        SSLSocketFactory sf = mySSLContext.getSocketFactory();
                        clsName = sf.getClass().getName();
                        break;
                    case 1:
                        SSLServerSocketFactory ssf =
                            mySSLContext.getServerSocketFactory();
                        clsName = ssf.getClass().getName();
                        break;
                    case 2:
                        SSLEngine se = mySSLContext.createSSLEngine();
                        clsName = se.getClass().getName();
                        break;
                    case 3:
                        SSLEngine se2 = mySSLContext.createSSLEngine(null, 0);
                        clsName = se2.getClass().getName();
                        break;
                    default:
                        throw new Exception("Internal Test Error!");
                    }
                    if (!clsName.startsWith("MySSL")) {
                        throw new Exception("test#" + j +
                                             ": wrong impl is used");
                    } else {
                        System.out.println("test#" + j +
                                           ": accepted valid impl");
                    }
                } catch (RuntimeException re) {
                    // pass it on
                    throw re;
                }
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        String protocols[] = { "SSL", "TLS" };
        Provider extraProvider = new CheckSSLContextExport(protocols);
        Security.insertProviderAt(extraProvider, 1);
        try {
            for (int i = 0; i < protocols.length; i++) {
                System.out.println("Testing " + protocols[i] + "'s SSLContext");
                test(protocols[i]);
            }
            System.out.println("Test Passed");
        } finally {
            Security.removeProvider(extraProvider.getName());
        }
    }
}
