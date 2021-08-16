/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* Type-specific source code for unit test
 *
 * Regenerate the OrderX classes via genOrder.sh whenever this file changes.
 * We check in the generated source files so that the test tree can be used
 * independently of the rest of the source tree.
 */

// -- This file was mechanically generated: Do not edit! -- //

import java.nio.*;

public class OrderChar extends Order {
    private static void ckCharBuffer(CharBuffer buf, ByteOrder expected) {
        ck(buf.asReadOnlyBuffer().order(), expected);
        ck(buf.duplicate().order(), expected);
        ck(buf.slice().order(), expected);

        ck(buf.subSequence(buf.position(), buf.remaining()).order(), expected);
        ck(buf.subSequence(buf.position(), buf.position()).order(), expected);

    }

    static void ckCharBuffer() {
        char[] array = new char[LENGTH];
        CharBuffer buf = CharBuffer.wrap(array);
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = CharBuffer.wrap(array, LENGTH/2, LENGTH/2);
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = CharBuffer.allocate(LENGTH);
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = CharBuffer.wrap("abcdefghijk");
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = CharBuffer.wrap("abcdefghijk", 0, 5);
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = CharBuffer.wrap(array).subSequence(0, LENGTH);
        ck(buf.order(), nord);
        ckCharBuffer(buf, nord);

        buf = ByteBuffer.wrap(new byte[LENGTH]).asCharBuffer();
        ck(buf.order(), be);
        ckCharBuffer(buf, be);

        buf = ByteBuffer.wrap(new byte[LENGTH]).order(le).asCharBuffer();
        ck(buf.order(), le);
        ckCharBuffer(buf, le);

    }
}

