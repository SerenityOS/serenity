/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OOPSTORAGEPARSTATE_INLINE_HPP
#define SHARE_GC_SHARED_OOPSTORAGEPARSTATE_INLINE_HPP

#include "gc/shared/oopStorageParState.hpp"

#include "gc/shared/oopStorage.inline.hpp"
#include "metaprogramming/conditional.hpp"
#include "utilities/macros.hpp"

template<typename F>
class OopStorage::BasicParState::AlwaysTrueFn {
  F _f;

public:
  AlwaysTrueFn(F f) : _f(f) {}

  template<typename OopPtr>     // [const] oop*
  bool operator()(OopPtr ptr) const { _f(ptr); return true; }
};

struct OopStorage::BasicParState::IterationData {
  size_t _segment_start;
  size_t _segment_end;
  size_t _processed;
};

template<bool is_const, typename F>
inline void OopStorage::BasicParState::iterate(F f) {
  // Wrap f in ATF so we can use Block::iterate.
  AlwaysTrueFn<F> atf_f(f);
  IterationData data = {};      // zero initialize.
  while (claim_next_segment(&data)) {
    assert(data._segment_start < data._segment_end, "invariant");
    assert(data._segment_end <= _block_count, "invariant");
    typedef typename Conditional<is_const, const Block*, Block*>::type BlockPtr;
    size_t i = data._segment_start;
    do {
      BlockPtr block = _active_array->at(i);
      block->iterate(atf_f);
    } while (++i < data._segment_end);
  }
}

template<bool concurrent, bool is_const>
template<typename F>
inline void OopStorage::ParState<concurrent, is_const>::iterate(F f) {
  _basic_state.template iterate<is_const>(f);
}

template<bool concurrent, bool is_const>
template<typename Closure>
inline void OopStorage::ParState<concurrent, is_const>::oops_do(Closure* cl) {
  this->iterate(oop_fn(cl));
}

template<typename F>
inline void OopStorage::ParState<false, false>::iterate(F f) {
  _basic_state.template iterate<false>(f);
}

template<typename Closure>
inline void OopStorage::ParState<false, false>::oops_do(Closure* cl) {
  this->iterate(oop_fn(cl));
}

template<typename Closure>
inline void OopStorage::ParState<false, false>::weak_oops_do(Closure* cl) {
  this->iterate(skip_null_fn(oop_fn(cl)));
}

template<typename IsAliveClosure, typename Closure>
inline void OopStorage::ParState<false, false>::weak_oops_do(IsAliveClosure* is_alive, Closure* cl) {
  this->iterate(if_alive_fn(is_alive, oop_fn(cl)));
}

#endif // SHARE_GC_SHARED_OOPSTORAGEPARSTATE_INLINE_HPP
