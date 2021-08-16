/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.gc.z;

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;

class ZBarrier {
    private static boolean is_weak_good_or_null_fast_path(Address addr) {
        return ZAddress.is_weak_good_or_null(addr);
    }

    private static Address weak_load_barrier_on_oop_slow_path(Address addr) {
        return ZAddress.is_weak_good(addr) ? ZAddress.good(addr) : relocate_or_remap(addr);
    }

    private static boolean during_relocate() {
        return ZGlobals.ZGlobalPhase() == ZGlobals.ZPhaseRelocate;
    }

    private static Address relocate(Address addr) {
        return zheap().relocate_object(addr);
    }

    private static ZHeap zheap() {
        ZCollectedHeap zCollectedHeap = (ZCollectedHeap)VM.getVM().getUniverse().heap();
        return zCollectedHeap.heap();
    }

    private static Address remap(Address addr) {
        return zheap().remapObject(addr);
    }

    private static Address relocate_or_remap(Address addr) {
        return during_relocate() ? relocate(addr) : remap(addr);
    }

    static Address weak_barrier(Address o) {
        // Fast path
        if (is_weak_good_or_null_fast_path(o)) {
            // Return the good address instead of the weak good address
            // to ensure that the currently active heap view is used.
            return ZAddress.good_or_null(o);
        }

        // Slow path
        return weak_load_barrier_on_oop_slow_path(o);
    }
}
