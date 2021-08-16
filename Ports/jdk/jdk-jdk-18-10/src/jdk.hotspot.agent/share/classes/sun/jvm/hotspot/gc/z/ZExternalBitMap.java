/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.BitMap;
import sun.jvm.hotspot.utilities.BitMapInterface;

/** Discontiguous bitmap for ZGC. */
public class ZExternalBitMap implements BitMapInterface {
    private ZPageTable pageTable;
    private final long oopSize;

    private HashMap<ZPage, BitMap> pageToBitMap = new HashMap<ZPage, BitMap>();

    public ZExternalBitMap(ZCollectedHeap collectedHeap) {
        pageTable = collectedHeap.heap().pageTable();
        oopSize = VM.getVM().getOopSize();
    }

    private ZPage getPage(long zOffset) {
        if (zOffset > ZGlobals.ZAddressOffsetMask()) {
            throw new RuntimeException("Not a Z offset: " + zOffset);
        }

        ZPage page = pageTable.get(ZUtils.longToAddress(zOffset));
        if (page == null) {
            throw new RuntimeException("Address not in pageTable: " + zOffset);
        }
        return page;
    }

    private BitMap getOrAddBitMap(ZPage page) {
        BitMap bitMap = pageToBitMap.get(page);
        if (bitMap == null) {
            long size = page.size();

            long maxNumObjects = size >>> page.object_alignment_shift();
            if (maxNumObjects > Integer.MAX_VALUE) {
                throw new RuntimeException("int overflow");
            }
            int intMaxNumObjects = (int)maxNumObjects;

            bitMap = new BitMap(intMaxNumObjects);
            pageToBitMap.put(page,  bitMap);
        }

        return bitMap;
    }

    private int pageLocalBitMapIndex(ZPage page, long zOffset) {
        long pageLocalZOffset = zOffset - page.start();
        return (int)(pageLocalZOffset >>> page.object_alignment_shift());
    }

    private long convertToZOffset(long offset) {
        long addr = oopSize * offset;
        return addr & ZGlobals.ZAddressOffsetMask();
    }

    @Override
    public boolean at(long offset) {
        long zOffset = convertToZOffset(offset);
        ZPage page = getPage(zOffset);
        BitMap bitMap = getOrAddBitMap(page);
        int index = pageLocalBitMapIndex(page, zOffset);

        return bitMap.at(index);
    }

    @Override
    public void atPut(long offset, boolean value) {
        long zOffset = convertToZOffset(offset);
        ZPage page = getPage(zOffset);
        BitMap bitMap = getOrAddBitMap(page);
        int index = pageLocalBitMapIndex(page, zOffset);

        bitMap.atPut(index, value);
    }

    @Override
    public void clear() {
        for (BitMap bitMap : pageToBitMap.values()) {
            bitMap.clear();
        }
    }
}
