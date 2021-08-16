/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Tests that the channel returned by System.inheritedChannel()
 * is in blocking mode, bound, and in the case of a SocketChannel
 * connected to a peer.
 *
 * The test works by launching a test service (called StateTestService) so
 * that it inherits each type of channel. The test service checks the
 * socket state and replies back to this class via an out-of-band
 * channel.
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import jdk.test.lib.Utils;

public class StateTest {

    private static int failures = 0;

    private static String TEST_SERVICE = "StateTestService";

    /*
     * Reads the test result from the "out-of-band" connection to the test service.
     *
     * The out-of-band connection is just a TCP connection from the service to
     * this class. waitForTestResult just waits (with timeout) for the service
     * to connect. Once connected it waits (with timeout) for the test result.
     * The test result is examined.
     */
    private static void waitForTestResult(ServerSocketChannel ssc, boolean expectFail) throws IOException {
        Selector sel = ssc.provider().openSelector();
        SelectionKey sk;
        SocketChannel sc;

        /*
         * Wait for service to connect
         */
        System.err.println("Waiting for the service to connect");
        ssc.configureBlocking(false);
        sk = ssc.register(sel, SelectionKey.OP_ACCEPT);
        long to = Utils.adjustTimeout(15*1000);
        sc = null;
        for (;;) {
            long st = System.currentTimeMillis();
            sel.select(to);
            if (sk.isAcceptable() && ((sc = ssc.accept()) != null)) {
                // connection established
                break;
            }
            sel.selectedKeys().remove(sk);
            to -= System.currentTimeMillis() - st;
            if (to <= 0) {
                throw new IOException("Timed out waiting for service to report test result");
            }
        }
        sk.cancel();
        ssc.configureBlocking(false);

        /*
         * Wait for service to report test result
         */
        System.err.println("Waiting for the service to report test result");
        sc.configureBlocking(false);
        sk = sc.register(sel, SelectionKey.OP_READ);
        to = Utils.adjustTimeout(5000);
        ByteBuffer bb = ByteBuffer.allocateDirect(20);
        for (;;) {
            long st = System.currentTimeMillis();
            sel.select(to);
            if (sk.isReadable()) {
                int n = sc.read(bb);
                if (n > 0) {
                    break;
                }
                if (n < 0) {
                    throw new IOException("Premature EOF - no test result from service");
                }
            }
            sel.selectedKeys().remove(sk);
            to -= System.currentTimeMillis() - st;
            if (to <= 0) {
                throw new IOException("Timed out waiting for service to report test result");
            }
        }
        System.err.println("Cleaning up");
        sk.cancel();
        sc.close();
        sel.close();

        /*
         * Examine the test result
         */
        System.err.println("Examine test result");
        bb.flip();
        byte b = bb.get();

        if (expectFail && b == 'P') {
            System.err.println("Test passed - test is expected to fail!!!");
            failures++;
        }
        if (!expectFail && b != 'P') {
            System.err.println("Test failed!");
            failures++;
        }
    }

    public static void main(String args[]) throws IOException {
        boolean expectFail = false;

        /*
         *   [-expectFail] [options...]
         */
        String options[] = args;
        if (args.length > 0 && args[0].equals("-expectFail")) {
            // shift out first arg to create options
            expectFail = true;
            options = new String[args.length-1];
            if (args.length > 1) {
                System.arraycopy(args, 1, options, 0, args.length-1);
            }
        }

        /*
         * Create the listener which will be used to read the test result
         * from the service.
         */
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        System.err.println("Listener bound to: " + ssc.socket().getLocalSocketAddress());

        /*
         * The port is passed to the service as an argument.
         */
        int port = ssc.socket().getLocalPort();
        String arg[] = new String[1];
        arg[0] = String.valueOf(port);

        /*
         * Launch service with a SocketChannel (tcp nowait)
         */
        System.err.println("launchWithInetSocketChannel");
        SocketChannel sc = Launcher.launchWithInetSocketChannel(TEST_SERVICE, options, arg);
        System.err.println("Waiting for test results");
        waitForTestResult(ssc, expectFail);
        sc.close();

        /*
         * Launch service with a ServerSocketChannel (tcp wait)
         * launchWithServerSocketChannel establishes a connection to the service
         * and the returned SocketChannel is connected to the service.
         */
        System.err.println("launchWithInetServerSocketChannel");
        sc = Launcher.launchWithInetServerSocketChannel(TEST_SERVICE, options, arg);
        waitForTestResult(ssc, expectFail);
        sc.close();

        /*
         * Launch service with a DatagramChannel (udp wait)
         */
        System.err.println("launchWithDatagramChannel");
        DatagramChannel dc = Launcher.launchWithDatagramChannel(TEST_SERVICE, options, arg);
        waitForTestResult(ssc, expectFail);
        dc.close();

        System.err.println("done");
        if (failures > 0) {
            throw new RuntimeException("Test failed - see log for details");
        } else {
            System.out.println("All tests passed.");
        }
    }
}
