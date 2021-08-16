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
 * @test TestRegionAlignment.java
 * @bug 8013791
 * @requires vm.gc.G1
 * @summary Make sure that G1 ergonomics pick a heap size that is aligned with the region size
 * @run main/othervm -XX:+UseG1GC -XX:G1HeapRegionSize=32m -XX:MaxRAM=555m gc.g1.TestRegionAlignment
 *
 * When G1 ergonomically picks a maximum heap size it must be aligned to the region size.
 * This test tries to get the VM to pick a small and unaligned heap size (by using MaxRAM=555) and a
 * large region size (by using -XX:G1HeapRegionSize=32m). This will fail without the fix for 8013791.
 */
public class TestRegionAlignment {
    public static void main(String[] args) { }
}
