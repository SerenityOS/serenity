/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check the specific behaviour of the setReuseAddress(boolean)
 *          method.
 * @run main Basic
 * @run main/othervm -Dsun.net.useExclusiveBind Basic
 * @run main/othervm -Dsun.net.useExclusiveBind=true Basic
 * @run main/othervm -Djava.net.preferIPv4Stack=true Basic
 * @run main/othervm -Dsun.net.useExclusiveBind
 *                   -Djava.net.preferIPv4Stack=true Basic
 * @run main/othervm -Dsun.net.useExclusiveBind=true
 *                   -Djava.net.preferIPv4Stack=true Basic
 */
import java.net.*;
import jdk.test.lib.net.IPSupport;

public class Basic {

    static int testCount = 0;
    static int failures = 0;

    void test(String msg) {
        testCount++;
        System.out.println("***************************************");
        System.out.println("Test " + testCount + ": " + msg);
    }

    void passed() {
        System.out.println("Test passed.");
    }

    void failed() {
        failures++;
        System.out.println("Test failed.");
    }

    void check(boolean pass) {
        if (pass) {
            passed();
        } else {
            failed();
        }
    }

    void SocketTests() throws Exception {
        Socket s1 = new Socket();

        test("Socket should be created with SO_REUSEADDR disabled");
        check(!s1.getReuseAddress());

        test("Socket.setReuseAddress(true)");
        s1.setReuseAddress(true);
        check(s1.getReuseAddress());

        test("Socket.setReuseAddress(false)");
        s1.setReuseAddress(false);
        check(!s1.getReuseAddress() );

        /* bind to any port */
        s1.bind( new InetSocketAddress(0) );

        test("Binding Socket to port already in use should throw " +
             "a BindException");
        Socket s2 = new Socket();
        try {
            s2.bind( new InetSocketAddress(s1.getLocalPort()) );
            failed();
        } catch (BindException e) {
            passed();
        }
        s2.close();

        s1.close();
    }

    void ServerSocketTests() throws Exception {
        ServerSocket s1 = new ServerSocket();

        test("ServerSocket.setReuseAddress(true)");
        s1.setReuseAddress(true);
        check(s1.getReuseAddress());

        test("Socket.setReuseAddress(false)");
        s1.setReuseAddress(false);
        check(!s1.getReuseAddress() );

        /* bind to any port */
        s1.bind( new InetSocketAddress(0) );

        test("Binding ServerSocket to port already in use should throw " +
                "a BindException");
        ServerSocket s2 = new ServerSocket();
        try {
            s2.bind( new InetSocketAddress(s1.getLocalPort()) );
            failed();
        } catch (BindException e) {
            passed();
        }
        s2.close();

        s1.close();
    }

    void DatagramSocketTests() throws Exception {
        DatagramSocket s1 = new DatagramSocket(null);

        test("DatagramSocket should be created with SO_REUSEADDR disabled");
        check(!s1.getReuseAddress());

        test("DatagramSocket.setReuseAddress(true)");
        s1.setReuseAddress(true);
        check(s1.getReuseAddress());

        test("DatagramSocket.setReuseAddress(false)");
        s1.setReuseAddress(false);
        check(!s1.getReuseAddress() );

        /* bind to any port */
        s1.bind( new InetSocketAddress(0) );

        test("Binding datagram socket to port already in use should throw " +
             "a BindException");
        DatagramSocket s2 = new DatagramSocket(null);
        try {
            s2.bind( new InetSocketAddress(s1.getLocalPort()) );
            failed();
        } catch (BindException e) {
            passed();
        }
        s2.close();
        s1.close();

        // bind with SO_REUSEADDR enabled

        s1 = new DatagramSocket(null);
        s1.setReuseAddress(true);
        s1.bind( new InetSocketAddress(0) );

        test("Bind 2 datagram sockets to the same port - second " +
             "bind doesn't have SO_REUSEADDR enabled");
        s2 = new DatagramSocket(null);
        try {
            s2.bind( new InetSocketAddress(s1.getLocalPort()) );
            failed();
        } catch (BindException e) {
            passed();
        }
        s2.close();

        test("Bind 2 datagram sockets to the same port - both have " +
             "SO_REUSEADDR enabled");
        s2 = new DatagramSocket(null);
        s2.setReuseAddress(true);
        try {
            s2.bind( new InetSocketAddress(s1.getLocalPort()) );
            passed();
        } catch (BindException e) {
            if (System.getProperty("sun.net.useExclusiveBind") != null) {
                // exclusive bind enabled - expected result
                passed();
            } else {
                failed();
            }
        }
        s2.close();

        s1.close();

    }

    void MulticastSocketTests() throws Exception {
        test("Check SO_REUSEADDR is enabled in MulticastSocket()");
        MulticastSocket s1 = new MulticastSocket();
        check(s1.getReuseAddress());
        s1.close();

        test("Check that SO_REUSEADDR is not disabled by " +
             "MulticastSocket.bind()");

        s1 = new MulticastSocket(null);

        // bind to specific address
        InetSocketAddress isa = new InetSocketAddress(
                                   InetAddress.getLocalHost(), 0);
        s1.bind(isa);
        check(s1.getReuseAddress());
        s1.close();
    }

    Basic() throws Exception {

        SocketTests();
        ServerSocketTests();
        DatagramSocketTests();
        MulticastSocketTests();

        System.out.println("***************************************");
        System.out.println(testCount + " test(s) executed, " +
                           failures + " failure(s).");
        if (failures > 0) {
            throw new Exception(failures + " test(s) failed");
        }
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();
        new Basic();
    }

}
