/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4777504 8024883
 * @summary Ensure that a Selector can return at least 100 selected keys
 * @author Mark Reinhold
 * @library ..
 * @build SelectorLimit
 * @run main/othervm SelectorLimit
 * @run main/othervm -Dsun.nio.ch.maxUpdateArraySize=128 SelectorLimit
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class SelectorLimit {

    static PrintStream log = System.err;

    static class Listener
        extends TestThread
    {
        volatile int count = 0;
        private ServerSocketChannel ssc;

        Listener(ServerSocketChannel ssc) {
            super("Listener");
            this.ssc = ssc;
        }

        void go() throws IOException {
            for (;;) {
                ssc.accept();
                count++;
            }
        }

    }

    static final int N_KEYS = 500;
    static final int MIN_KEYS = 100;

    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        TestUtil.bind(ssc);
        Listener lth = new Listener(ssc);
        lth.start();

        Selector sel = Selector.open();
        SocketChannel[] sca = new SocketChannel[N_KEYS];
        for (int i = 0; i < N_KEYS; i++) {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.register(sel, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
            sc.connect(ssc.socket().getLocalSocketAddress());
        }

        for (int i = 0; i < 10; i++) {
            if (lth.count >= MIN_KEYS)
                break;
            Thread.sleep(1000);
        }
        log.println(lth.count + " connections accepted");
        Thread.sleep(500);

        int n = sel.select();
        log.println(n + " keys selected");
        if (n < MIN_KEYS)
            throw new Exception("Only selected " + n + " keys");

    }

}
