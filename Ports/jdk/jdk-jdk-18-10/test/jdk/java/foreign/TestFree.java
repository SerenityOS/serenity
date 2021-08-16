/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @bug 8248421
 * @summary SystemCLinker should have a way to free memory allocated outside Java
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestFree
 */

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.assertEquals;

public class TestFree {
    private static MemorySegment asArray(MemoryAddress addr, MemoryLayout layout, int numElements) {
        return addr.asSegment(numElements * layout.byteSize(), ResourceScope.globalScope());
    }

    public void test() throws Throwable {
        String str = "hello world";
        MemoryAddress addr = allocateMemory(str.length() + 1);
        MemorySegment seg = asArray(addr, C_CHAR, str.length() + 1);
        seg.copyFrom(MemorySegment.ofArray(str.getBytes()));
        MemoryAccess.setByteAtOffset(seg, str.length(), (byte)0);
        assertEquals(str, toJavaString(seg));
        freeMemory(addr);
    }
}
