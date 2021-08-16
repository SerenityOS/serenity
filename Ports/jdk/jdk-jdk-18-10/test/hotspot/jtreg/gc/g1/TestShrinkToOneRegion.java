/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestShrinkToOneRegion.java
 * @bug 8013872
 * @requires vm.gc.G1
 * @summary Shrinking the heap down to one region used to hit an assert
 * @run main/othervm -XX:+UseG1GC -XX:G1HeapRegionSize=32m -Xmx256m gc.g1.TestShrinkToOneRegion
 *
 * Doing a System.gc() without having allocated many objects will shrink the heap.
 * With a large region size we will shrink the heap to one region.
 */
public class TestShrinkToOneRegion {
    public static void main(String[] args) {
        System.gc();
    }
}
