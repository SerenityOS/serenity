/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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

package sun.jvm.hotspot.gc.shenandoah;

import sun.jvm.hotspot.debugger.OopHandle;
import sun.jvm.hotspot.gc.shared.ContiguousSpace;
import sun.jvm.hotspot.gc.shared.LiveRegionsProvider;
import sun.jvm.hotspot.memory.MemRegion;
import sun.jvm.hotspot.oops.Mark;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.oops.UnknownOopException;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.utilities.AddressOps;

import java.util.ArrayList;
import java.util.List;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;


public class ShenandoahHeapRegion extends VMObject implements LiveRegionsProvider {
    private static int EmptyUncommitted;
    private static int EmptyCommitted;
    private static int Regular;
    private static int HumongousStart;
    private static int HumongousCont;
    private static int PinnedHumongousStart;
    private static int CSet;
    private static int Pinned;
    private static int PinnedCSet;
    private static int Trash;

    private static CIntegerField RegionSizeBytesField;
    private static Field         RegionStateField;
    private static CIntegerField RegionIndexField;
    private static CIntegerField RegionSizeBytesShiftField;

    private static AddressField BottomField;
    private static AddressField TopField;
    private static AddressField EndField;

    private ShenandoahHeap heap;

    static {
        VM.registerVMInitializedObserver(new Observer() {
            public void update(Observable o, Object data) {
                initialize(VM.getVM().getTypeDataBase());
            }
        });
    }

    static private synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("ShenandoahHeapRegion");
        RegionSizeBytesField = type.getCIntegerField("RegionSizeBytes");
        RegionStateField = type.getField("_state");
        RegionIndexField = type.getCIntegerField("_index");
        BottomField = type.getAddressField("_bottom");
        TopField = type.getAddressField("_top");
        EndField = type.getAddressField("_end");

        RegionSizeBytesShiftField = type.getCIntegerField("RegionSizeBytesShift");

        EmptyUncommitted     = db.lookupIntConstant("ShenandoahHeapRegion::_empty_uncommitted").intValue();
        EmptyCommitted       = db.lookupIntConstant("ShenandoahHeapRegion::_empty_committed").intValue();
        Regular              = db.lookupIntConstant("ShenandoahHeapRegion::_regular").intValue();
        HumongousStart       = db.lookupIntConstant("ShenandoahHeapRegion::_humongous_start").intValue();
        HumongousCont        = db.lookupIntConstant("ShenandoahHeapRegion::_humongous_cont").intValue();
        PinnedHumongousStart = db.lookupIntConstant("ShenandoahHeapRegion::_pinned_humongous_start").intValue();
        CSet                 = db.lookupIntConstant("ShenandoahHeapRegion::_cset").intValue();
        Pinned               = db.lookupIntConstant("ShenandoahHeapRegion::_pinned").intValue();
        PinnedCSet           = db.lookupIntConstant("ShenandoahHeapRegion::_pinned_cset").intValue();
        Trash                = db.lookupIntConstant("ShenandoahHeapRegion::_trash").intValue();
    }

    public static long regionSizeBytes() {
        return RegionSizeBytesField.getValue();
    }

    public static int  regionSizeBytesShift() {
        return RegionSizeBytesShiftField.getJInt();
    }

    public ShenandoahHeapRegion(Address addr) {
        super(addr);
    }

    public void setHeap(ShenandoahHeap heap) {
        this.heap = heap;
    }

    public Address bottom() {
        return BottomField.getValue(addr);
    }

    public Address top() {
        return TopField.getValue(addr);
    }

    public Address end() {
        return EndField.getValue(addr);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(index());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ShenandoahHeapRegion) {
            ShenandoahHeapRegion otherRegion = (ShenandoahHeapRegion)other;
            return otherRegion.index() == index();
        }
        return false;
    }

    public List<MemRegion> getLiveRegions() {
        List<MemRegion> res = new ArrayList<>();
        int state = regionState();
        if (state == EmptyUncommitted || state == EmptyCommitted || state == Trash) {
            // No live data
        } else if (state == HumongousCont) {
            // Handled by HumongousStart
        } else if (state == HumongousStart || state == PinnedHumongousStart) {
            handleHumongousRegion(res);
        } else if (state == Regular || state == Pinned) {
            handleRegularRegion(res);
        } else if (state == CSet || state == PinnedCSet) {
            // CSet
            handleCSetRegion(res);
        } else {
            throw new RuntimeException("Unknown region state: " + state);
        }
        return res;
    }

    /*
     * Note: RegionState is an enum on JVM side. Seems that there is not
     *       a standard way to read enum value. We read it as an integer
     *       from the field's offset.
     */
    private int regionState() {
        long offset = RegionStateField.getOffset();
        return addr.getJIntAt(offset);
    }

    private void handleHumongousRegion(List<MemRegion> res) {
        long index = index();
        Address topAddr = top();
        ShenandoahHeapRegion region = heap.getRegion(++ index);
        while (region.regionState() == HumongousCont) {
            topAddr = region.top();
            region = heap.getRegion(++ index);
        }
        res.add(new MemRegion(bottom(), topAddr));
    }

    private void handleRegularRegion(List<MemRegion> res) {
        res.add(new MemRegion(bottom(), top()));
    }

    // Filter out forwarded objects, they should be counted in other regions
    private void handleCSetRegion(List<MemRegion> res) {
        Address end = top();
        Address start = bottom();

        Address regionStart = null;
        Address regionEnd = null;
        while (AddressOps.lessThan(start, end)) {
            long size = getObjectSize(start);
            if (hasForwardee(start)) {
                // has to-space object, skip this one
                if (regionEnd != null) {
                    MemRegion mr = new MemRegion(regionStart, regionEnd);
                    res.add(mr);
                    regionStart = null;
                    regionEnd = null;
                }
            } else {
                if (regionStart == null) {
                    regionStart = start;
                } else {
                    regionEnd = start.addOffsetTo(size);
                }
            }
            start = start.addOffsetTo(size);
        }

        if (regionStart != null) {
            MemRegion mr = new MemRegion(regionStart, top());
            res.add(mr);
        }
    }

    public long index() {
        return RegionIndexField.getValue(addr);
    }

    private boolean hasForwardee(Address rawPtr) {
        // Forwarding pointer is stored in mark word when it is flagged "marked"
        Mark mark = new Mark(rawPtr);
        return mark.isMarked();
    }

    private long getObjectSize(Address rawPtr) {
        OopHandle handle = rawPtr.addOffsetToAsOopHandle(0);
        Oop obj = null;

        try {
            // Best effort, may fail
            obj = VM.getVM().getObjectHeap().newOop(handle);
        } catch (UnknownOopException exp) {
            throw new RuntimeException(" UnknownOopException  " + exp);
        }
        return obj.getObjectSize();
    }
}
