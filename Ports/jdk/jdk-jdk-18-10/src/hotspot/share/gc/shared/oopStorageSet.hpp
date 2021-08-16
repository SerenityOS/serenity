/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OOPSTORAGESET_HPP
#define SHARE_GC_SHARED_OOPSTORAGESET_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class OopStorage;

class OopStorageSet : public AllStatic {
  friend class OopStorageSetTest;

  // Must be updated when new OopStorages are introduced
  static const uint strong_count = 4 JVMTI_ONLY(+ 1);
  static const uint weak_count = 8 JVMTI_ONLY(+ 1) JFR_ONLY(+ 1);

  static const uint all_count = strong_count + weak_count;
  static const uint all_start = 0;
  static const uint all_end = all_start + all_count;

  static const uint strong_start = all_start;
  static const uint strong_end = strong_start + strong_count;

  static const uint weak_start = strong_end;
  static const uint weak_end = weak_start + weak_count;
  static_assert(all_end == weak_end, "invariant");

  static OopStorage* _storages[all_count];

  static void verify_initialized(uint index) NOT_DEBUG_RETURN;

  static OopStorage* get_storage(uint index);

  template<typename E>
  static OopStorage* get_storage(E id);

  // Testing support
  static void fill_strong(OopStorage* storage[strong_count]);
  static void fill_weak(OopStorage* storage[weak_count]);
  static void fill_all(OopStorage* storage[all_count]);

public:
  enum class StrongId : uint {}; // [strong_start, strong_end)
  enum class WeakId : uint {};   // [weak_start, weak_end)
  enum class Id : uint {};       // [all_start, all_end)

  // Give these access to the private start/end/count constants.
  friend struct EnumeratorRange<StrongId>;
  friend struct EnumeratorRange<WeakId>;
  friend struct EnumeratorRange<Id>;

  static OopStorage* storage(StrongId id) { return get_storage(id); }
  static OopStorage* storage(WeakId id) { return get_storage(id); }
  static OopStorage* storage(Id id) { return get_storage(id); }

  static OopStorage* create_strong(const char* name, MEMFLAGS memflags);
  static OopStorage* create_weak(const char* name, MEMFLAGS memflags);

  // Support iteration over the storage objects.
  template<typename StorageId> class Range;
  template<typename StorageId> class Iterator;

  template <typename Closure>
  static void strong_oops_do(Closure* cl);

};

ENUMERATOR_VALUE_RANGE(OopStorageSet::StrongId,
                       OopStorageSet::strong_start,
                       OopStorageSet::strong_end);

ENUMERATOR_VALUE_RANGE(OopStorageSet::WeakId,
                       OopStorageSet::weak_start,
                       OopStorageSet::weak_end);

ENUMERATOR_VALUE_RANGE(OopStorageSet::Id,
                       OopStorageSet::all_start,
                       OopStorageSet::all_end);

template<typename StorageId>
class OopStorageSet::Iterator {
  EnumIterator<StorageId> _it;

public:
  constexpr Iterator() : _it() {}
  explicit constexpr Iterator(EnumIterator<StorageId> it) : _it(it) {}

  constexpr bool operator==(Iterator other) const { return _it == other._it; }
  constexpr bool operator!=(Iterator other) const { return _it != other._it; }

  constexpr Iterator& operator++() { ++_it; return *this; }
  constexpr Iterator operator++(int) { Iterator i = *this; ++_it; return i; }

  OopStorage* operator*() const { return storage(*_it); }
  OopStorage* operator->() const { return operator*(); }
};

template<typename StorageId>
class OopStorageSet::Range {
  EnumRange<StorageId> _range;

public:
  constexpr auto begin() const { return Iterator<StorageId>(_range.begin()); }
  constexpr auto end() const { return Iterator<StorageId>(_range.end()); }
};

#endif // SHARE_GC_SHARED_OOPSTORAGESET_HPP
