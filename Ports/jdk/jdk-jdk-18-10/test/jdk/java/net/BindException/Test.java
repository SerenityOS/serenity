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
 * @bug 4417734
 * @key intermittent
 * @summary Test that we get a BindException in all expected combinations
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main Test -d
 */

import java.net.*;
import java.util.Enumeration;
import jdk.test.lib.NetworkConfiguration;

public class Test {

    static Object[][] getTestCombinations() {
        return new Object[][]  {
            { "ServerSocket",   "Socket" },
            { "Socket",         "Socket" },
            { "DatagramSocket", "DatagramSocket" },
        };
    }

    static InetAddress ia4_this;
    static InetAddress ia6_this;

    static int count;
    static int failures;
    static boolean retried;

    static void doTest(Object test[], InetAddress ia1, InetAddress ia2,
                       boolean silent) throws Exception {
        /*
         * Increment test count
         */
        count++;

        doTest(test, count, ia1, ia2, silent, !retried);
    }

    static void doTest(Object test[], int count, InetAddress ia1, InetAddress ia2,
                       boolean silent, boolean retry) throws Exception {
        String s1_type = (String)test[0];
        String s2_type = (String)test[1];
        int port = 0;

        /*
         * Do the test
         */

        boolean gotBindException = false;
        boolean failed = false;
        Exception failed_exc = null;

        Socket sock1 = null;
        ServerSocket ss = null;
        DatagramSocket dsock1 = null;
        boolean firstBound = false;

        try {
            /* bind the first socket */

            if (s1_type.equals("Socket")) {
                sock1 = new Socket();
                sock1.bind( new InetSocketAddress(ia1, 0));
                port = sock1.getLocalPort();
            }

            if (s1_type.equals("ServerSocket")) {
                ss = new ServerSocket(0, 0, ia1);
                port = ss.getLocalPort();
            }

            if (s1_type.equals("DatagramSocket")) {
                dsock1 = new DatagramSocket( new InetSocketAddress(ia1, 0) );
                port = dsock1.getLocalPort();
            }

            /* bind the second socket */

            // The fact that the port was available for ia1 does not
            // guarantee that it will also be available for ia2 as something
            // else might already be bound to that port.
            // For the sake of test stability we will retry once in
            // case of unexpected bind exception.

            firstBound = true;
            if (s2_type.equals("Socket")) {
                try (Socket sock2 = new Socket()) {
                    sock2.bind( new InetSocketAddress(ia2, port));
                }
            }

            if (s2_type.equals("ServerSocket")) {
                try (ServerSocket ss2 = new ServerSocket(port, 0, ia2)) { }
            }

            if (s2_type.equals("DatagramSocket")) {
                try (DatagramSocket ds =
                        new DatagramSocket(new InetSocketAddress(ia2, port))) { }
            }

        } catch (BindException be) {
            gotBindException = true;
            failed_exc = be;
        } catch (Exception e) {
            failed = true;
            failed_exc = e;
        } finally {
            if (sock1 != null) sock1.close();
            if (ss != null) ss.close();
            if (dsock1 != null) dsock1.close();
        }

        /*
         * Did we expect a BindException?
         */
        boolean expectedBindException = true;
        if (ia1 == ia4_this && ia2 == ia6_this) {
            expectedBindException = false;
        }
        if (ia1 == ia6_this && ia2 == ia4_this) {
            expectedBindException = false;
        }

        /*
         * Did it fail?
         */

        if (!failed && gotBindException != expectedBindException) {
            failed = true;
        }

        /*
         * If test passed and running in silent mode then exit
         */
        if (!failed && silent) {
            return;
        }

        if (failed && retry && firstBound) {
            // retry once at the first failure only
            retried = true;
            if (!silent) {
                System.out.println("");
                System.out.println("**************************");
                System.out.println("Test " + count + ": Retrying...");
            }
            doTest(test, count, ia1, ia2, silent, false);
            return;
        }

        if (failed || !silent) {
            System.out.println("");
            System.out.println("**************************");
            System.out.println("Test " + count);

            System.out.println(s1_type + " binds: " + ia1 + " port: " + port);
            System.out.println(s2_type + " binds: " + ia2);

            if (!failed) {
                if (gotBindException) {
                    System.out.println("Got expected BindException - test passed!");
                    failed_exc.printStackTrace(System.out);
                } else {
                    System.out.println("No BindException as expected - test passed!");
                }
                return;
            }
        }
        if (gotBindException) {
            System.out.println("BindException unexpected - test failed!!!");
            failed_exc.printStackTrace(System.out);
        } else {
            System.out.println("No bind failure as expected - test failed!!!");
        }
        failures++;
    }

    public static void main(String args[]) throws Exception {

        boolean silent = true;
        if (args.length > 0) {
            if (args[0].equals("-d")) {
                silent = false;
            }
        }

        /*
         * Test needs an IPv4 and IPv6 address to run.
         */
        Enumeration nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface ni = (NetworkInterface)nifs.nextElement();

            Enumeration addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress ia = (InetAddress)addrs.nextElement();

                if (ia.isLoopbackAddress() || ia.isAnyLocalAddress()) {
                    continue;
                }

                if ((ia instanceof Inet4Address) && (ia4_this == null)) {
                    ia4_this = ia;
                }

                if ((ia instanceof Inet6Address) && (ia6_this == null)) {
                    ia6_this = ia;
                }
            }
        }

        /*
         * Perform tests on all combinations of IPv4 and IPv6
         * addresses.
         */
        InetAddress addrs[] = { ia4_this, ia6_this };

        if (!silent) {
            System.out.println("Using ia4_this:" + ia4_this);
            System.out.println("Using ia6_this:" + ia6_this);
        }

        Object tests[][] = getTestCombinations();

        for (int i=0; i<tests.length; i++) {
            Object test[] = tests[i];

            for (int j=0; j<addrs.length; j++) {
                for (int k=0; k<addrs.length; k++) {

                    if (addrs[j] == null || addrs[k] == null) {
                        continue;
                    }

                    doTest( test, addrs[j], addrs[k], silent);
                }
            }
        }

        System.out.println("");
        System.out.println(count + " test(s) executed. " + failures + " failure(s).");

        if (failures > 0) {
            System.err.println("********************************");
            NetworkConfiguration.printSystemConfiguration(System.err);
            System.out.println("********************************");
            throw new Exception(failures + " tests(s) failed - see log");
        }
    }
}
