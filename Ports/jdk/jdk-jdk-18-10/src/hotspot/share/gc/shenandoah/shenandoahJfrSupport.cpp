/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahJfrSupport.hpp"
#include "jfr/jfrEvents.hpp"
#if INCLUDE_JFR
#include "jfr/metadata/jfrSerializer.hpp"
#endif

#if INCLUDE_JFR

class ShenandoahHeapRegionStateConstant : public JfrSerializer {
  friend class ShenandoahHeapRegion;
public:
  virtual void serialize(JfrCheckpointWriter& writer) {
    static const u4 nof_entries = ShenandoahHeapRegion::region_states_num();
    writer.write_count(nof_entries);
    for (u4 i = 0; i < nof_entries; ++i) {
      writer.write_key(i);
      writer.write(ShenandoahHeapRegion::region_state_to_string((ShenandoahHeapRegion::RegionState)i));
    }
  }
};

void ShenandoahJFRSupport::register_jfr_type_serializers() {
  JfrSerializer::register_serializer(TYPE_SHENANDOAHHEAPREGIONSTATE,
                                     true,
                                     new ShenandoahHeapRegionStateConstant());
}
#endif

class ShenandoahDumpHeapRegionInfoClosure : public ShenandoahHeapRegionClosure {
public:
  virtual void heap_region_do(ShenandoahHeapRegion* r) {
    EventShenandoahHeapRegionInformation evt;
    evt.set_index((unsigned) r->index());
    evt.set_state((u8)r->state());
    evt.set_start((uintptr_t)r->bottom());
    evt.set_used(r->used());
    evt.commit();
  }
};

void VM_ShenandoahSendHeapRegionInfoEvents::doit() {
  ShenandoahDumpHeapRegionInfoClosure c;
  ShenandoahHeap::heap()->heap_region_iterate(&c);
}
