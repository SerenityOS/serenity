/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4640544 8044773
 * @summary Unit test for ServerSocketChannel setOption/getOption/options
 *          methods.
 * @modules jdk.net
 * @requires !vm.graal.enabled
 * @run main SocketOptionTests
 * @run main/othervm --limit-modules=java.base SocketOptionTests
 */

import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;
import static java.net.StandardSocketOptions.*;
import static jdk.net.ExtendedSocketOptions.*;

public class SocketOptionTests {

    private static final int DEFAULT_KEEP_ALIVE_PROBES = 7;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 1973;
    private static final int DEFAULT_KEEP_ALIVE_INTVL = 53;

    static void checkOption(ServerSocketChannel ssc, SocketOption name, Object expectedValue)
        throws IOException
    {
        Object value = ssc.getOption(name);
        if (!value.equals(expectedValue))
            throw new RuntimeException("value not as expected");
    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        // check supported options
        Set<SocketOption<?>> options = ssc.supportedOptions();
        boolean reuseport = options.contains(SO_REUSEPORT);
        if (!options.contains(SO_REUSEADDR))
            throw new RuntimeException("SO_REUSEADDR should be supported");
        if (!options.contains(SO_REUSEPORT) && reuseport)
            throw new RuntimeException("SO_REUSEPORT should be supported");
        if (!options.contains(SO_RCVBUF))
            throw new RuntimeException("SO_RCVBUF should be supported");

        // allowed to change when not bound
        ssc.setOption(SO_RCVBUF, 256*1024);     // can't check
        int before = ssc.getOption(SO_RCVBUF);
        int after = ssc.setOption(SO_RCVBUF, Integer.MAX_VALUE).getOption(SO_RCVBUF);
        if (after < before)
            throw new RuntimeException("setOption caused SO_RCVBUF to decrease");
        ssc.setOption(SO_REUSEADDR, true);
        checkOption(ssc, SO_REUSEADDR, true);
        ssc.setOption(SO_REUSEADDR, false);
        checkOption(ssc, SO_REUSEADDR, false);
        if (reuseport) {
            ssc.setOption(SO_REUSEPORT, true);
            checkOption(ssc, SO_REUSEPORT, true);
            ssc.setOption(SO_REUSEPORT, false);
            checkOption(ssc, SO_REUSEPORT, false);
        }
        if (ssc.supportedOptions().containsAll(List.of(TCP_KEEPCOUNT,
                TCP_KEEPIDLE, TCP_KEEPINTERVAL))) {
            ssc.setOption(TCP_KEEPCOUNT, DEFAULT_KEEP_ALIVE_PROBES);
            checkOption(ssc, TCP_KEEPCOUNT, DEFAULT_KEEP_ALIVE_PROBES);
            ssc.setOption(TCP_KEEPIDLE, DEFAULT_KEEP_ALIVE_TIME);
            checkOption(ssc, TCP_KEEPIDLE, DEFAULT_KEEP_ALIVE_TIME);
            ssc.setOption(TCP_KEEPINTERVAL, DEFAULT_KEEP_ALIVE_INTVL);
            checkOption(ssc, TCP_KEEPINTERVAL, DEFAULT_KEEP_ALIVE_INTVL);

        }

        // NullPointerException
        try {
            ssc.setOption(null, "value");
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            ssc.getOption(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }

        // ClosedChannelException
        ssc.close();
        try {
            ssc.setOption(SO_REUSEADDR, true);
            throw new RuntimeException("ClosedChannelException not thrown");
        } catch (ClosedChannelException x) {
        }
    }
}
