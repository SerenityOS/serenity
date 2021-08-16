/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code;

/**
 * Constants and intrinsic definition for memory barriers.
 *
 * The documentation for each constant is taken from Doug Lea's
 * <a href="http://gee.cs.oswego.edu/dl/jmm/cookbook.html">The JSR-133 Cookbook for Compiler
 * Writers</a>.
 * <p>
 * The {@code JMM_*} constants capture the memory barriers necessary to implement the Java Memory
 * Model with respect to volatile field accesses. Their values are explained by this comment from
 * templateTable_i486.cpp in the HotSpot source code:
 *
 * <pre>
 * Volatile variables demand their effects be made known to all CPU's in
 * order.  Store buffers on most chips allow reads &amp; writes to reorder; the
 * JMM's ReadAfterWrite.java test fails in -Xint mode without some kind of
 * memory barrier (i.e., it's not sufficient that the interpreter does not
 * reorder volatile references, the hardware also must not reorder them).
 *
 * According to the new Java Memory Model (JMM):
 * (1) All volatiles are serialized wrt to each other.
 * ALSO reads &amp; writes act as acquire &amp; release, so:
 * (2) A read cannot let unrelated NON-volatile memory refs that happen after
 * the read float up to before the read.  It's OK for non-volatile memory refs
 * that happen before the volatile read to float down below it.
 * (3) Similarly, a volatile write cannot let unrelated NON-volatile memory refs
 * that happen BEFORE the write float down to after the write.  It's OK for
 * non-volatile memory refs that happen after the volatile write to float up
 * before it.
 *
 * We only put in barriers around volatile refs (they are expensive), not
 * _between_ memory refs (which would require us to track the flavor of the
 * previous memory refs).  Requirements (2) and (3) require some barriers
 * before volatile stores and after volatile loads.  These nearly cover
 * requirement (1) but miss the volatile-store-volatile-load case.  This final
 * case is placed after volatile-stores although it could just as well go
 * before volatile-loads.
 * </pre>
 */
public class MemoryBarriers {

    /**
     * The sequence {@code Load1; LoadLoad; Load2} ensures that {@code Load1}'s data are loaded
     * before data accessed by {@code Load2} and all subsequent load instructions are loaded. In
     * general, explicit {@code LoadLoad} barriers are needed on processors that perform speculative
     * loads and/or out-of-order processing in which waiting load instructions can bypass waiting
     * stores. On processors that guarantee to always preserve load ordering, these barriers amount
     * to no-ops.
     */
    public static final int LOAD_LOAD = 0x0001;

    /**
     * The sequence {@code Load1; LoadStore; Store2} ensures that {@code Load1}'s data are loaded
     * before all data associated with {@code Store2} and subsequent store instructions are flushed.
     * {@code LoadStore} barriers are needed only on those out-of-order processors in which waiting
     * store instructions can bypass loads.
     */
    public static final int LOAD_STORE = 0x0002;

    /**
     * The sequence {@code Store1; StoreLoad; Load2} ensures that {@code Store1}'s data are made
     * visible to other processors (i.e., flushed to main memory) before data accessed by
     * {@code Load2} and all subsequent load instructions are loaded. {@code StoreLoad} barriers
     * protect against a subsequent load incorrectly using {@code Store1}'s data value rather than
     * that from a more recent store to the same location performed by a different processor.
     * Because of this, on the processors discussed below, a {@code StoreLoad} is strictly necessary
     * only for separating stores from subsequent loads of the same location(s) as were stored
     * before the barrier. {@code StoreLoad} barriers are needed on nearly all recent
     * multiprocessors, and are usually the most expensive kind. Part of the reason they are
     * expensive is that they must disable mechanisms that ordinarily bypass cache to satisfy loads
     * from write-buffers. This might be implemented by letting the buffer fully flush, among other
     * possible stalls.
     */
    public static final int STORE_LOAD = 0x0004;

    /**
     * The sequence {@code Store1; StoreStore; Store2} ensures that {@code Store1}'s data are
     * visible to other processors (i.e., flushed to memory) before the data associated with
     * {@code Store2} and all subsequent store instructions. In general, {@code StoreStore} barriers
     * are needed on processors that do not otherwise guarantee strict ordering of flushes from
     * write buffers and/or caches to other processors or main memory.
     */
    public static final int STORE_STORE = 0x0008;

    public static final int JMM_PRE_VOLATILE_WRITE = LOAD_STORE | STORE_STORE;
    public static final int JMM_POST_VOLATILE_WRITE = STORE_LOAD | STORE_STORE;
    public static final int JMM_PRE_VOLATILE_READ = 0;
    public static final int JMM_POST_VOLATILE_READ = LOAD_LOAD | LOAD_STORE;

    public static String barriersString(int barriers) {
        StringBuilder sb = new StringBuilder();
        sb.append((barriers & LOAD_LOAD) != 0 ? "LOAD_LOAD " : "");
        sb.append((barriers & LOAD_STORE) != 0 ? "LOAD_STORE " : "");
        sb.append((barriers & STORE_LOAD) != 0 ? "STORE_LOAD " : "");
        sb.append((barriers & STORE_STORE) != 0 ? "STORE_STORE " : "");
        return sb.toString().trim();
    }
}
