/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/heapRegion.hpp"
#include "g1HeapRegionEventSender.hpp"
#include "jfr/jfrEvents.hpp"
#include "runtime/vmThread.hpp"

class DumpEventInfoClosure : public HeapRegionClosure {
public:
  bool do_heap_region(HeapRegion* r) {
    EventG1HeapRegionInformation evt;
    evt.set_index(r->hrm_index());
    evt.set_type(r->get_trace_type());
    evt.set_start((uintptr_t)r->bottom());
    evt.set_used(r->used());
    evt.commit();
    return false;
  }
};

class VM_G1SendHeapRegionInfoEvents : public VM_Operation {
  virtual void doit() {
    DumpEventInfoClosure c;
    G1CollectedHeap::heap()->heap_region_iterate(&c);
  }
  virtual VMOp_Type type() const { return VMOp_HeapIterateOperation; }
};

void G1HeapRegionEventSender::send_events() {
  if (UseG1GC) {
    VM_G1SendHeapRegionInfoEvents op;
    VMThread::execute(&op);
  }
}
