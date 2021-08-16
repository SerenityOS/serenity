/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestConcurrentSystemGC
 * @bug 8195158
 * @summary Test that a System.gc() with -XX:+ExplicitGCInvokesConcurrent
 *          is *not* upgraded to STW full GC
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent -Xmx8m -Xms8m -XX:G1HeapRegionSize=1m gc.g1.TestConcurrentSystemGC
 */

import java.util.ArrayList;
import java.util.List;

public class TestConcurrentSystemGC {
    public static List<char[]> memory;
    public static void main(String[] args) throws Exception {
        memory = new ArrayList<>();
        try {
            while (true) {
                memory.add(new char[1024 * 128]);
                System.gc(); // should not trigger any assertions
            }
        } catch (OutOfMemoryError e) {
            memory = null;
            System.gc();
        }
    }
}
