/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4965541
 * @summary verify that setSessionTimeout() with large values works
 * @run main/othervm Timeout
 * @author Andreas Sterbenz
 */

// The SunJSSE provider cannot use System Properties in samevm/agentvm mode.
// Please run JSSE test in othervm mode.

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;

public class Timeout {

    public static void main(String[] args) throws Exception {
        SSLServerSocketFactory ssf =
                (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        try (SSLServerSocket ss = (SSLServerSocket)ssf.createServerSocket()) {
            String[] protocols = ss.getSupportedProtocols();
            for (int i = 0; i < protocols.length; i++) {
                if (protocols[i].equals("SSLv2Hello")) {
                    continue;
                }
                SSLContext sslc = SSLContext.getInstance(protocols[i]);
                SSLSessionContext sslsc = sslc.getServerSessionContext();
                System.out.println("Protocol: " + protocols[i]);
                sslsc.setSessionTimeout(Integer.MAX_VALUE);
                int newtime = sslsc.getSessionTimeout();
                if (newtime != Integer.MAX_VALUE) {
                    throw new Exception ("Expected timeout: " +
                            Integer.MAX_VALUE + ", got instead: " +
                            newtime);
                }
            }
        }
        System.out.println("Finished");
    }
}
