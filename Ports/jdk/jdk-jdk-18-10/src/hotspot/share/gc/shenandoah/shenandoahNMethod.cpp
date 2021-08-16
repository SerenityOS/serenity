/*
 * Copyright (c) 2019, 2021, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahNMethod.inline.hpp"
#include "memory/resourceArea.hpp"

ShenandoahNMethod::ShenandoahNMethod(nmethod* nm, GrowableArray<oop*>& oops, bool non_immediate_oops) :
  _nm(nm), _oops(NULL), _oops_count(0), _unregistered(false) {

  if (!oops.is_empty()) {
    _oops_count = oops.length();
    _oops = NEW_C_HEAP_ARRAY(oop*, _oops_count, mtGC);
    for (int c = 0; c < _oops_count; c++) {
      _oops[c] = oops.at(c);
    }
  }
  _has_non_immed_oops = non_immediate_oops;

  assert_same_oops();
}

ShenandoahNMethod::~ShenandoahNMethod() {
  if (_oops != NULL) {
    FREE_C_HEAP_ARRAY(oop*, _oops);
  }
}

class ShenandoahHasCSetOopClosure : public OopClosure {
private:
  ShenandoahHeap* const _heap;
  bool                  _has_cset_oops;

public:
  ShenandoahHasCSetOopClosure(ShenandoahHeap *heap) :
    _heap(heap),
    _has_cset_oops(false) {
  }

  bool has_cset_oops() const {
    return _has_cset_oops;
  }

  void do_oop(oop* p) {
    oop value = RawAccess<>::oop_load(p);
    if (!_has_cset_oops && _heap->in_collection_set(value)) {
      _has_cset_oops = true;
    }
  }

  void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }
};

bool ShenandoahNMethod::has_cset_oops(ShenandoahHeap *heap) {
  ShenandoahHasCSetOopClosure cl(heap);
  oops_do(&cl);
  return cl.has_cset_oops();
}

void ShenandoahNMethod::update() {
  ResourceMark rm;
  bool non_immediate_oops = false;
  GrowableArray<oop*> oops;

  detect_reloc_oops(nm(), oops, non_immediate_oops);
  if (oops.length() != _oops_count) {
    if (_oops != NULL) {
      FREE_C_HEAP_ARRAY(oop*, _oops);
      _oops = NULL;
    }

    _oops_count = oops.length();
    if (_oops_count > 0) {
      _oops = NEW_C_HEAP_ARRAY(oop*, _oops_count, mtGC);
    }
  }

  for (int index = 0; index < _oops_count; index ++) {
    _oops[index] = oops.at(index);
  }
  _has_non_immed_oops = non_immediate_oops;

  assert_same_oops();
}

void ShenandoahNMethod::detect_reloc_oops(nmethod* nm, GrowableArray<oop*>& oops, bool& has_non_immed_oops) {
  has_non_immed_oops = false;
  // Find all oops relocations
  RelocIterator iter(nm);
  while (iter.next()) {
    if (iter.type() != relocInfo::oop_type) {
      // Not an oop
      continue;
    }

    oop_Relocation* r = iter.oop_reloc();
    if (!r->oop_is_immediate()) {
      // Non-immediate oop found
      has_non_immed_oops = true;
      continue;
    }

    oop value = r->oop_value();
    if (value != NULL) {
      oop* addr = r->oop_addr();
      shenandoah_assert_correct(addr, value);
      shenandoah_assert_not_in_cset_except(addr, value, ShenandoahHeap::heap()->cancelled_gc());
      shenandoah_assert_not_forwarded(addr, value);
      // Non-NULL immediate oop found. NULL oops can safely be
      // ignored since the method will be re-registered if they
      // are later patched to be non-NULL.
      oops.push(addr);
    }
  }
}

ShenandoahNMethod* ShenandoahNMethod::for_nmethod(nmethod* nm) {
  ResourceMark rm;
  bool non_immediate_oops = false;
  GrowableArray<oop*> oops;

  detect_reloc_oops(nm, oops, non_immediate_oops);
  return new ShenandoahNMethod(nm, oops, non_immediate_oops);
}

void ShenandoahNMethod::heal_nmethod(nmethod* nm) {
  ShenandoahNMethod* data = gc_data(nm);
  assert(data != NULL, "Sanity");
  assert(data->lock()->owned_by_self(), "Must hold the lock");

  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  if (heap->is_concurrent_mark_in_progress()) {
    ShenandoahKeepAliveClosure cl;
    data->oops_do(&cl);
  } else if (heap->is_concurrent_weak_root_in_progress() ||
             heap->is_concurrent_strong_root_in_progress()) {
    ShenandoahEvacOOMScope evac_scope;
    heal_nmethod_metadata(data);
  } else {
    // There is possibility that GC is cancelled when it arrives final mark.
    // In this case, concurrent root phase is skipped and degenerated GC should be
    // followed, where nmethods are disarmed.
    assert(heap->cancelled_gc(), "What else?");
  }
}

#ifdef ASSERT
void ShenandoahNMethod::assert_correct() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  for (int c = 0; c < _oops_count; c++) {
    oop *loc = _oops[c];
    assert(_nm->code_contains((address) loc) || _nm->oops_contains(loc), "nmethod should contain the oop*");
    oop o = RawAccess<>::oop_load(loc);
    shenandoah_assert_correct_except(loc, o, o == NULL || heap->is_full_gc_move_in_progress());
  }

  oop* const begin = _nm->oops_begin();
  oop* const end = _nm->oops_end();
  for (oop* p = begin; p < end; p++) {
    if (*p != Universe::non_oop_word()) {
      oop o = RawAccess<>::oop_load(p);
      shenandoah_assert_correct_except(p, o, o == NULL || heap->is_full_gc_move_in_progress());
    }
  }
}

class ShenandoahNMethodOopDetector : public OopClosure {
private:
  ResourceMark rm; // For growable array allocation below.
  GrowableArray<oop*> _oops;

public:
  ShenandoahNMethodOopDetector() : _oops(10) {};

  void do_oop(oop* o) {
    _oops.append(o);
  }
  void do_oop(narrowOop* o) {
    fatal("NMethods should not have compressed oops embedded.");
  }

  GrowableArray<oop*>* oops() {
    return &_oops;
  }

  bool has_oops() {
    return !_oops.is_empty();
  }
};

void ShenandoahNMethod::assert_same_oops(bool allow_dead) {
  ShenandoahNMethodOopDetector detector;
  nm()->oops_do(&detector, allow_dead);

  GrowableArray<oop*>* oops = detector.oops();

  int count = _oops_count;
  for (int index = 0; index < _oops_count; index ++) {
    assert(oops->contains(_oops[index]), "Must contain this oop");
  }

  for (oop* p = nm()->oops_begin(); p < nm()->oops_end(); p ++) {
    if (*p == Universe::non_oop_word()) continue;
    count++;
    assert(oops->contains(p), "Must contain this oop");
  }

  if (oops->length() < count) {
    stringStream debug_stream;
    debug_stream.print_cr("detected locs: %d", oops->length());
    for (int i = 0; i < oops->length(); i++) {
      debug_stream.print_cr("-> " PTR_FORMAT, p2i(oops->at(i)));
    }
    debug_stream.print_cr("recorded oops: %d", _oops_count);
    for (int i = 0; i < _oops_count; i++) {
      debug_stream.print_cr("-> " PTR_FORMAT, p2i(_oops[i]));
    }
    GrowableArray<oop*> check;
    bool non_immed;
    detect_reloc_oops(nm(), check, non_immed);
    debug_stream.print_cr("check oops: %d", check.length());
    for (int i = 0; i < check.length(); i++) {
      debug_stream.print_cr("-> " PTR_FORMAT, p2i(check.at(i)));
    }
    fatal("Must match #detected: %d, #recorded: %d, #total: %d, begin: " PTR_FORMAT ", end: " PTR_FORMAT "\n%s",
          oops->length(), _oops_count, count, p2i(nm()->oops_begin()), p2i(nm()->oops_end()), debug_stream.as_string());
  }
}
#endif

ShenandoahNMethodTable::ShenandoahNMethodTable() :
  _heap(ShenandoahHeap::heap()),
  _index(0),
  _itr_cnt(0) {
  _list = new ShenandoahNMethodList(minSize);
}

ShenandoahNMethodTable::~ShenandoahNMethodTable() {
  assert(_list != NULL, "Sanity");
  _list->release();
}

void ShenandoahNMethodTable::register_nmethod(nmethod* nm) {
  assert(CodeCache_lock->owned_by_self(), "Must have CodeCache_lock held");
  assert(_index >= 0 && _index <= _list->size(), "Sanity");

  ShenandoahNMethod* data = ShenandoahNMethod::gc_data(nm);
  ShenandoahReentrantLocker data_locker(data != NULL ? data->lock() : NULL);

  if (data != NULL) {
    assert(contain(nm), "Must have been registered");
    assert(nm == data->nm(), "Must be same nmethod");
    data->update();
  } else {
    data = ShenandoahNMethod::for_nmethod(nm);
    assert(data != NULL, "Sanity");
    ShenandoahNMethod::attach_gc_data(nm, data);
    ShenandoahLocker locker(&_lock);
    log_register_nmethod(nm);
    append(data);
  }
  // Disarm new nmethod
  ShenandoahNMethod::disarm_nmethod(nm);
}

void ShenandoahNMethodTable::unregister_nmethod(nmethod* nm) {
  assert_locked_or_safepoint(CodeCache_lock);

  ShenandoahNMethod* data = ShenandoahNMethod::gc_data(nm);
  assert(data != NULL, "Sanity");
  if (Thread::current()->is_Code_cache_sweeper_thread()) {
    wait_until_concurrent_iteration_done();
  }
  log_unregister_nmethod(nm);
  ShenandoahLocker locker(&_lock);
  assert(contain(nm), "Must have been registered");

  ShenandoahReentrantLocker data_locker(data->lock());
  data->mark_unregistered();
}

void ShenandoahNMethodTable::flush_nmethod(nmethod* nm) {
  assert(CodeCache_lock->owned_by_self(), "Must have CodeCache_lock held");
  assert(Thread::current()->is_Code_cache_sweeper_thread(), "Must from Sweep thread");
  ShenandoahNMethod* data = ShenandoahNMethod::gc_data(nm);
  assert(data != NULL, "Sanity");

  // Can not alter the array when iteration is in progress
  wait_until_concurrent_iteration_done();
  log_flush_nmethod(nm);

  ShenandoahLocker locker(&_lock);
  int idx = index_of(nm);
  assert(idx >= 0 && idx < _index, "Invalid index");
  ShenandoahNMethod::attach_gc_data(nm, NULL);
  remove(idx);
}

bool ShenandoahNMethodTable::contain(nmethod* nm) const {
  return index_of(nm) != -1;
}

ShenandoahNMethod* ShenandoahNMethodTable::at(int index) const {
  assert(index >= 0 && index < _index, "Out of bound");
  return _list->at(index);
}

int ShenandoahNMethodTable::index_of(nmethod* nm) const {
  for (int index = 0; index < length(); index ++) {
    if (at(index)->nm() == nm) {
      return index;
    }
  }
  return -1;
}

void ShenandoahNMethodTable::remove(int idx) {
  shenandoah_assert_locked_or_safepoint(CodeCache_lock);
  assert(!iteration_in_progress(), "Can not happen");
  assert(_index >= 0 && _index <= _list->size(), "Sanity");

  assert(idx >= 0 && idx < _index, "Out of bound");
  ShenandoahNMethod* snm = _list->at(idx);
  ShenandoahNMethod* tmp = _list->at(_index - 1);
  _list->set(idx, tmp);
  _index --;

  delete snm;
}

void ShenandoahNMethodTable::wait_until_concurrent_iteration_done() {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");
  while (iteration_in_progress()) {
    CodeCache_lock->wait_without_safepoint_check();
  }
}

void ShenandoahNMethodTable::append(ShenandoahNMethod* snm) {
  if (is_full()) {
    int new_size = 2 * _list->size();
    // Rebuild table and replace current one
    rebuild(new_size);
  }

  _list->set(_index++,  snm);
  assert(_index >= 0 && _index <= _list->size(), "Sanity");
}

void ShenandoahNMethodTable::rebuild(int size) {
  ShenandoahNMethodList* new_list = new ShenandoahNMethodList(size);
  new_list->transfer(_list, _index);

  // Release old list
  _list->release();
  _list = new_list;
}

ShenandoahNMethodTableSnapshot* ShenandoahNMethodTable::snapshot_for_iteration() {
  _itr_cnt++;
  return new ShenandoahNMethodTableSnapshot(this);
}

void ShenandoahNMethodTable::finish_iteration(ShenandoahNMethodTableSnapshot* snapshot) {
  assert(iteration_in_progress(), "Why we here?");
  assert(snapshot != NULL, "No snapshot");
  _itr_cnt--;

  delete snapshot;
}

void ShenandoahNMethodTable::log_register_nmethod(nmethod* nm) {
  LogTarget(Debug, gc, nmethod) log;
  if (!log.is_enabled()) {
    return;
  }

  ResourceMark rm;
  log.print("Register NMethod: %s.%s [" PTR_FORMAT "] (%s)",
            nm->method()->method_holder()->external_name(),
            nm->method()->name()->as_C_string(),
            p2i(nm),
            nm->compiler_name());
}

void ShenandoahNMethodTable::log_unregister_nmethod(nmethod* nm) {
  LogTarget(Debug, gc, nmethod) log;
  if (!log.is_enabled()) {
    return;
  }

  ResourceMark rm;
  log.print("Unregister NMethod: %s.%s [" PTR_FORMAT "]",
            nm->method()->method_holder()->external_name(),
            nm->method()->name()->as_C_string(),
            p2i(nm));
}

void ShenandoahNMethodTable::log_flush_nmethod(nmethod* nm) {
  LogTarget(Debug, gc, nmethod) log;
  if (!log.is_enabled()) {
    return;
  }

  ResourceMark rm;
  log.print("Flush NMethod: (" PTR_FORMAT ")", p2i(nm));
}

#ifdef ASSERT
void ShenandoahNMethodTable::assert_nmethods_correct() {
  assert_locked_or_safepoint(CodeCache_lock);

  for (int index = 0; index < length(); index ++) {
    ShenandoahNMethod* m = _list->at(index);
    // Concurrent unloading may have dead nmethods to be cleaned by sweeper
    if (m->is_unregistered()) continue;
    m->assert_correct();
  }
}
#endif


ShenandoahNMethodList::ShenandoahNMethodList(int size) :
  _size(size), _ref_count(1) {
  _list = NEW_C_HEAP_ARRAY(ShenandoahNMethod*, size, mtGC);
}

ShenandoahNMethodList::~ShenandoahNMethodList() {
  assert(_list != NULL, "Sanity");
  assert(_ref_count == 0, "Must be");
  FREE_C_HEAP_ARRAY(ShenandoahNMethod*, _list);
}

void ShenandoahNMethodList::transfer(ShenandoahNMethodList* const list, int limit) {
  assert(limit <= size(), "Sanity");
  ShenandoahNMethod** old_list = list->list();
  for (int index = 0; index < limit; index++) {
    _list[index] = old_list[index];
  }
}

ShenandoahNMethodList* ShenandoahNMethodList::acquire() {
  assert_locked_or_safepoint(CodeCache_lock);
  _ref_count++;
  return this;
}

void ShenandoahNMethodList::release() {
  assert_locked_or_safepoint(CodeCache_lock);
  _ref_count--;
  if (_ref_count == 0) {
    delete this;
  }
}

ShenandoahNMethodTableSnapshot::ShenandoahNMethodTableSnapshot(ShenandoahNMethodTable* table) :
  _heap(ShenandoahHeap::heap()), _list(table->_list->acquire()), _limit(table->_index), _claimed(0) {
}

ShenandoahNMethodTableSnapshot::~ShenandoahNMethodTableSnapshot() {
  _list->release();
}

void ShenandoahNMethodTableSnapshot::parallel_blobs_do(CodeBlobClosure *f) {
  size_t stride = 256; // educated guess

  ShenandoahNMethod** const list = _list->list();

  size_t max = (size_t)_limit;
  while (_claimed < max) {
    size_t cur = Atomic::fetch_and_add(&_claimed, stride);
    size_t start = cur;
    size_t end = MIN2(cur + stride, max);
    if (start >= max) break;

    for (size_t idx = start; idx < end; idx++) {
      ShenandoahNMethod* nmr = list[idx];
      assert(nmr != NULL, "Sanity");
      if (nmr->is_unregistered()) {
        continue;
      }

      // A nmethod can become a zombie before it is unregistered.
      if (nmr->nm()->is_alive()) {
        nmr->assert_correct();
        f->do_code_blob(nmr->nm());
      }
    }
  }
}

void ShenandoahNMethodTableSnapshot::concurrent_nmethods_do(NMethodClosure* cl) {
  size_t stride = 256; // educated guess

  ShenandoahNMethod** list = _list->list();
  size_t max = (size_t)_limit;
  while (_claimed < max) {
    size_t cur = Atomic::fetch_and_add(&_claimed, stride);
    size_t start = cur;
    size_t end = MIN2(cur + stride, max);
    if (start >= max) break;

    for (size_t idx = start; idx < end; idx++) {
      ShenandoahNMethod* data = list[idx];
      assert(data != NULL, "Should not be NULL");
      if (!data->is_unregistered()) {
        cl->do_nmethod(data->nm());
      }
    }
  }
}

ShenandoahConcurrentNMethodIterator::ShenandoahConcurrentNMethodIterator(ShenandoahNMethodTable* table) :
  _table(table), _table_snapshot(NULL) {
}

void ShenandoahConcurrentNMethodIterator::nmethods_do_begin() {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");
  _table_snapshot = _table->snapshot_for_iteration();
}

void ShenandoahConcurrentNMethodIterator::nmethods_do(NMethodClosure* cl) {
  assert(_table_snapshot != NULL, "Must first call nmethod_do_begin()");
  _table_snapshot->concurrent_nmethods_do(cl);
}

void ShenandoahConcurrentNMethodIterator::nmethods_do_end() {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");
  _table->finish_iteration(_table_snapshot);
  CodeCache_lock->notify_all();
}
