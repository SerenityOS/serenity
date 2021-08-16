/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4744052
 * @summary Test for keys remaining in selector after channel closed
 */

import java.nio.*;
import java.nio.channels.*;

public class Close {

    public static void main(String args[]) throws Exception {
        Selector sa = Selector.open();
        Selector sb = Selector.open();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        SelectionKey sk = sc.register(sa, SelectionKey.OP_READ);
        sc.register(sb, SelectionKey.OP_READ);
        sc.keyFor(sa).cancel();
        sa.select(1);
        sc.close();
        sa.select(1);
        sb.select(1);
        if (sa.keys().size() > 0)
            throw new RuntimeException("Keys remain in selector a");
        if (sb.keys().size() > 0)
            throw new RuntimeException("Keys remain in selector b");
        sa.close();
        sb.close();
    }
}
