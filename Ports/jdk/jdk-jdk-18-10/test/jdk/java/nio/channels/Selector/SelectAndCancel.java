/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4729342
 * @library /test/lib
 * @build jdk.test.lib.Utils
 * @run main SelectAndCancel
 * @summary Check for CancelledKeyException when key cancelled during select
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class SelectAndCancel {
    static volatile SelectionKey sk;
    static volatile Throwable ex;

    /*
     * CancelledKeyException is the failure symptom of 4729342
     * NOTE: The failure is timing dependent and is not always
     * seen immediately when the bug is present.
     */
    public static void main(String[] args) throws Throwable {
        final Selector selector = Selector.open();
        final ServerSocketChannel ssc = ServerSocketChannel.open().bind(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        final InetSocketAddress isa = (InetSocketAddress)ssc.getLocalAddress();
        final CountDownLatch signal = new CountDownLatch(1);

        // Create and start a selector in a separate thread.
        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        ssc.configureBlocking(false);
                        sk = ssc.register(selector, SelectionKey.OP_ACCEPT);
                        signal.countDown();
                        selector.select();
                    } catch (Throwable e) {
                        ex = e;
                    }
                }
            });
        t.start();

        signal.await();
        // Wait for above thread to get to select() before we call cancel.
        Thread.sleep((long)(300 * jdk.test.lib.Utils.TIMEOUT_FACTOR));

        // CancelledKeyException should not be thrown.
        ssc.close();
        sk.cancel();
        selector.close();
        t.join();
        if (ex != null) {
            throw ex;
        }
    }
}
