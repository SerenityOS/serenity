/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_HPP
#define SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_HPP

#include "gc/parallel/psParallelCompact.hpp"
#include "gc/shared/taskqueue.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "memory/allocation.hpp"
#include "utilities/stack.hpp"

class MutableSpace;
class PSOldGen;
class ParCompactionManager;
class ObjectStartArray;
class ParallelCompactData;
class ParMarkBitMap;

class ParCompactionManager : public CHeapObj<mtGC> {
  friend class MarkFromRootsTask;
  friend class ParallelCompactRefProcProxyTask;
  friend class ParallelScavengeRefProcProxyTask;
  friend class ParMarkBitMap;
  friend class PSParallelCompact;
  friend class UpdateDensePrefixAndCompactionTask;

 private:
  typedef GenericTaskQueue<oop, mtGC>             OopTaskQueue;
  typedef GenericTaskQueueSet<OopTaskQueue, mtGC> OopTaskQueueSet;

  // 32-bit:  4K * 8 = 32KiB; 64-bit:  8K * 16 = 128KiB
  #define QUEUE_SIZE (1 << NOT_LP64(12) LP64_ONLY(13))
  typedef OverflowTaskQueue<ObjArrayTask, mtGC, QUEUE_SIZE> ObjArrayTaskQueue;
  typedef GenericTaskQueueSet<ObjArrayTaskQueue, mtGC>      ObjArrayTaskQueueSet;
  #undef QUEUE_SIZE
  typedef OverflowTaskQueue<size_t, mtGC>             RegionTaskQueue;
  typedef GenericTaskQueueSet<RegionTaskQueue, mtGC>  RegionTaskQueueSet;

  static ParCompactionManager** _manager_array;
  static OopTaskQueueSet*       _oop_task_queues;
  static ObjArrayTaskQueueSet*  _objarray_task_queues;
  static ObjectStartArray*      _start_array;
  static RegionTaskQueueSet*    _region_task_queues;
  static PSOldGen*              _old_gen;

  OverflowTaskQueue<oop, mtGC>        _marking_stack;
  ObjArrayTaskQueue             _objarray_stack;
  size_t                        _next_shadow_region;

  // Is there a way to reuse the _marking_stack for the
  // saving empty regions?  For now just create a different
  // type of TaskQueue.
  RegionTaskQueue              _region_stack;

  static ParMarkBitMap* _mark_bitmap;

  // Contains currently free shadow regions. We use it in
  // a LIFO fashion for better data locality and utilization.
  static GrowableArray<size_t>* _shadow_region_array;

  // Provides mutual exclusive access of _shadow_region_array.
  // See pop/push_shadow_region_mt_safe() below
  static Monitor*               _shadow_region_monitor;

  HeapWord* _last_query_beg;
  oop _last_query_obj;
  size_t _last_query_ret;

  static PSOldGen* old_gen()             { return _old_gen; }
  static ObjectStartArray* start_array() { return _start_array; }
  static OopTaskQueueSet* oop_task_queues()  { return _oop_task_queues; }

  static void initialize(ParMarkBitMap* mbm);

 protected:
  // Array of task queues.  Needed by the task terminator.
  static RegionTaskQueueSet* region_task_queues()      { return _region_task_queues; }
  OverflowTaskQueue<oop, mtGC>*  marking_stack()       { return &_marking_stack; }

  // Pushes onto the marking stack.  If the marking stack is full,
  // pushes onto the overflow stack.
  void stack_push(oop obj);
  // Do not implement an equivalent stack_pop.  Deal with the
  // marking stack and overflow stack directly.

 public:
  static const size_t InvalidShadow = ~0;
  static size_t  pop_shadow_region_mt_safe(PSParallelCompact::RegionData* region_ptr);
  static void    push_shadow_region_mt_safe(size_t shadow_region);
  static void    push_shadow_region(size_t shadow_region);
  static void    remove_all_shadow_regions();

  inline size_t  next_shadow_region() { return _next_shadow_region; }
  inline void    set_next_shadow_region(size_t record) { _next_shadow_region = record; }
  inline size_t  move_next_shadow_region_by(size_t workers) {
    _next_shadow_region += workers;
    return next_shadow_region();
  }

  void reset_bitmap_query_cache() {
    _last_query_beg = NULL;
    _last_query_obj = NULL;
    _last_query_ret = 0;
  }

  // Bitmap query support, cache last query and result
  HeapWord* last_query_begin() { return _last_query_beg; }
  oop last_query_object() { return _last_query_obj; }
  size_t last_query_return() { return _last_query_ret; }

  void set_last_query_begin(HeapWord *new_beg) { _last_query_beg = new_beg; }
  void set_last_query_object(oop new_obj) { _last_query_obj = new_obj; }
  void set_last_query_return(size_t new_ret) { _last_query_ret = new_ret; }

  static void reset_all_bitmap_query_caches();

  RegionTaskQueue* region_stack()                { return &_region_stack; }

  static ParCompactionManager* get_vmthread_cm() { return _manager_array[ParallelGCThreads]; }

  ParCompactionManager();

  // Pushes onto the region stack at the given index.  If the
  // region stack is full,
  // pushes onto the region overflow stack.
  static void verify_region_list_empty(uint stack_index);
  ParMarkBitMap* mark_bitmap() { return _mark_bitmap; }

  // void drain_stacks();

  bool should_update();
  bool should_copy();

  // Save for later processing.  Must not fail.
  inline void push(oop obj);
  inline void push_objarray(oop objarray, size_t index);
  inline void push_region(size_t index);

  // Check mark and maybe push on marking stack.
  template <typename T> inline void mark_and_push(T* p);

  inline void follow_klass(Klass* klass);

  void follow_class_loader(ClassLoaderData* klass);

  // Access function for compaction managers
  static ParCompactionManager* gc_thread_compaction_manager(uint index);

  static bool steal(int queue_num, oop& t);
  static bool steal_objarray(int queue_num, ObjArrayTask& t);
  static bool steal(int queue_num, size_t& region);

  // Process tasks remaining on any marking stack
  void follow_marking_stacks();
  inline bool marking_stacks_empty() const;

  // Process tasks remaining on any stack
  void drain_region_stacks();

  void follow_contents(oop obj);
  void follow_array(objArrayOop array, int index);

  void update_contents(oop obj);

  class FollowStackClosure: public VoidClosure {
   private:
    ParCompactionManager* _compaction_manager;
    TaskTerminator* _terminator;
    uint _worker_id;
   public:
    FollowStackClosure(ParCompactionManager* cm, TaskTerminator* terminator, uint worker_id)
      : _compaction_manager(cm), _terminator(terminator), _worker_id(worker_id) { }
    virtual void do_void();
  };

  // Called after marking.
  static void verify_all_marking_stack_empty() NOT_DEBUG_RETURN;

  // Region staks hold regions in from-space; called after compaction.
  static void verify_all_region_stack_empty() NOT_DEBUG_RETURN;
};

bool ParCompactionManager::marking_stacks_empty() const {
  return _marking_stack.is_empty() && _objarray_stack.is_empty();
}

#endif // SHARE_GC_PARALLEL_PSCOMPACTIONMANAGER_HPP
