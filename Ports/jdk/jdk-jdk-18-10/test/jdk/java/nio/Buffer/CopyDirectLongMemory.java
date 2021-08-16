/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

import java.nio.*;

public class CopyDirectLongMemory
    extends CopyDirectMemory
{
    private static void init(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put(i, (long)ic(i));
        b.limit(n);
        b.position(0);
    }

    private static void init(long [] a) {
        for (int i = 0; i < a.length; i++)
            a[i] = (long)ic(i + 1);
    }

    public static void test() {



        ByteBuffer bb = ByteBuffer.allocateDirect(1024 * 1024 + 1024);
        LongBuffer b = bb.asLongBuffer();

        init(b);
        long [] a = new long[b.capacity()];
        init(a);

        // copyFromLongArray (a -> b)
        b.put(a);
        for (int i = 0; i < a.length; i++)
            ck(b, b.get(i), (long)ic(i + 1));

        // copyToLongArray (b -> a)
        init(b);
        init(a);
        b.get(a);
        for (int i = 0; i < a.length; i++)
            if (a[i] != b.get(i))
                fail("Copy failed at " + i + ": '"
                     + a[i] + "' != '" + b.get(i) + "'");
    }
}
