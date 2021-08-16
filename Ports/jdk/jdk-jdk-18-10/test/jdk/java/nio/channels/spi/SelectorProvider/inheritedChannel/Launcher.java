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
 * A Launcher to launch a java process with its standard input, output,
 * and error streams connected to a socket.
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;

import static java.net.StandardProtocolFamily.UNIX;

public class Launcher {

    static {
        System.loadLibrary("InheritedChannel");
    }

    private static native void launch0(String cmdarray[], int fd) throws IOException;

    private static void launch(String className, String options[], String args[], int fd) throws IOException {
        // java [-options] class [args...]
        int optsLen = (options == null) ? 0 : options.length;
        int argsLen = (args == null) ? 0 : args.length;
        int len = 1 + optsLen + 1 + argsLen;
        String cmdarray[] = new String[len];
        int pos = 0;
        cmdarray[pos++] = Util.javaCommand();
        if (options != null) {
            for (String opt: options) {
                cmdarray[pos++] = opt;
            }
        }
        cmdarray[pos++] = className;
        if (args != null) {
            for (String arg: args) {
                cmdarray[pos++] = arg;
            }
        }
        launch0(cmdarray, fd);
    }

    /**
     * Launch 'java' with specified class. The launched process will inherit
     * a connected Unix Domain socket. The remote endpoint will be the
     * SocketChannel returned by this method.
     */
    public static SocketChannel launchWithUnixSocketChannel(String className)
            throws IOException
    {
        UnixDomainSocketAddress addr = null;
        try (ServerSocketChannel ssc = ServerSocketChannel.open(UNIX)) {
            addr = (UnixDomainSocketAddress)ssc.bind(null).getLocalAddress();
            SocketChannel sc1 = SocketChannel.open(addr);
            try (SocketChannel sc2 = ssc.accept()) {
                launch(className, null, null, Util.getFD(sc2));
            }
            return sc1;
        } finally {
            if (addr != null)
                Files.delete(addr.getPath());
        }
    }

    /**
     * Launch 'java' with specified class with the specified arguments (may be null).
     * The launched process will inherit a connected TCP socket. The remote endpoint
     * will be the SocketChannel returned by this method.
     */
    public static SocketChannel launchWithInetSocketChannel(String className,
                                                        String options[],
                                                        String... args)
            throws IOException
    {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
            InetSocketAddress isa = new InetSocketAddress(InetAddress.getLocalHost(),
                                                      ssc.socket().getLocalPort());
            SocketChannel sc1 = SocketChannel.open(isa);
            try (SocketChannel sc2 = ssc.accept()) {
                launch(className, options, args, Util.getFD(sc2));
            }
            return sc1;
        }
    }

    /**
     * Launch specified class with a SocketChannel created externally.
     */
    public static void launchWithSocketChannel(String className,
                                               SocketChannel sc,
                                               String[] options,
                                               String... args) throws Exception {
        launch(className, options, args, Util.getFD(sc));
    }

    /**
     * Launch 'java' with specified class with the specified arguments (may be null).
     * The launched process will inherited a TCP listener socket.
     * Once launched this method tries to connect to service. If a connection
     * can be established a SocketChannel, connected to the service, is returned.
     */
    public static SocketChannel launchWithInetServerSocketChannel(String className,
                                                              String[] options,
                                                              String... args)
            throws IOException
    {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
            int port = ssc.socket().getLocalPort();
            launch(className, options, args, Util.getFD(ssc));
            InetSocketAddress isa = new InetSocketAddress(InetAddress.getLocalHost(), port);
            return SocketChannel.open(isa);
        }
    }

    public static SocketChannel launchWithUnixServerSocketChannel(String className) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        ssc.bind(null);
        var addr = ssc.getLocalAddress();
        launch(className, null, null, Util.getFD(ssc));
        ssc.close();
        return SocketChannel.open(addr);
    }

    /**
     * Launch specified class with a ServerSocketChannel created externally.
     */
    public static void launchWithServerSocketChannel(String className,
                                                     ServerSocketChannel ssc,
                                                     String[] options,
                                                     String... args)
            throws Exception {
        launch(className, options, args, Util.getFD(ssc));
    }

    /**
     * Launch 'java' with specified class with the specified arguments (may be null).
     * The launch process will inherited a bound UDP socket.
     * Once launched this method creates a DatagramChannel and "connects
     * it to the service. The created DatagramChannel is then returned.
     * As it is connected any packets sent from the socket will be
     * sent to the service.
     */
    public static DatagramChannel launchWithDatagramChannel(String className,
                                                            String[] options,
                                                            String... args)
            throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            address = InetAddress.getLoopbackAddress();
        }
        DatagramChannel dc = DatagramChannel.open();
        dc.socket().bind(new InetSocketAddress(address, 0));

        int port = dc.socket().getLocalPort();
        launch(className, options, args, Util.getFD(dc));
        dc.close();

        dc = DatagramChannel.open();
        InetSocketAddress isa = new InetSocketAddress(address, port);

        dc.connect(isa);
        return dc;
    }
}
