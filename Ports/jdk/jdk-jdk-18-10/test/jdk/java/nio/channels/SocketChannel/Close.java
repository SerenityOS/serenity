/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4458266
 */

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;


public class Close {

    static SelectionKey open() throws IOException {
        SocketChannel sc = SocketChannel.open();
        Selector sel = Selector.open();
        sc.configureBlocking(false);
        return sc.register(sel, SelectionKey.OP_READ);
    }

    static void check(SelectionKey sk) throws IOException {
        if (sk.isValid())
            throw new RuntimeException("Key still valid");
        if (sk.channel().isOpen())
            throw new RuntimeException("Channel still open");
        //      if (!((SocketChannel)sk.channel()).socket().isClosed())
        //  throw new RuntimeException("Socket still open");
    }

    static void testSocketClose() throws IOException {
        SelectionKey sk = open();
        //((SocketChannel)sk.channel()).socket().close();
        check(sk);
    }

    static void testChannelClose() throws IOException {
        SelectionKey sk = open();
        try {
            sk.channel().close();
            check(sk);
        } finally {
            sk.selector().close();
        }
    }

    public static void main(String[] args) throws Exception {
        //##    testSocketClose();
        testChannelClose();
    }

}
