/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4953599
 * @run main/timeout=15 CheckLocking
 * @summary check if setInterest blocks while in select
 */

import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.net.*;

public class CheckLocking implements Runnable {

    private static Selector selector;

    public CheckLocking() {
    }

    public void run() {
        try {
            selector.select();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static void doSelect() throws Exception {
        Thread thread = new Thread(new CheckLocking());
        thread.start();
        Thread.sleep(1000);
    }

    public static void main (String[] args) throws Exception {
        selector = SelectorProvider.provider().openSelector();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        SelectionKey sk = sc.register(selector,0,null);
        doSelect();
        sk.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
        sc.close();
        selector.close();
    }
}
