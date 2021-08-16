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
 * @summary Test trivial stuff
 */

import java.nio.channels.*;


public class Trivial {

    public static void main(String[] args) throws Exception {
        SocketChannel sc = SocketChannel.open();
        Selector sel = Selector.open();
        try {
            if (sc.keyFor(sel) != null)
                throw new Exception("keyFor != null");
            sc.configureBlocking(false);
            SelectionKey sk = sc.register(sel, SelectionKey.OP_READ, args);
            if (sc.keyFor(sel) != sk)
                throw new Exception("keyFor returned " + sc.keyFor(sel));
            if (sk.attachment() != args)
                throw new Exception("attachment() returned " + sk.attachment());
            Trivial t = new Trivial();
            sk.attach(t);
            if (sk.attachment() != t)
                throw new Exception("Wrong attachment");
            sk.isReadable();
            sk.isWritable();
            sk.isConnectable();
            sk.isAcceptable();
        } finally {
            sel.close();
            sc.close();
        }
    }

}
