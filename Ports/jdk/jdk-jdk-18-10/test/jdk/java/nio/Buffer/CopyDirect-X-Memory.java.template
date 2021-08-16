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

#warn This file is preprocessed before being compiled

import java.nio.*;

public class CopyDirect$Type$Memory
    extends CopyDirectMemory
{
    private static void init($Type$Buffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put(i, ($type$)ic(i));
        b.limit(n);
        b.position(0);
    }

    private static void init($type$ [] a) {
        for (int i = 0; i < a.length; i++)
            a[i] = ($type$)ic(i + 1);
    }

    public static void test() {
#if[byte]
        ByteBuffer b = ByteBuffer.allocateDirect(1024 * 1024 + 1024);
#else[byte]
        ByteBuffer bb = ByteBuffer.allocateDirect(1024 * 1024 + 1024);
        $Type$Buffer b = bb.as$Type$Buffer();
#end[byte]
        init(b);
        $type$ [] a = new $type$[b.capacity()];
        init(a);

        // copyFrom$Type$Array (a -> b)
        b.put(a);
        for (int i = 0; i < a.length; i++)
            ck(b, b.get(i), ($type$)ic(i + 1));

        // copyTo$Type$Array (b -> a)
        init(b);
        init(a);
        b.get(a);
        for (int i = 0; i < a.length; i++)
            if (a[i] != b.get(i))
                fail("Copy failed at " + i + ": '"
                     + a[i] + "' != '" + b.get(i) + "'");
    }
}
