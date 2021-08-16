/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_COUNTERS_HPP
#define SHARE_MEMORY_METASPACE_COUNTERS_HPP

#include "metaprogramming/isSigned.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

// We seem to be counting a lot of things which makes it worthwhile to
// make helper classes for all that boilerplate coding.

// AbstractCounter counts something and asserts overflow and underflow.
template <class T>
class AbstractCounter {

  T _c;

  // Only allow unsigned values for now
  STATIC_ASSERT(IsSigned<T>::value == false);

public:

  AbstractCounter() : _c(0) {}

  T get() const           { return _c; }

  void increment() { increment_by(1); }
  void decrement() { decrement_by(1); }

  void increment_by(T v) {
#ifdef ASSERT
    T old = _c;
    assert(old + v >= old,
        "overflow (" UINT64_FORMAT "+" UINT64_FORMAT ")", (uint64_t)old, (uint64_t)v);
#endif
    _c += v;
  }

  void decrement_by(T v) {
    assert(_c >= v,
           "underflow (" UINT64_FORMAT "-" UINT64_FORMAT ")",
           (uint64_t)_c, (uint64_t)v);
    _c -= v;
  }

  void reset()                { _c = 0; }

#ifdef ASSERT
  void check(T expected) const {
    assert(_c == expected, "Counter mismatch: %d, expected: %d.",
           (int)_c, (int)expected);
    }
#endif

};

// Atomic variant of AbstractCounter.
template <class T>
class AbstractAtomicCounter {

  volatile T _c;

  // Only allow unsigned values for now
  STATIC_ASSERT(IsSigned<T>::value == false);

public:

  AbstractAtomicCounter() : _c(0) {}

  T get() const               { return _c; }

  void increment() {
    Atomic::inc(&_c);
  }

  void decrement() {
    Atomic::dec(&_c);
  }

  void increment_by(T v) {
    Atomic::add(&_c, v);
  }

  void decrement_by(T v) {
    Atomic::sub(&_c, v);
  }

#ifdef ASSERT
  void check(T expected) const {
    assert(_c == expected, "Counter mismatch: %d, expected: %d.",
           (int)_c, (int)expected);
    }
#endif

};

typedef AbstractCounter<size_t>   SizeCounter;
typedef AbstractCounter<unsigned> IntCounter;

typedef AbstractAtomicCounter<size_t> SizeAtomicCounter;

// We often count memory ranges (blocks, chunks etc).
// Make a helper class for that.
template <class T_num, class T_size>
class AbstractMemoryRangeCounter {

  AbstractCounter<T_num>  _count;
  AbstractCounter<T_size> _total_size;

public:

  void add(T_size s) {
    if(s > 0) {
      _count.increment();
      _total_size.increment_by(s);
    }
  }

  void sub(T_size s) {
    if(s > 0) {
      _count.decrement();
      _total_size.decrement_by(s);
    }
  }

  T_num count() const       { return _count.get(); }
  T_size total_size() const { return _total_size.get(); }

#ifdef ASSERT
  void check(T_num expected_count, T_size expected_size) const {
    _count.check(expected_count);
    _total_size.check(expected_size);
  }
  void check(const AbstractMemoryRangeCounter<T_num, T_size>& other) const {
    check(other.count(), other.total_size());
  }
#endif

};

typedef AbstractMemoryRangeCounter<unsigned, size_t> MemRangeCounter;

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_COUNTERS_HPP

