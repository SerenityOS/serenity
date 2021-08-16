/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @bug 8170920
 * @run main MinimumRcvBufferSize
 * @run main/othervm -Djava.net.preferIPv4Stack=true MinimumRcvBufferSize
 */

import java.nio.channels.*;
import java.net.*;
import jdk.test.lib.net.IPSupport;

public class MinimumRcvBufferSize {

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        boolean error = false;
        ServerSocketChannel channel = ServerSocketChannel.open();
        int before = channel.getOption(StandardSocketOptions.SO_RCVBUF);
        channel.setOption(StandardSocketOptions.SO_RCVBUF, Integer.MAX_VALUE);
        int after = channel.getOption(StandardSocketOptions.SO_RCVBUF);
        if (before > after) {
            System.err.println("Test failed: SO_RCVBUF");
            error = true;
        }

        SocketChannel channel1 = SocketChannel.open();
        before = channel1.getOption(StandardSocketOptions.SO_SNDBUF);
        channel1.setOption(StandardSocketOptions.SO_SNDBUF, Integer.MAX_VALUE);
        after = channel1.getOption(StandardSocketOptions.SO_SNDBUF);
        if (before > after) {
            System.err.println("Test failed: SO_SNDBUF");
            error = true;
        }
        if (error)
            throw new RuntimeException("Test failed");
    }
}
