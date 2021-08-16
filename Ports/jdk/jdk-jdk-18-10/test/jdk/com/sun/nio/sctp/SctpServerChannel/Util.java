/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.SocketException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.nio.ByteBuffer;
import com.sun.nio.sctp.SctpChannel;
import static java.lang.System.out;

public class Util {
    static final int SMALL_BUFFER = 128;
    static final String SMALL_MESSAGE =
      "Under the bridge and over the dam, looking for berries, berries for jam";

    static final int LARGE_BUFFER = 32768;
    static final String LARGE_MESSAGE;

    static {
        StringBuffer sb = new StringBuffer(LARGE_BUFFER);
        for (int i=0; i<460; i++)
          sb.append(SMALL_MESSAGE);

        LARGE_MESSAGE = sb.toString();
    }

    static boolean isSCTPSupported() {
        try {
            SctpChannel c = SctpChannel.open();
            c.close();
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (UnsupportedOperationException e) {
            out.println(e);
        }

        return false;
    }
    /**
     * Returns a list of all the addresses on the system.
     * @param  inclLoopback
     *         if {@code true}, include the loopback addresses
     * @param  ipv4Only
     *         it {@code true}, only IPv4 addresses will be included
     */
    static List<InetAddress> getAddresses(boolean inclLoopback,
                                          boolean ipv4Only)
        throws SocketException {
        ArrayList<InetAddress> list = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> nets =
                 NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netInf : Collections.list(nets)) {
            Enumeration<InetAddress> addrs = netInf.getInetAddresses();
            for (InetAddress addr : Collections.list(addrs)) {
                if (!list.contains(addr) &&
                        (inclLoopback ? true : !addr.isLoopbackAddress()) &&
                        (ipv4Only ? (addr instanceof Inet4Address) : true)) {
                    list.add(addr);
                }
            }
        }

        return list;
    }

    static void dumpAddresses(SctpChannel channel,
                              PrintStream printStream)
        throws IOException {
        Set<SocketAddress> addrs = channel.getAllLocalAddresses();
        printStream.println("Local Addresses: ");
        for (Iterator<SocketAddress> it = addrs.iterator(); it.hasNext(); ) {
            InetSocketAddress addr = (InetSocketAddress)it.next();
            printStream.println("\t" + addr);
        }
    }

    /**
     * Compare the contents of the given ByteBuffer with the contens of the
     * given byte array. true if, and only if, the contents are the same.
     */
    static boolean compare(ByteBuffer bb, byte[] message) {
        if (message.length != bb.remaining()) {
            out.println("Compare failed, byte array length != to buffer remaining");
            return false;
        }

        for (int i=0; i<message.length; i++) {
            byte b = bb.get();
            if (message[i] != b) {
                out.println("Position " + i + ": " + message[i] + " != " + b);
                return false;
            }
        }

        return true;
    }

    static boolean compare(ByteBuffer bb, String str) {
        try{
            return Util.compare(bb, str.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException unsupported) {
            throw new AssertionError(unsupported);
        }
    }
}
