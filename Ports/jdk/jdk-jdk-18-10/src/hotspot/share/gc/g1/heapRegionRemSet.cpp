/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <cstdio>

#include "precompiled.hpp"
#include "gc/g1/g1BlockOffsetTable.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentRefine.hpp"
#include "gc/g1/heapRegionManager.inline.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "memory/allocation.hpp"
#include "memory/padded.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/powerOfTwo.hpp"

const char* HeapRegionRemSet::_state_strings[] =  {"Untracked", "Updating", "Complete"};
const char* HeapRegionRemSet::_short_state_strings[] =  {"UNTRA", "UPDAT", "CMPLT"};

HeapRegionRemSet::HeapRegionRemSet(HeapRegion* hr,
                                   G1CardSetConfiguration* config) :
  _m(Mutex::leaf + 1, FormatBuffer<128>("HeapRegionRemSet lock #%u", hr->hrm_index()), true, Monitor::_safepoint_check_never),
  _code_roots(),
  _card_set_mm(config, G1CardSetFreePool::free_list_pool()),
  _card_set(config, &_card_set_mm),
  _hr(hr),
  _state(Untracked) { }

void HeapRegionRemSet::clear_fcc() {
  G1FromCardCache::clear(_hr->hrm_index());
}

void HeapRegionRemSet::clear(bool only_cardset) {
  MutexLocker x(&_m, Mutex::_no_safepoint_check_flag);
  clear_locked(only_cardset);
}

void HeapRegionRemSet::clear_locked(bool only_cardset) {
  if (!only_cardset) {
    _code_roots.clear();
  }
  clear_fcc();
  _card_set.clear();
  set_state_empty();
  assert(occupied() == 0, "Should be clear.");
}

void HeapRegionRemSet::print_static_mem_size(outputStream* out) {
  out->print_cr("  Static structures = " SIZE_FORMAT, HeapRegionRemSet::static_mem_size());
}

// Code roots support
//
// The code root set is protected by two separate locking schemes
// When at safepoint the per-hrrs lock must be held during modifications
// except when doing a full gc.
// When not at safepoint the CodeCache_lock must be held during modifications.
// When concurrent readers access the contains() function
// (during the evacuation phase) no removals are allowed.

void HeapRegionRemSet::add_strong_code_root(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert((!CodeCache_lock->owned_by_self() || SafepointSynchronize::is_at_safepoint()),
          "should call add_strong_code_root_locked instead. CodeCache_lock->owned_by_self(): %s, is_at_safepoint(): %s",
          BOOL_TO_STR(CodeCache_lock->owned_by_self()), BOOL_TO_STR(SafepointSynchronize::is_at_safepoint()));
  // Optimistic unlocked contains-check
  if (!_code_roots.contains(nm)) {
    MutexLocker ml(&_m, Mutex::_no_safepoint_check_flag);
    add_strong_code_root_locked(nm);
  }
}

void HeapRegionRemSet::add_strong_code_root_locked(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert((CodeCache_lock->owned_by_self() ||
         (SafepointSynchronize::is_at_safepoint() &&
          (_m.owned_by_self() || Thread::current()->is_VM_thread()))),
          "not safely locked. CodeCache_lock->owned_by_self(): %s, is_at_safepoint(): %s, _m.owned_by_self(): %s, Thread::current()->is_VM_thread(): %s",
          BOOL_TO_STR(CodeCache_lock->owned_by_self()), BOOL_TO_STR(SafepointSynchronize::is_at_safepoint()),
          BOOL_TO_STR(_m.owned_by_self()), BOOL_TO_STR(Thread::current()->is_VM_thread()));
  _code_roots.add(nm);
}

void HeapRegionRemSet::remove_strong_code_root(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert_locked_or_safepoint(CodeCache_lock);

  MutexLocker ml(CodeCache_lock->owned_by_self() ? NULL : &_m, Mutex::_no_safepoint_check_flag);
  _code_roots.remove(nm);

  // Check that there were no duplicates
  guarantee(!_code_roots.contains(nm), "duplicate entry found");
}

void HeapRegionRemSet::strong_code_roots_do(CodeBlobClosure* blk) const {
  _code_roots.nmethods_do(blk);
}

void HeapRegionRemSet::clean_strong_code_roots(HeapRegion* hr) {
  _code_roots.clean(hr);
}

size_t HeapRegionRemSet::strong_code_roots_mem_size() {
  return _code_roots.mem_size();
}
