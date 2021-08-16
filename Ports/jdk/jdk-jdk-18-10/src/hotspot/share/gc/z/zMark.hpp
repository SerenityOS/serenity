/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZMARK_HPP
#define SHARE_GC_Z_ZMARK_HPP

#include "gc/z/zMarkStack.hpp"
#include "gc/z/zMarkStackAllocator.hpp"
#include "gc/z/zMarkStackEntry.hpp"
#include "gc/z/zMarkTerminate.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/globalDefinitions.hpp"

class Thread;
class ZMarkCache;
class ZPageTable;
class ZWorkers;

class ZMark {
  friend class ZMarkTask;

private:
  ZWorkers* const     _workers;
  ZPageTable* const   _page_table;
  ZMarkStackAllocator _allocator;
  ZMarkStripeSet      _stripes;
  ZMarkTerminate      _terminate;
  volatile bool       _work_terminateflush;
  volatile size_t     _work_nproactiveflush;
  volatile size_t     _work_nterminateflush;
  size_t              _nproactiveflush;
  size_t              _nterminateflush;
  size_t              _ntrycomplete;
  size_t              _ncontinue;
  uint                _nworkers;

  size_t calculate_nstripes(uint nworkers) const;

  bool is_array(uintptr_t addr) const;
  void push_partial_array(uintptr_t addr, size_t size, bool finalizable);
  void follow_small_array(uintptr_t addr, size_t size, bool finalizable);
  void follow_large_array(uintptr_t addr, size_t size, bool finalizable);
  void follow_array(uintptr_t addr, size_t size, bool finalizable);
  void follow_partial_array(ZMarkStackEntry entry, bool finalizable);
  void follow_array_object(objArrayOop obj, bool finalizable);
  void follow_object(oop obj, bool finalizable);
  void mark_and_follow(ZMarkCache* cache, ZMarkStackEntry entry);

  template <typename T> bool drain(ZMarkStripe* stripe,
                                   ZMarkThreadLocalStacks* stacks,
                                   ZMarkCache* cache,
                                   T* timeout);
  bool try_steal_local(ZMarkStripe* stripe, ZMarkThreadLocalStacks* stacks);
  bool try_steal_global(ZMarkStripe* stripe, ZMarkThreadLocalStacks* stacks);
  bool try_steal(ZMarkStripe* stripe, ZMarkThreadLocalStacks* stacks);
  void idle() const;
  bool flush(bool at_safepoint);
  bool try_proactive_flush();
  bool try_flush(volatile size_t* nflush);
  bool try_terminate();
  bool try_complete();
  bool try_end();

  void prepare_work();
  void finish_work();

  void work_without_timeout(ZMarkCache* cache,
                            ZMarkStripe* stripe,
                            ZMarkThreadLocalStacks* stacks);
  void work_with_timeout(ZMarkCache* cache,
                         ZMarkStripe* stripe,
                         ZMarkThreadLocalStacks* stacks,
                         uint64_t timeout_in_micros);
  void work(uint64_t timeout_in_micros);

  void verify_all_stacks_empty() const;

public:
  ZMark(ZWorkers* workers, ZPageTable* page_table);

  bool is_initialized() const;

  template <bool gc_thread, bool follow, bool finalizable, bool publish> void mark_object(uintptr_t addr);

  void start();
  void mark(bool initial);
  bool end();
  void free();

  void flush_and_free();
  bool flush_and_free(Thread* thread);
};

#endif // SHARE_GC_Z_ZMARK_HPP
