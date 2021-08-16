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

#ifndef SHARE_UTILITIES_ENUMITERATOR_HPP
#define SHARE_UTILITIES_ENUMITERATOR_HPP

#include <type_traits>
#include <limits>
#include "memory/allStatic.hpp"
#include "metaprogramming/enableIf.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include "utilities/debug.hpp"

// Iteration support for enums.
//
// E is enum type, U is underlying type of E.
//
// case 1:
// enum has sequential enumerators, with E first and E last (inclusive).
//
// case 2:
// enum has sequential values, with U start and U end (exclusive).
// This can be mapped onto case 1 by casting start/(end-1).
//
// case 3:
// enum has non-sequential non-duplicate enumerators
// Iteration could be supported via array or other sequence of enumerators.
// Don't bother.
//
// case 4:
// enum has non-sequential enumerators with duplicate values
// Not clear what iteration should mean in this case.
// Don't bother trying to figure this out.
//
//
// EnumRange -- defines the range of *one specific* iteration loop.
// EnumIterator -- the current point in the iteration loop.

// Example:
//
// /* With range-base for (recommended) */
// for (auto index : EnumRange<vmSymbolID>{}) {
//    ....
// }
//
// /* Without range-based for */
// constexpr EnumRange<vmSymbolID> vmSymbolsRange{};
// using vmSymbolsIterator = EnumIterator<vmSymbolID>;
// for (vmSymbolsIterator it = vmSymbolsRange.begin(); it != vmSymbolsRange.end(); ++it) {
//  vmSymbolID index = *it; ....
// }

// EnumeratorRange is a traits type supporting iteration over the enumerators of T.
// Specializations must provide static const data members named "_start" and "_end".
// The type of _start and _end must be the underlying type of T.
// _start is the inclusive lower bound of values in the range.
// _end is the exclusive upper bound of values in the range.
// The enumerators of T must have sequential values in that range.
template<typename T> struct EnumeratorRange;

// Helper class for ENUMERATOR_RANGE and ENUMERATOR_VALUE_RANGE.
struct EnumeratorRangeImpl : AllStatic {
  template<typename T> using Underlying = std::underlying_type_t<T>;

  // T not deduced to verify argument is of expected type.
  template<typename T, typename U, ENABLE_IF(std::is_same<T, U>::value)>
  static constexpr Underlying<T> start_value(U first) {
    return static_cast<Underlying<T>>(first);
  }

  // T not deduced to verify argument is of expected type.
  template<typename T, typename U, ENABLE_IF(std::is_same<T, U>::value)>
  static constexpr Underlying<T> end_value(U last) {
    Underlying<T> value = static_cast<Underlying<T>>(last);
    assert(value < std::numeric_limits<Underlying<T>>::max(), "end value overflow");
    return static_cast<Underlying<T>>(value + 1);
  }
};

// Specialize EnumeratorRange<T>.  Start and End must be constant expressions
// whose value is convertible to the underlying type of T.  They provide the
// values of the required _start and _end members respectively.
#define ENUMERATOR_VALUE_RANGE(T, Start, End)                           \
  template<> struct EnumeratorRange<T> {                                \
    static constexpr EnumeratorRangeImpl::Underlying<T> _start{Start};  \
    static constexpr EnumeratorRangeImpl::Underlying<T> _end{End};      \
  };

// Specialize EnumeratorRange<T>.  First and Last must be constant expressions
// of type T.  They determine the values of the required _start and _end members
// respectively.  _start is the underlying value of First. _end is the underlying
// value of Last, plus one.
#define ENUMERATOR_RANGE(T, First, Last)                                \
  ENUMERATOR_VALUE_RANGE(T,                                             \
                         EnumeratorRangeImpl::start_value<T>(First),    \
                         EnumeratorRangeImpl::end_value<T>(Last));

// An internal helper class for EnumRange and EnumIterator, computing some
// additional information based on T and EnumeratorRange<T>, and performing
// or supporting various validity checks.
template<typename T>
class EnumIterationTraits : AllStatic {
  using RangeType = EnumeratorRange<T>;

public:
  // The underlying type for T.
  using Underlying = std::underlying_type_t<T>;

  // The value of the first enumerator of T.
  static constexpr Underlying _start = RangeType::_start;

  // The one-past-the-end value for T.
  static constexpr Underlying _end = RangeType::_end;

  static_assert(_start != _end, "empty range");
  static_assert(_start <= _end, "invalid range"); // <= so only one failure when ==.

  // Verify value is in [start, end].
  // The values for start and end default to _start and _end, respectively.
  // The (deduced) type V is expected to be either T or Underlying.
  template<typename V>
  static constexpr void assert_in_range(V value,
                                        V start = PrimitiveConversions::cast<V>(_start),
                                        V end = PrimitiveConversions::cast<V>(_end)) {
    assert(start <= value, "out of range");
    assert(value <= end, "out of range");
  }

  // Convert an enumerator value to the corresponding underlying type.
  static constexpr Underlying underlying_value(T value) {
    return static_cast<Underlying>(value);
  }

  // Convert a value to the corresponding enumerator.
  static constexpr T enumerator(Underlying value) {
    return static_cast<T>(value);
  }
};

template<typename T>
class EnumIterator {
  using Traits = EnumIterationTraits<T>;

  using Underlying = typename Traits::Underlying;
  Underlying _value;

  constexpr void assert_in_bounds() const {
    assert(_value < Traits::_end, "beyond the end");
  }

public:
  using EnumType = T;

  // Return a beyond-the-end iterator.
  constexpr EnumIterator() : _value(Traits::_end) {}

  // Return an iterator with the indicated value.
  constexpr explicit EnumIterator(T value) :
    _value(Traits::underlying_value(value))
  {
    Traits::assert_in_range(value);
  }

  // True if the iterators designate the same enumeration value.
  constexpr bool operator==(EnumIterator other) const {
    return _value == other._value;
  }

  // True if the iterators designate different enumeration values.
  constexpr bool operator!=(EnumIterator other) const {
    return _value != other._value;
  }

  // Return the current value.
  // precondition: this is not beyond the last enumerator.
  constexpr T operator*() const {
    assert_in_bounds();
    return Traits::enumerator(_value);
  }

  // Step this iterator to the next value.
  // precondition: this is not beyond the last enumerator.
  constexpr EnumIterator& operator++() {
    assert_in_bounds();
    ++_value;
    return *this;
  }

  // Return a copy and step this iterator to the next value.
  // precondition: this is not beyond the last enumerator.
  constexpr EnumIterator operator++(int) {
    assert_in_bounds();
    EnumIterator result = *this;
    ++_value;
    return result;
  }
};

template<typename T>
class EnumRange {
  using Traits = EnumIterationTraits<T>;
  using Underlying = typename Traits::Underlying;

  Underlying _start;
  Underlying _end;

  constexpr void assert_not_empty() const {
    assert(size() > 0, "empty range");
  }

public:
  using EnumType = T;
  using Iterator = EnumIterator<T>;

  // Default constructor gives the full range.
  constexpr EnumRange() :
    EnumRange(Traits::enumerator(Traits::_start)) {}

  // Range from start to the (exclusive) end of the enumerator range.
  constexpr explicit EnumRange(T start) :
    EnumRange(start, Traits::enumerator(Traits::_end)) {}

  // Range from start (inclusive) to end (exclusive).
  // precondition: start <= end.
  constexpr EnumRange(T start, T end) :
    _start(Traits::underlying_value(start)),
    _end(Traits::underlying_value(end))
  {
    Traits::assert_in_range(start);
    Traits::assert_in_range(end);
    assert(start <= end, "invalid range");
  }

  // Return an iterator for the start of the range.
  constexpr Iterator begin() const {
    return Iterator(Traits::enumerator(_start));
  }

  // Return an iterator for the end of the range.
  constexpr Iterator end() const {
    return Iterator(Traits::enumerator(_end));
  }

  // Return the number of enumerator values in the range.
  constexpr size_t size() const {
    return static_cast<size_t>(_end - _start); // _end is exclusive
  }

  // Return the first enumerator in the range.
  // precondition: size() > 0
  constexpr T first() const {
    assert_not_empty();
    return Traits::enumerator(_start);
  }

  // Return the last enumerator in the range.
  // precondition: size() > 0
  constexpr T last() const {
    assert_not_empty();
    return Traits::enumerator(_end - 1);
  }

  // Convert value to a zero-based index into the range [first(), last()].
  // precondition: first() <= value && value <= last()
  constexpr size_t index(T value) const {
    Traits::assert_in_range(value, first(), last());
    return static_cast<size_t>(Traits::underlying_value(value) - _start);
  }
};

#endif // SHARE_UTILITIES_ENUMITERATOR_HPP
