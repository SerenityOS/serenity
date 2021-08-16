/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test SanityTest
 * @summary Sanity check of String.intern() & GC
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI SanityTest
 */

import java.util.*;
import sun.hotspot.WhiteBox;


public class SanityTest {
    public static Object tmp;
    public static void main(String... args) {

        WhiteBox wb = WhiteBox.getWhiteBox();
        StringBuilder sb = new StringBuilder();
        sb.append("1234x"); sb.append("x56789");
        String str = sb.toString();

        if (wb.isInStringTable(str)) {
            throw new RuntimeException("String " + str + " is already interned");
        }
        str.intern();
        if (!wb.isInStringTable(str)) {
            throw new RuntimeException("String " + str + " is not interned");
        }
        str = sb.toString();
        wb.fullGC();
        if (wb.isInStringTable(str)) {
            throw new RuntimeException("String " + str + " is in StringTable even after GC");
        }
    }
}
