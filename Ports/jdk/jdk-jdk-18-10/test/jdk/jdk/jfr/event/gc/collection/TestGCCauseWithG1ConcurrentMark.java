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

package jdk.jfr.event.gc.collection;
import jdk.test.lib.jfr.GCHelper;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 *
 * @requires vm.gc == "G1" | vm.gc == null
 * @requires vm.opt.ExplicitGCInvokesConcurrent != false
 * @library /test/lib /test/jdk
 *
 * @run driver jdk.jfr.event.gc.collection.TestGCCauseWithG1ConcurrentMark
 */
public class TestGCCauseWithG1ConcurrentMark {
    public static void main(String[] args) throws Exception {
        String testID = "G1ConcurrentMark";
        String[] vmFlags = {"-XX:+UseG1GC", "-XX:+ExplicitGCInvokesConcurrent"};
        String[] gcNames = {GCHelper.gcG1New, GCHelper.gcG1Old, GCHelper.gcG1Full};
        String[] gcCauses = {"G1 Evacuation Pause", "G1 Preventive Collection", "G1 Compaction Pause", "System.gc()"};
        GCGarbageCollectionUtil.test(testID, vmFlags, gcNames, gcCauses);
    }
}

