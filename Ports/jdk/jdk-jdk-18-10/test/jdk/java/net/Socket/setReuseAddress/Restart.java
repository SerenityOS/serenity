/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4476378
 * @library /test/lib
 * @summary Check that SO_REUSEADDR allows a server to restart
 *          after a crash.
 * @run main Restart
 * @run main/othervm -Dsun.net.useExclusiveBind Restart
 * @run main/othervm -Dsun.net.useExclusiveBind=true Restart
 * @run main/othervm -Djava.net.preferIPv4Stack=true Restart
 * @run main/othervm -Dsun.net.useExclusiveBind
 *                   -Djava.net.preferIPv4Stack=true Restart
 * @run main/othervm -Dsun.net.useExclusiveBind=true
 *                   -Djava.net.preferIPv4Stack=true Restart
 */
import java.net.*;
import jdk.test.lib.net.IPSupport;

public class Restart {

    /*
     * Test that a server can bind to the same port after
     * a crash -- ie: while previous connection still in
     * TIME_WAIT state we should be able to re-bind if
     * SO_REUSEADDR is enabled.
     */

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        InetAddress localHost = InetAddress.getLocalHost();
        ServerSocket ss = new ServerSocket(0, 0, localHost);
        Socket s1 = null, s2 = null;
        try {
            int port = ss.getLocalPort();

            s1 = new Socket(localHost, port);
            s2 = ss.accept();

            // close server socket and the accepted connection
            ss.close();
            s2.close();

            ss = new ServerSocket();
            ss.bind( new InetSocketAddress(localHost, port) );
            ss.close();

            // close the client socket
            s1.close();
        } catch (BindException be) {
            if (System.getProperty("sun.net.useExclusiveBind") != null) {
                // exclusive bind, expected exception
            } else {
                throw be;
            }
        } finally {
            if (ss != null) ss.close();
            if (s1 != null) s1.close();
            if (s2 != null) s2.close();
        }
    }
}
