/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "code/nmethod.hpp"
#include "gc/g1/g1CodeBlobClosure.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentMark.inline.hpp"
#include "gc/g1/heapRegion.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"

template <typename T>
void G1CodeBlobClosure::HeapRegionGatheringOopClosure::do_oop_work(T* p) {
  _work->do_oop(p);
  T oop_or_narrowoop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(oop_or_narrowoop)) {
    oop o = CompressedOops::decode_not_null(oop_or_narrowoop);
    HeapRegion* hr = _g1h->heap_region_containing(o);
    assert(!_g1h->is_in_cset(o) || hr->rem_set()->strong_code_roots_list_contains(_nm), "if o still in collection set then evacuation failed and nm must already be in the remset");
    hr->add_strong_code_root(_nm);
  }
}

void G1CodeBlobClosure::HeapRegionGatheringOopClosure::do_oop(oop* o) {
  do_oop_work(o);
}

void G1CodeBlobClosure::HeapRegionGatheringOopClosure::do_oop(narrowOop* o) {
  do_oop_work(o);
}

template<typename T>
void G1CodeBlobClosure::MarkingOopClosure::do_oop_work(T* p) {
  T oop_or_narrowoop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(oop_or_narrowoop)) {
    oop o = CompressedOops::decode_not_null(oop_or_narrowoop);
    _cm->mark_in_next_bitmap(_worker_id, o);
  }
}

G1CodeBlobClosure::MarkingOopClosure::MarkingOopClosure(uint worker_id) :
  _cm(G1CollectedHeap::heap()->concurrent_mark()), _worker_id(worker_id) { }

void G1CodeBlobClosure::MarkingOopClosure::do_oop(oop* o) {
  do_oop_work(o);
}

void G1CodeBlobClosure::MarkingOopClosure::do_oop(narrowOop* o) {
  do_oop_work(o);
}

void G1CodeBlobClosure::do_evacuation_and_fixup(nmethod* nm) {
  _oc.set_nm(nm);
  nm->oops_do(&_oc);
  nm->fix_oop_relocations();
}

void G1CodeBlobClosure::do_marking(nmethod* nm) {
  nm->oops_do(&_marking_oc);
}

class G1NmethodProcessor : public nmethod::OopsDoProcessor {
  G1CodeBlobClosure* _cl;

public:
  G1NmethodProcessor(G1CodeBlobClosure* cl) : _cl(cl) { }

  void do_regular_processing(nmethod* nm) {
    _cl->do_evacuation_and_fixup(nm);
  }

  void do_remaining_strong_processing(nmethod* nm) {
    _cl->do_marking(nm);
  }
};

void G1CodeBlobClosure::do_code_blob(CodeBlob* cb) {
  nmethod* nm = cb->as_nmethod_or_null();
  if (nm == NULL) {
    return;
  }

  G1NmethodProcessor cl(this);

  if (_strong) {
    nm->oops_do_process_strong(&cl);
  } else {
    nm->oops_do_process_weak(&cl);
  }
}
