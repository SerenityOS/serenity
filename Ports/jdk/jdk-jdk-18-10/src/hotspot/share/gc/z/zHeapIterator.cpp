/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zGranuleMap.inline.hpp"
#include "gc/z/zHeapIterator.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zNMethod.hpp"
#include "gc/z/zOop.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "utilities/bitMap.inline.hpp"

class ZHeapIteratorBitMap : public CHeapObj<mtGC> {
private:
  CHeapBitMap _bitmap;

public:
  ZHeapIteratorBitMap(size_t size_in_bits) :
      _bitmap(size_in_bits, mtGC) {}

  bool try_set_bit(size_t index) {
    return _bitmap.par_set_bit(index);
  }
};

class ZHeapIteratorContext {
private:
  ZHeapIterator* const           _iter;
  ZHeapIteratorQueue* const      _queue;
  ZHeapIteratorArrayQueue* const _array_queue;
  const uint                     _worker_id;
  ZStatTimerDisable              _timer_disable;

public:
  ZHeapIteratorContext(ZHeapIterator* iter, uint worker_id) :
      _iter(iter),
      _queue(_iter->_queues.queue(worker_id)),
      _array_queue(_iter->_array_queues.queue(worker_id)),
      _worker_id(worker_id) {}

  void mark_and_push(oop obj) const {
    if (_iter->mark_object(obj)) {
      _queue->push(obj);
    }
  }

  void push_array(const ObjArrayTask& array) const {
    _array_queue->push(array);
  }

  bool pop(oop& obj) const {
    return _queue->pop_overflow(obj) || _queue->pop_local(obj);
  }

  bool pop_array(ObjArrayTask& array) const {
    return _array_queue->pop_overflow(array) || _array_queue->pop_local(array);
  }

  bool steal(oop& obj) const {
    return _iter->_queues.steal(_worker_id, obj);
  }

  bool steal_array(ObjArrayTask& array) const {
    return _iter->_array_queues.steal(_worker_id, array);
  }

  bool is_drained() const {
    return _queue->is_empty() && _array_queue->is_empty();
  }
};

template <bool Weak>
class ZHeapIteratorRootOopClosure : public OopClosure {
private:
  const ZHeapIteratorContext& _context;

  oop load_oop(oop* p) {
    if (Weak) {
      return NativeAccess<AS_NO_KEEPALIVE | ON_PHANTOM_OOP_REF>::oop_load(p);
    }

    return NativeAccess<AS_NO_KEEPALIVE>::oop_load(p);
  }

public:
  ZHeapIteratorRootOopClosure(const ZHeapIteratorContext& context) :
      _context(context) {}

  virtual void do_oop(oop* p) {
    const oop obj = load_oop(p);
    _context.mark_and_push(obj);
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }
};

template <bool VisitReferents>
class ZHeapIteratorOopClosure : public OopIterateClosure {
private:
  const ZHeapIteratorContext& _context;
  const oop                   _base;

  oop load_oop(oop* p) {
    assert(ZCollectedHeap::heap()->is_in(p), "Should be in heap");

    if (VisitReferents) {
      return HeapAccess<AS_NO_KEEPALIVE | ON_UNKNOWN_OOP_REF>::oop_load_at(_base, _base->field_offset(p));
    }

    return HeapAccess<AS_NO_KEEPALIVE>::oop_load(p);
  }

public:
  ZHeapIteratorOopClosure(const ZHeapIteratorContext& context, oop base) :
      OopIterateClosure(),
      _context(context),
      _base(base) {}

  virtual ReferenceIterationMode reference_iteration_mode() {
    return VisitReferents ? DO_FIELDS : DO_FIELDS_EXCEPT_REFERENT;
  }

  virtual void do_oop(oop* p) {
    const oop obj = load_oop(p);
    _context.mark_and_push(obj);
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  virtual bool do_metadata() {
    return true;
  }

  virtual void do_klass(Klass* k) {
    ClassLoaderData* const cld = k->class_loader_data();
    ZHeapIteratorOopClosure::do_cld(cld);
  }

  virtual void do_cld(ClassLoaderData* cld) {
    class NativeAccessClosure : public OopClosure {
    private:
      const ZHeapIteratorContext& _context;

    public:
      explicit NativeAccessClosure(const ZHeapIteratorContext& context) :
          _context(context) {}

      virtual void do_oop(oop* p) {
        assert(!ZCollectedHeap::heap()->is_in(p), "Should not be in heap");
        const oop obj = NativeAccess<AS_NO_KEEPALIVE>::oop_load(p);
        _context.mark_and_push(obj);
      }

      virtual void do_oop(narrowOop* p) {
        ShouldNotReachHere();
      }
    };

    NativeAccessClosure cl(_context);
    cld->oops_do(&cl, ClassLoaderData::_claim_other);
  }
};

ZHeapIterator::ZHeapIterator(uint nworkers, bool visit_weaks) :
    _visit_weaks(visit_weaks),
    _timer_disable(),
    _bitmaps(ZAddressOffsetMax),
    _bitmaps_lock(),
    _queues(nworkers),
    _array_queues(nworkers),
    _roots(ClassLoaderData::_claim_other),
    _weak_roots(),
    _terminator(nworkers, &_queues) {

  // Create queues
  for (uint i = 0; i < _queues.size(); i++) {
    ZHeapIteratorQueue* const queue = new ZHeapIteratorQueue();
    queue->initialize();
    _queues.register_queue(i, queue);
  }

  // Create array queues
  for (uint i = 0; i < _array_queues.size(); i++) {
    ZHeapIteratorArrayQueue* const array_queue = new ZHeapIteratorArrayQueue();
    array_queue->initialize();
    _array_queues.register_queue(i, array_queue);
  }
}

ZHeapIterator::~ZHeapIterator() {
  // Destroy bitmaps
  ZHeapIteratorBitMapsIterator iter(&_bitmaps);
  for (ZHeapIteratorBitMap* bitmap; iter.next(&bitmap);) {
    delete bitmap;
  }

  // Destroy array queues
  for (uint i = 0; i < _array_queues.size(); i++) {
    delete _array_queues.queue(i);
  }

  // Destroy queues
  for (uint i = 0; i < _queues.size(); i++) {
    delete _queues.queue(i);
  }
}

static size_t object_index_max() {
  return ZGranuleSize >> ZObjectAlignmentSmallShift;
}

static size_t object_index(oop obj) {
  const uintptr_t addr = ZOop::to_address(obj);
  const uintptr_t offset = ZAddress::offset(addr);
  const uintptr_t mask = ZGranuleSize - 1;
  return (offset & mask) >> ZObjectAlignmentSmallShift;
}

ZHeapIteratorBitMap* ZHeapIterator::object_bitmap(oop obj) {
  const uintptr_t offset = ZAddress::offset(ZOop::to_address(obj));
  ZHeapIteratorBitMap* bitmap = _bitmaps.get_acquire(offset);
  if (bitmap == NULL) {
    ZLocker<ZLock> locker(&_bitmaps_lock);
    bitmap = _bitmaps.get(offset);
    if (bitmap == NULL) {
      // Install new bitmap
      bitmap = new ZHeapIteratorBitMap(object_index_max());
      _bitmaps.release_put(offset, bitmap);
    }
  }

  return bitmap;
}

bool ZHeapIterator::mark_object(oop obj) {
  if (obj == NULL) {
    return false;
  }

  ZHeapIteratorBitMap* const bitmap = object_bitmap(obj);
  const size_t index = object_index(obj);
  return bitmap->try_set_bit(index);
}

typedef ClaimingCLDToOopClosure<ClassLoaderData::_claim_other> ZHeapIteratorCLDCLosure;

class ZHeapIteratorNMethodClosure : public NMethodClosure {
private:
  OopClosure* const        _cl;
  BarrierSetNMethod* const _bs_nm;

public:
  ZHeapIteratorNMethodClosure(OopClosure* cl) :
      _cl(cl),
      _bs_nm(BarrierSet::barrier_set()->barrier_set_nmethod()) {}

  virtual void do_nmethod(nmethod* nm) {
    // If ClassUnloading is turned off, all nmethods are considered strong,
    // not only those on the call stacks. The heap iteration might happen
    // before the concurrent processign of the code cache, make sure that
    // all nmethods have been processed before visiting the oops.
    _bs_nm->nmethod_entry_barrier(nm);

    ZNMethod::nmethod_oops_do(nm, _cl);
  }
};

class ZHeapIteratorThreadClosure : public ThreadClosure {
private:
  OopClosure* const        _cl;
  CodeBlobToNMethodClosure _cb_cl;

public:
  ZHeapIteratorThreadClosure(OopClosure* cl, NMethodClosure* nm_cl) :
      _cl(cl),
      _cb_cl(nm_cl) {}

  void do_thread(Thread* thread) {
    thread->oops_do(_cl, &_cb_cl);
  }
};

void ZHeapIterator::push_strong_roots(const ZHeapIteratorContext& context) {
  ZHeapIteratorRootOopClosure<false /* Weak */> cl(context);
  ZHeapIteratorCLDCLosure cld_cl(&cl);
  ZHeapIteratorNMethodClosure nm_cl(&cl);
  ZHeapIteratorThreadClosure thread_cl(&cl, &nm_cl);

  _roots.apply(&cl,
               &cld_cl,
               &thread_cl,
               &nm_cl);
}

void ZHeapIterator::push_weak_roots(const ZHeapIteratorContext& context) {
  ZHeapIteratorRootOopClosure<true  /* Weak */> cl(context);
  _weak_roots.apply(&cl);
}

template <bool VisitWeaks>
void ZHeapIterator::push_roots(const ZHeapIteratorContext& context) {
  push_strong_roots(context);
  if (VisitWeaks) {
    push_weak_roots(context);
  }
}

template <bool VisitReferents>
void ZHeapIterator::follow_object(const ZHeapIteratorContext& context, oop obj) {
  ZHeapIteratorOopClosure<VisitReferents> cl(context, obj);
  obj->oop_iterate(&cl);
}

void ZHeapIterator::follow_array(const ZHeapIteratorContext& context, oop obj) {
  // Follow klass
  ZHeapIteratorOopClosure<false /* VisitReferents */> cl(context, obj);
  cl.do_klass(obj->klass());

  // Push array chunk
  context.push_array(ObjArrayTask(obj, 0 /* index */));
}

void ZHeapIterator::follow_array_chunk(const ZHeapIteratorContext& context, const ObjArrayTask& array) {
  const objArrayOop obj = objArrayOop(array.obj());
  const int length = obj->length();
  const int start = array.index();
  const int stride = MIN2<int>(length - start, ObjArrayMarkingStride);
  const int end = start + stride;

  // Push remaining array chunk first
  if (end < length) {
    context.push_array(ObjArrayTask(obj, end));
  }

  // Follow array chunk
  ZHeapIteratorOopClosure<false /* VisitReferents */> cl(context, obj);
  obj->oop_iterate_range(&cl, start, end);
}

template <bool VisitWeaks>
void ZHeapIterator::visit_and_follow(const ZHeapIteratorContext& context, ObjectClosure* cl, oop obj) {
  // Visit
  cl->do_object(obj);

  // Follow
  if (obj->is_objArray()) {
    follow_array(context, obj);
  } else {
    follow_object<VisitWeaks>(context, obj);
  }
}

template <bool VisitWeaks>
void ZHeapIterator::drain(const ZHeapIteratorContext& context, ObjectClosure* cl) {
  ObjArrayTask array;
  oop obj;

  do {
    while (context.pop(obj)) {
      visit_and_follow<VisitWeaks>(context, cl, obj);
    }

    if (context.pop_array(array)) {
      follow_array_chunk(context, array);
    }
  } while (!context.is_drained());
}

template <bool VisitWeaks>
void ZHeapIterator::steal(const ZHeapIteratorContext& context, ObjectClosure* cl) {
  ObjArrayTask array;
  oop obj;

  if (context.steal_array(array)) {
    follow_array_chunk(context, array);
  } else if (context.steal(obj)) {
    visit_and_follow<VisitWeaks>(context, cl, obj);
  }
}

template <bool VisitWeaks>
void ZHeapIterator::drain_and_steal(const ZHeapIteratorContext& context, ObjectClosure* cl) {
  do {
    drain<VisitWeaks>(context, cl);
    steal<VisitWeaks>(context, cl);
  } while (!context.is_drained() || !_terminator.offer_termination());
}

template <bool VisitWeaks>
void ZHeapIterator::object_iterate_inner(const ZHeapIteratorContext& context, ObjectClosure* object_cl) {
  push_roots<VisitWeaks>(context);
  drain_and_steal<VisitWeaks>(context, object_cl);
}

void ZHeapIterator::object_iterate(ObjectClosure* cl, uint worker_id) {
  ZHeapIteratorContext context(this, worker_id);

  if (_visit_weaks) {
    object_iterate_inner<true /* VisitWeaks */>(context, cl);
  } else {
    object_iterate_inner<false /* VisitWeaks */>(context, cl);
  }
}
