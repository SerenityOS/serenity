/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_INLINE_HPP
#define SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_INLINE_HPP

#include "gc/shared/oopStorageSetParState.hpp"

#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "memory/iterator.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"

template <bool concurrent, bool is_const>
template <typename Closure>
void OopStorageSetStrongParState<concurrent, is_const>::oops_do(Closure* cl) {
  for (auto id : EnumRange<OopStorageSet::StrongId>()) {
    this->par_state(id)->oops_do(cl);
  }
}

template <typename ClosureType>
class DeadCounterClosure : public OopClosure {
private:
  ClosureType* const _cl;
  size_t             _num_dead;

public:
  DeadCounterClosure(ClosureType* cl) :
      _cl(cl),
      _num_dead(0) {}

  virtual void do_oop(oop* p) {
    _cl->do_oop(p);
    if (Atomic::load(p) == NULL) {
      _num_dead++;              // Count both already NULL and cleared by closure.
    }
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  size_t num_dead() const {
    return _num_dead;
  }
};

template <bool concurrent, bool is_const>
template <typename Closure>
void OopStorageSetWeakParState<concurrent, is_const>::oops_do(Closure* cl) {
  for (auto id : EnumRange<OopStorageSet::WeakId>()) {
    auto state = this->par_state(id);
    if (state->storage()->should_report_num_dead()) {
      DeadCounterClosure<Closure> counting_cl(cl);
      state->oops_do(&counting_cl);
      state->increment_num_dead(counting_cl.num_dead());
    } else {
      state->oops_do(cl);
    }
  }
}

template <bool concurrent, bool is_const>
void OopStorageSetWeakParState<concurrent, is_const>::report_num_dead() {
  for (auto id : EnumRange<OopStorageSet::WeakId>()) {
    auto state = this->par_state(id);
    state->storage()->report_num_dead(state->num_dead());
  }
}

#endif // SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_INLINE_HPP
