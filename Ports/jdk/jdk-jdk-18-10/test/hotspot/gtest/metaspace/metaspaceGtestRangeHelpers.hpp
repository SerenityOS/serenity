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

#ifndef GTEST_METASPACE_METASPACEGTESTRANGEHELPERS_HPP
#define GTEST_METASPACE_METASPACEGTESTRANGEHELPERS_HPP

// We use ranges-of-things in these tests a lot so some helpers help
// keeping the code small.

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "runtime/os.hpp" // For os::random
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

using metaspace::chunklevel_t;
using namespace metaspace::chunklevel;

// A range of numerical values.
template <typename T, typename Td>
class Range : public StackObj {

  // start and size of range
  T   _start;
  Td  _size;

  static Td random_uncapped_offset() {
    if (sizeof(Td) > 4) {
      return (Td)((uint64_t)os::random() * os::random());
    } else {
      return (Td)os::random();
    }
  }

protected:

  static void swap_if_needed(T& lo, T& hi) {
    if (lo > hi) {
      T v = lo;
      lo = hi;
      hi = v;
    }
  }

public:

  // Lowest value in range
  T lowest() const      { return _start; }

  // Highest value in range (including)
  T highest() const     { return _start + (_size - 1); }

  T start() const       { return _start; }
  T end() const         { return _start + _size; }

  // Number of values in range
  Td size() const       { return _size; }

  bool is_empty() const { return size() == 0; }

  bool contains(T v) const {
    return v >= _start && v < end();
  }

  bool contains(Range<T, Td> r) const {
    return contains(r.lowest()) && contains(r.highest());
  }

  // Create a range from [start, end)
  Range(T start, T end) : _start(start), _size(end - start) {
    assert(end >= start, "start and end reversed");
  }

  // a range with a given size, starting at 0
  Range(Td size) : _start(0), _size(size) {}

  // Return a random offset
  Td random_offset() const {
    assert(!is_empty(), "Range too small");
    Td v = random_uncapped_offset() % size();
    return v;
  }

  // Return a random value within the range
  T random_value() const {
    assert(!is_empty(), "Range too small");
    T v = _start + random_offset();
    assert(contains(v), "Sanity");
    return v;
  }

  // Return the head of this range up to but excluding <split_point>
  Range<T, Td> head(Td split_point) const {
    assert(_size >= split_point, "Sanity");
    return Range<T, Td>(_start, _start + split_point);
  }

  // Return the tail of this range, starting at <split_point>
  Range<T, Td> tail(Td split_point) const {
    assert(_size > split_point, "Sanity");
    return Range<T, Td>(_start + split_point, end());
  }

  // Return a non-empty random sub range.
  Range<T, Td> random_subrange() const {
    assert(size() > 1, "Range too small");
    Td sz = MAX2((Td)1, random_offset());
    return random_sized_subrange(sz);
  }

  // Return a subrange of given size at a random start position
  Range<T, Td> random_sized_subrange(Td subrange_size) const {
    assert(subrange_size > 0 && subrange_size < _size, "invalid size");
    T start = head(_size - subrange_size).random_value();
    return Range<T, Td>(start, start + subrange_size);
  }

  //// aligned ranges ////

  bool range_is_aligned(Td alignment) const {
    return is_aligned(_size, alignment) && is_aligned(_start, alignment);
  }

  // Return a non-empty aligned random sub range.
  Range<T, Td> random_aligned_subrange(Td alignment) const {
    assert(alignment > 0, "Sanity");
    assert(range_is_aligned(alignment), "Outer range needs to be aligned"); // to keep matters simple
    assert(_size >= alignment, "Outer range too small.");
    Td sz = MAX2((Td)1, random_offset());
    sz = align_up(sz, alignment);
    return random_aligned_sized_subrange(sz, alignment);
  }

  // Return a subrange of given size at a random aligned start position
  Range<T, Td> random_aligned_sized_subrange(Td subrange_size, Td alignment) const {
    assert(alignment > 0, "Sanity");
    assert(range_is_aligned(alignment), "Outer range needs to be aligned"); // to keep matters simple
    assert(subrange_size > 0 && subrange_size <= _size &&
           is_aligned(subrange_size, alignment), "invalid subrange size");
    if (_size == subrange_size) {
      return *this;
    }
    T start = head(_size - subrange_size).random_value();
    start = align_down(start, alignment);
    return Range<T, Td>(start, start + subrange_size);
  }

};

typedef Range<int, int> IntRange;
typedef Range<size_t, size_t> SizeRange;
typedef Range<chunklevel_t, int> ChunkLevelRange;

struct ChunkLevelRanges : public AllStatic {
  static ChunkLevelRange small_chunks()  { return ChunkLevelRange(CHUNK_LEVEL_32K, CHUNK_LEVEL_1K + 1); }
  static ChunkLevelRange medium_chunks() { return ChunkLevelRange(CHUNK_LEVEL_512K, CHUNK_LEVEL_32K + 1); }
  static ChunkLevelRange large_chunks()  { return ChunkLevelRange(CHUNK_LEVEL_4M, CHUNK_LEVEL_512K + 1); }
  static ChunkLevelRange all_chunks()    { return ChunkLevelRange(CHUNK_LEVEL_4M, CHUNK_LEVEL_1K + 1); }
};

#endif // GTEST_METASPACE_METASPACEGTESTRANGEHELPERS_HPP
