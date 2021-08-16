/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_CARDTABLEBARRIERSET_HPP
#define SHARE_GC_SHARED_CARDTABLEBARRIERSET_HPP

#include "gc/shared/cardTable.hpp"
#include "gc/shared/modRefBarrierSet.hpp"
#include "utilities/align.hpp"

// This kind of "BarrierSet" allows a "CollectedHeap" to detect and
// enumerate ref fields that have been modified (since the last
// enumeration.)

// As it currently stands, this barrier is *imprecise*: when a ref field in
// an object "o" is modified, the card table entry for the card containing
// the head of "o" is dirtied, not necessarily the card containing the
// modified field itself.  For object arrays, however, the barrier *is*
// precise; only the card containing the modified element is dirtied.
// Closures used to scan dirty cards should take these
// considerations into account.

class CardTableBarrierSet: public ModRefBarrierSet {
  // Some classes get to look at some private stuff.
  friend class VMStructs;

public:

  typedef CardTable::CardValue CardValue;
protected:
  // Used in support of ReduceInitialCardMarks; only consulted if COMPILER2
  // or INCLUDE_JVMCI is being used
  bool       _defer_initial_card_mark;
  CardTable* _card_table;

  CardTableBarrierSet(BarrierSetAssembler* barrier_set_assembler,
                      BarrierSetC1* barrier_set_c1,
                      BarrierSetC2* barrier_set_c2,
                      CardTable* card_table,
                      const BarrierSet::FakeRtti& fake_rtti);

 public:
  CardTableBarrierSet(CardTable* card_table);
  ~CardTableBarrierSet();

  CardTable* card_table() const { return _card_table; }

  virtual void initialize();

  void write_region(MemRegion mr) {
    invalidate(mr);
  }

  void write_ref_array_work(MemRegion mr);

 public:
  // Record a reference update. Note that these versions are precise!
  // The scanning code has to handle the fact that the write barrier may be
  // either precise or imprecise. We make non-virtual inline variants of
  // these functions here for performance.
  template <DecoratorSet decorators, typename T>
  void write_ref_field_post(T* field, oop newVal);

  virtual void invalidate(MemRegion mr);

  // ReduceInitialCardMarks
  void initialize_deferred_card_mark_barriers();

  // If the CollectedHeap was asked to defer a store barrier above,
  // this informs it to flush such a deferred store barrier to the
  // remembered set.
  void flush_deferred_card_mark_barrier(JavaThread* thread);

  // If a compiler is eliding store barriers for TLAB-allocated objects,
  // we will be informed of a slow-path allocation by a call
  // to on_slowpath_allocation_exit() below. Such a call precedes the
  // initialization of the object itself, and no post-store-barriers will
  // be issued. Some heap types require that the barrier strictly follows
  // the initializing stores. (This is currently implemented by deferring the
  // barrier until the next slow-path allocation or gc-related safepoint.)
  // This interface answers whether a particular barrier type needs the card
  // mark to be thus strictly sequenced after the stores.
  virtual bool card_mark_must_follow_store() const;

  virtual void on_slowpath_allocation_exit(JavaThread* thread, oop new_obj);
  virtual void on_thread_detach(Thread* thread);

  virtual void make_parsable(JavaThread* thread) { flush_deferred_card_mark_barrier(thread); }

  virtual void print_on(outputStream* st) const;

  template <DecoratorSet decorators, typename BarrierSetT = CardTableBarrierSet>
  class AccessBarrier: public ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT> {};
};

template<>
struct BarrierSet::GetName<CardTableBarrierSet> {
  static const BarrierSet::Name value = BarrierSet::CardTableBarrierSet;
};

template<>
struct BarrierSet::GetType<BarrierSet::CardTableBarrierSet> {
  typedef ::CardTableBarrierSet type;
};

#endif // SHARE_GC_SHARED_CARDTABLEBARRIERSET_HPP
