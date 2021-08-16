/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.detailed;

/**
 * @test
 * @bug 8212766
 * @key jfr
 * @summary Test that events are created when an object is aged or promoted during a GC and the copying of the object requires a new PLAB or direct heap allocation
 * @requires vm.hasJFR
 *
 * @requires (vm.gc == "G1" | vm.gc == null)
 *           & vm.opt.ExplicitGCInvokesConcurrent != true
 * @library /test/lib /test/jdk
 * @run main/othervm -Xmx32m -Xms32m -Xmn12m -XX:+UseG1GC -XX:-UseStringDeduplication -XX:MaxTenuringThreshold=5 -XX:InitialTenuringThreshold=5 jdk.jfr.event.gc.detailed.TestPromotionEventWithG1
 * @run main/othervm -Xmx32m -Xms32m -Xmn12m -XX:AllocatePrefetchLines=1 -XX:AllocateInstancePrefetchLines=1 -XX:AllocatePrefetchStepSize=16 -XX:AllocatePrefetchDistance=1 -XX:+UseG1GC
  *                  -XX:-UseStringDeduplication -Xlog:os+cpu=info -XX:MaxTenuringThreshold=5 -XX:InitialTenuringThreshold=5 -XX:MinTLABSize=768 -XX:TLABSize=768 jdk.jfr.event.gc.detailed.TestPromotionEventWithG1
 */
public class TestPromotionEventWithG1 {

    public static void main(String[] args) throws Throwable {
        PromotionEvent.test();
    }
}
