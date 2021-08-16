/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_INLINE_HPP
#define SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_INLINE_HPP

#include "gc/parallel/psCompactionManager.hpp"

#include "classfile/classLoaderData.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "gc/parallel/parMarkBitMap.hpp"
#include "gc/parallel/psParallelCompact.inline.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/arrayOop.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class PCMarkAndPushClosure: public OopClosure {
private:
  ParCompactionManager* _compaction_manager;
public:
  PCMarkAndPushClosure(ParCompactionManager* cm) : _compaction_manager(cm) { }

  template <typename T> void do_oop_nv(T* p)      { _compaction_manager->mark_and_push(p); }
  virtual void do_oop(oop* p)                     { do_oop_nv(p); }
  virtual void do_oop(narrowOop* p)               { do_oop_nv(p); }
};

class PCIterateMarkAndPushClosure: public MetadataVisitingOopIterateClosure {
private:
  ParCompactionManager* _compaction_manager;
public:
  PCIterateMarkAndPushClosure(ParCompactionManager* cm, ReferenceProcessor* rp) : MetadataVisitingOopIterateClosure(rp), _compaction_manager(cm) { }

  template <typename T> void do_oop_nv(T* p)      { _compaction_manager->mark_and_push(p); }
  virtual void do_oop(oop* p)                     { do_oop_nv(p); }
  virtual void do_oop(narrowOop* p)               { do_oop_nv(p); }

  void do_klass_nv(Klass* k)                      { _compaction_manager->follow_klass(k); }
  void do_cld_nv(ClassLoaderData* cld)            { _compaction_manager->follow_class_loader(cld); }
};

inline bool ParCompactionManager::steal(int queue_num, oop& t) {
  return oop_task_queues()->steal(queue_num, t);
}

inline bool ParCompactionManager::steal_objarray(int queue_num, ObjArrayTask& t) {
  return _objarray_task_queues->steal(queue_num, t);
}

inline bool ParCompactionManager::steal(int queue_num, size_t& region) {
  return region_task_queues()->steal(queue_num, region);
}

inline void ParCompactionManager::push(oop obj) {
  _marking_stack.push(obj);
}

void ParCompactionManager::push_objarray(oop obj, size_t index)
{
  ObjArrayTask task(obj, index);
  assert(task.is_valid(), "bad ObjArrayTask");
  _objarray_stack.push(task);
}

void ParCompactionManager::push_region(size_t index)
{
#ifdef ASSERT
  const ParallelCompactData& sd = PSParallelCompact::summary_data();
  ParallelCompactData::RegionData* const region_ptr = sd.region(index);
  assert(region_ptr->claimed(), "must be claimed");
  assert(region_ptr->_pushed++ == 0, "should only be pushed once");
#endif
  region_stack()->push(index);
}

template <typename T>
inline void ParCompactionManager::mark_and_push(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    assert(ParallelScavengeHeap::heap()->is_in(obj), "should be in heap");

    if (mark_bitmap()->is_unmarked(obj) && PSParallelCompact::mark_obj(obj)) {
      push(obj);
    }
  }
}

inline void ParCompactionManager::follow_klass(Klass* klass) {
  oop holder = klass->class_loader_data()->holder_no_keepalive();
  mark_and_push(&holder);
}

inline void ParCompactionManager::FollowStackClosure::do_void() {
  _compaction_manager->follow_marking_stacks();
  if (_terminator != nullptr) {
    steal_marking_work(*_terminator, _worker_id);
  }
}

template <typename T>
inline void follow_array_specialized(objArrayOop obj, int index, ParCompactionManager* cm) {
  const size_t len = size_t(obj->length());
  const size_t beg_index = size_t(index);
  assert(beg_index < len || len == 0, "index too large");

  const size_t stride = MIN2(len - beg_index, (size_t)ObjArrayMarkingStride);
  const size_t end_index = beg_index + stride;
  T* const base = (T*)obj->base();
  T* const beg = base + beg_index;
  T* const end = base + end_index;

  if (end_index < len) {
    cm->push_objarray(obj, end_index); // Push the continuation.
  }

  // Push the non-NULL elements of the next stride on the marking stack.
  for (T* e = beg; e < end; e++) {
    cm->mark_and_push<T>(e);
  }
}

inline void ParCompactionManager::follow_array(objArrayOop obj, int index) {
  if (UseCompressedOops) {
    follow_array_specialized<narrowOop>(obj, index, this);
  } else {
    follow_array_specialized<oop>(obj, index, this);
  }
}

inline void ParCompactionManager::update_contents(oop obj) {
  if (!obj->klass()->is_typeArray_klass()) {
    PCAdjustPointerClosure apc(this);
    obj->oop_iterate(&apc);
  }
}

inline void ParCompactionManager::follow_class_loader(ClassLoaderData* cld) {
  PCMarkAndPushClosure mark_and_push_closure(this);
  cld->oops_do(&mark_and_push_closure, true);
}

inline void ParCompactionManager::follow_contents(oop obj) {
  assert(PSParallelCompact::mark_bitmap()->is_marked(obj), "should be marked");
  if (obj->is_objArray()) {
    follow_array(objArrayOop(obj), 0);
  } else {
    PCIterateMarkAndPushClosure cl(this, PSParallelCompact::ref_processor());
    obj->oop_iterate(&cl);
  }
}

#endif // SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_INLINE_HPP
