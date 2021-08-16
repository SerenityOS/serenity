/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8065570
 * @summary Unit test for X-Buffer.order methods
 */

import java.nio.*;


public class Order {

    static final ByteOrder be = ByteOrder.BIG_ENDIAN;
    static final ByteOrder le = ByteOrder.LITTLE_ENDIAN;
    static final ByteOrder nord = ByteOrder.nativeOrder();

    protected static final int LENGTH = 16;

    static void ck(ByteOrder ord, ByteOrder expected) {
        if (ord != expected)
            throw new RuntimeException("Got " + ord + ", expected " + expected);
    }

    private static void ckViews(ByteBuffer bb) {
        ck(bb.asCharBuffer().order(), bb.order());
        ck(bb.asShortBuffer().order(), bb.order());
        ck(bb.asIntBuffer().order(), bb.order());
        ck(bb.asLongBuffer().order(), bb.order());
        ck(bb.asFloatBuffer().order(), bb.order());
        ck(bb.asDoubleBuffer().order(), bb.order());
    }

    private static void ckCopyViews(ByteBuffer bb) {
        ck(bb.asReadOnlyBuffer().order(), be);
        ck(bb.duplicate().order(), be);
        ck(bb.slice().order(), be);
    }

    private static void ckByteBuffer(ByteBuffer bb) {
        ckViews(bb);
        ckCopyViews(bb);
        bb.order(be);
        ckViews(bb);
        ckCopyViews(bb);
        bb.order(le);
        ckViews(bb);
        ckCopyViews(bb);
    }

    public static void main(String args[]) throws Exception {

        ck(ByteBuffer.wrap(new byte[LENGTH], LENGTH/2, LENGTH/2).order(), be);
        ck(ByteBuffer.wrap(new byte[LENGTH]).order(), be);
        ck(ByteBuffer.wrap(new byte[LENGTH], LENGTH/2, LENGTH/2).order(be).order(), be);
        ck(ByteBuffer.wrap(new byte[LENGTH]).order(be).order(), be);
        ck(ByteBuffer.wrap(new byte[LENGTH], LENGTH/2, LENGTH/2).order(le).order(), le);
        ck(ByteBuffer.wrap(new byte[LENGTH]).order(le).order(), le);
        ck(ByteBuffer.allocate(LENGTH).order(), be);
        ck(ByteBuffer.allocateDirect(LENGTH).order(), be);
        ck(ByteBuffer.allocate(LENGTH).order(be).order(), be);
        ck(ByteBuffer.allocate(LENGTH).order(le).order(), le);
        ck(ByteBuffer.allocateDirect(LENGTH).order(be).order(), be);
        ck(ByteBuffer.allocateDirect(LENGTH).order(le).order(), le);

        ckByteBuffer(ByteBuffer.allocate(LENGTH));
        ckByteBuffer(ByteBuffer.allocateDirect(LENGTH));

        OrderChar.ckCharBuffer();
        OrderShort.ckShortBuffer();
        OrderInt.ckIntBuffer();
        OrderLong.ckLongBuffer();
        OrderFloat.ckFloatBuffer();
        OrderDouble.ckDoubleBuffer();
    }
}
