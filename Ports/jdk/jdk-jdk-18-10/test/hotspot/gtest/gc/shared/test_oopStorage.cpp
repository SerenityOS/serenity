/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/workgroup.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "metaprogramming/conditional.hpp"
#include "metaprogramming/enableIf.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/align.hpp"
#include "utilities/ostream.hpp"
#include "utilities/quickSort.hpp"
#include "unittest.hpp"

// Access storage internals.
class OopStorage::TestAccess : public AllStatic {
public:
  typedef OopStorage::Block Block;
  typedef OopStorage::AllocationList AllocationList;
  typedef OopStorage::ActiveArray ActiveArray;

  static ActiveArray& active_array(const OopStorage& storage) {
    return *storage._active_array;
  }

  static AllocationList& allocation_list(OopStorage& storage) {
    return storage._allocation_list;
  }

  static const AllocationList& allocation_list(const OopStorage& storage) {
    return storage._allocation_list;
  }

  static Mutex* allocation_mutex(const OopStorage& storage) {
    return storage._allocation_mutex;
  }

  static bool reduce_deferred_updates(OopStorage& storage) {
    return storage.reduce_deferred_updates();
  }

  static bool block_is_empty(const Block& block) {
    return block.is_empty();
  }

  static bool block_is_full(const Block& block) {
    return block.is_full();
  }

  static unsigned block_allocation_count(const Block& block) {
    uintx bitmask = block.allocated_bitmask();
    unsigned count = 0;
    for ( ; bitmask != 0; bitmask >>= 1) {
      if ((bitmask & 1) != 0) {
        ++count;
      }
    }
    return count;
  }

  static size_t memory_per_block() {
    return Block::allocation_size();
  }

  static void block_array_set_block_count(ActiveArray* blocks, size_t count) {
    blocks->_block_count = count;
  }
};

typedef OopStorage::TestAccess TestAccess;

// The "Oop" prefix is to avoid collision with similar opto names when
// building with precompiled headers, or for consistency with that
// workaround.  There really should be an opto namespace.
typedef TestAccess::Block OopBlock;
typedef TestAccess::AllocationList AllocationList;
typedef TestAccess::ActiveArray ActiveArray;

// Using EXPECT_EQ can't use NULL directly. Otherwise AIX build breaks.
const OopBlock* const NULL_BLOCK = NULL;

static size_t list_length(const AllocationList& list) {
  size_t result = 0;
  for (const OopBlock* block = list.chead();
       block != NULL;
       block = list.next(*block)) {
    ++result;
  }
  return result;
}

static void clear_list(AllocationList& list) {
  OopBlock* next;
  for (OopBlock* block = list.head(); block != NULL; block = next) {
    next = list.next(*block);
    list.unlink(*block);
  }
}

static bool is_list_empty(const AllocationList& list) {
  return list.chead() == NULL;
}

static bool process_deferred_updates(OopStorage& storage) {
  MutexLocker ml(TestAccess::allocation_mutex(storage), Mutex::_no_safepoint_check_flag);
  bool result = false;
  while (TestAccess::reduce_deferred_updates(storage)) {
    result = true;
  }
  return result;
}

static void release_entry(OopStorage& storage, oop* entry, bool process_deferred = true) {
  *entry = NULL;
  storage.release(entry);
  if (process_deferred) {
    process_deferred_updates(storage);
  }
}

static size_t empty_block_count(const OopStorage& storage) {
  const AllocationList& list = TestAccess::allocation_list(storage);
  size_t count = 0;
  for (const OopBlock* block = list.ctail();
       (block != NULL) && block->is_empty();
       ++count, block = list.prev(*block))
  {}
  return count;
}

static size_t active_count(const OopStorage& storage) {
  return TestAccess::active_array(storage).block_count();
}

static OopBlock* active_head(const OopStorage& storage) {
  ActiveArray& ba = TestAccess::active_array(storage);
  size_t count = ba.block_count();
  if (count == 0) {
    return NULL;
  } else {
    return ba.at(count - 1);
  }
}

class OopStorageTest : public ::testing::Test {
public:
  OopStorageTest();
  ~OopStorageTest();

  OopStorage _storage;

  class CountingIterateClosure;
  template<bool is_const> class VM_CountAtSafepoint;
};

OopStorageTest::OopStorageTest() :
  _storage("Test Storage", mtGC)
{ }

OopStorageTest::~OopStorageTest() {
  clear_list(TestAccess::allocation_list(_storage));
}

class OopStorageTestWithAllocation : public OopStorageTest {
public:
  OopStorageTestWithAllocation();

  static const size_t _max_entries = 1000;
  oop* _entries[_max_entries];
};

OopStorageTestWithAllocation::OopStorageTestWithAllocation() {
  for (size_t i = 0; i < _max_entries; ++i) {
    _entries[i] = _storage.allocate();
    EXPECT_TRUE(_entries[i] != NULL);
    EXPECT_EQ(i + 1, _storage.allocation_count());
  }
};

const size_t OopStorageTestWithAllocation::_max_entries;

static bool is_allocation_list_sorted(const OopStorage& storage) {
  // The allocation_list isn't strictly sorted.  Rather, all empty
  // blocks are segregated to the end of the list.
  const AllocationList& list = TestAccess::allocation_list(storage);
  const OopBlock* block = list.ctail();
  for ( ; (block != NULL) && block->is_empty(); block = list.prev(*block)) {}
  for ( ; block != NULL; block = list.prev(*block)) {
    if (block->is_empty()) {
      return false;
    }
  }
  return true;
}

static size_t total_allocation_count(const OopStorage& storage) {
  size_t total_count = 0;
  const ActiveArray& ba = TestAccess::active_array(storage);
  size_t limit = active_count(storage);
  for (size_t i = 0; i < limit; ++i) {
    total_count += TestAccess::block_allocation_count(*ba.at(i));
  }
  return total_count;
}

TEST_VM_F(OopStorageTest, allocate_one) {
  EXPECT_EQ(0u, active_count(_storage));
  EXPECT_TRUE(is_list_empty(TestAccess::allocation_list(_storage)));

  oop* ptr = _storage.allocate();
  EXPECT_TRUE(ptr != NULL);
  EXPECT_EQ(1u, _storage.allocation_count());

  EXPECT_EQ(1u, active_count(_storage));
  EXPECT_EQ(1u, _storage.block_count());
  EXPECT_EQ(1u, list_length(TestAccess::allocation_list(_storage)));

  EXPECT_EQ(0u, empty_block_count(_storage));

  const OopBlock* block = TestAccess::allocation_list(_storage).chead();
  EXPECT_NE(block, (OopBlock*)NULL);
  EXPECT_EQ(block, active_head(_storage));
  EXPECT_FALSE(TestAccess::block_is_empty(*block));
  EXPECT_FALSE(TestAccess::block_is_full(*block));
  EXPECT_EQ(1u, TestAccess::block_allocation_count(*block));

  release_entry(_storage, ptr);
  EXPECT_EQ(0u, _storage.allocation_count());

  EXPECT_EQ(1u, active_count(_storage));
  EXPECT_EQ(1u, _storage.block_count());
  EXPECT_EQ(1u, list_length(TestAccess::allocation_list(_storage)));

  EXPECT_EQ(1u, empty_block_count(_storage));

  const OopBlock* new_block = TestAccess::allocation_list(_storage).chead();
  EXPECT_EQ(block, new_block);
  EXPECT_EQ(block, active_head(_storage));
  EXPECT_TRUE(TestAccess::block_is_empty(*block));
  EXPECT_FALSE(TestAccess::block_is_full(*block));
  EXPECT_EQ(0u, TestAccess::block_allocation_count(*block));
}

TEST_VM_F(OopStorageTest, allocation_count) {
  static const size_t max_entries = 1000;
  oop* entries[max_entries];

  AllocationList& allocation_list = TestAccess::allocation_list(_storage);

  EXPECT_EQ(0u, active_count(_storage));
  EXPECT_EQ(0u, _storage.block_count());
  EXPECT_TRUE(is_list_empty(allocation_list));

  size_t allocated = 0;
  for ( ; allocated < max_entries; ++allocated) {
    EXPECT_EQ(allocated, _storage.allocation_count());
    if (active_count(_storage) != 0) {
      EXPECT_EQ(1u, active_count(_storage));
      EXPECT_EQ(1u, _storage.block_count());
      const OopBlock& block = *TestAccess::active_array(_storage).at(0);
      EXPECT_EQ(allocated, TestAccess::block_allocation_count(block));
      if (TestAccess::block_is_full(block)) {
        break;
      } else {
        EXPECT_FALSE(is_list_empty(allocation_list));
        EXPECT_EQ(&block, allocation_list.chead());
      }
    }
    entries[allocated] = _storage.allocate();
  }

  EXPECT_EQ(allocated, _storage.allocation_count());
  EXPECT_EQ(1u, active_count(_storage));
  EXPECT_EQ(1u, _storage.block_count());
  EXPECT_TRUE(is_list_empty(allocation_list));
  const OopBlock& block = *TestAccess::active_array(_storage).at(0);
  EXPECT_TRUE(TestAccess::block_is_full(block));
  EXPECT_EQ(allocated, TestAccess::block_allocation_count(block));

  for (size_t i = 0; i < allocated; ++i) {
    release_entry(_storage, entries[i]);
    size_t remaining = allocated - (i + 1);
    EXPECT_EQ(remaining, TestAccess::block_allocation_count(block));
    EXPECT_EQ(remaining, _storage.allocation_count());
    EXPECT_FALSE(is_list_empty(allocation_list));
  }
}

TEST_VM_F(OopStorageTest, allocate_many) {
  static const size_t max_entries = 1000;
  oop* entries[max_entries];

  AllocationList& allocation_list = TestAccess::allocation_list(_storage);

  EXPECT_EQ(0u, empty_block_count(_storage));

  entries[0] = _storage.allocate();
  ASSERT_TRUE(entries[0] != NULL);
  EXPECT_EQ(1u, active_count(_storage));
  EXPECT_EQ(1u, _storage.block_count());
  EXPECT_EQ(1u, list_length(allocation_list));
  EXPECT_EQ(0u, empty_block_count(_storage));

  const OopBlock* block = TestAccess::active_array(_storage).at(0);
  EXPECT_EQ(1u, TestAccess::block_allocation_count(*block));
  EXPECT_EQ(block, allocation_list.chead());

  for (size_t i = 1; i < max_entries; ++i) {
    entries[i] = _storage.allocate();
    EXPECT_EQ(i + 1, _storage.allocation_count());
    ASSERT_TRUE(entries[i] != NULL);
    EXPECT_EQ(0u, empty_block_count(_storage));

    if (block == NULL) {
      ASSERT_FALSE(is_list_empty(allocation_list));
      EXPECT_EQ(1u, list_length(allocation_list));
      block = allocation_list.chead();
      EXPECT_EQ(1u, TestAccess::block_allocation_count(*block));
      EXPECT_EQ(block, active_head(_storage));
    } else if (TestAccess::block_is_full(*block)) {
      EXPECT_TRUE(is_list_empty(allocation_list));
      block = NULL;
    } else {
      EXPECT_FALSE(is_list_empty(allocation_list));
      EXPECT_EQ(block, allocation_list.chead());
      EXPECT_EQ(block, active_head(_storage));
    }
  }

  if (block != NULL) {
    EXPECT_NE(0u, TestAccess::block_allocation_count(*block));
    EXPECT_FALSE(is_list_empty(allocation_list));
    EXPECT_EQ(block, allocation_list.chead());
    EXPECT_EQ(block, active_head(_storage));
  }

  for (size_t i = 0; i < max_entries; ++i) {
    release_entry(_storage, entries[i]);
    EXPECT_TRUE(is_allocation_list_sorted(_storage));
    EXPECT_EQ(max_entries - (i + 1), total_allocation_count(_storage));
  }

  EXPECT_EQ(active_count(_storage), list_length(allocation_list));
  EXPECT_EQ(active_count(_storage), _storage.block_count());
  EXPECT_EQ(active_count(_storage), empty_block_count(_storage));
  for (const OopBlock* block = allocation_list.chead();
       block != NULL;
       block = allocation_list.next(*block)) {
    EXPECT_TRUE(TestAccess::block_is_empty(*block));
  }
}

TEST_VM_F(OopStorageTestWithAllocation, random_release) {
  static const size_t step = 11;
  ASSERT_NE(0u, _max_entries % step); // max_entries and step are mutually prime

  EXPECT_EQ(0u, empty_block_count(_storage));

  AllocationList& allocation_list = TestAccess::allocation_list(_storage);

  EXPECT_EQ(_max_entries, total_allocation_count(_storage));
  EXPECT_GE(1u, list_length(allocation_list));

  // Release all entries in "random" order.
  size_t released = 0;
  for (size_t i = 0; released < _max_entries; i = (i + step) % _max_entries) {
    if (_entries[i] != NULL) {
      release_entry(_storage, _entries[i]);
      _entries[i] = NULL;
      ++released;
      EXPECT_EQ(_max_entries - released, total_allocation_count(_storage));
      EXPECT_TRUE(is_allocation_list_sorted(_storage));
    }
  }

  EXPECT_EQ(active_count(_storage), list_length(allocation_list));
  EXPECT_EQ(active_count(_storage), _storage.block_count());
  EXPECT_EQ(0u, total_allocation_count(_storage));
  EXPECT_EQ(list_length(allocation_list), empty_block_count(_storage));
}

TEST_VM_F(OopStorageTestWithAllocation, random_allocate_release) {
  static const size_t release_step = 11;
  static const size_t allocate_step = 5;
  ASSERT_NE(0u, _max_entries % release_step); // max_entries and step are mutually prime

  EXPECT_EQ(0u, empty_block_count(_storage));

  AllocationList& allocation_list = TestAccess::allocation_list(_storage);

  EXPECT_EQ(_max_entries, total_allocation_count(_storage));
  EXPECT_GE(1u, list_length(allocation_list));

  // Release all entries in "random" order, "randomly" interspersed
  // with additional allocations.
  size_t released = 0;
  size_t total_released = 0;
  for (size_t i = 0; released < _max_entries; i = (i + release_step) % _max_entries) {
    if (_entries[i] != NULL) {
      release_entry(_storage, _entries[i]);
      _entries[i] = NULL;
      ++released;
      ++total_released;
      EXPECT_EQ(_max_entries - released, total_allocation_count(_storage));
      EXPECT_TRUE(is_allocation_list_sorted(_storage));
      if (total_released % allocate_step == 0) {
        _entries[i] = _storage.allocate();
        --released;
        EXPECT_EQ(_max_entries - released, total_allocation_count(_storage));
        EXPECT_TRUE(is_allocation_list_sorted(_storage));
      }
    }
  }

  EXPECT_EQ(active_count(_storage), list_length(allocation_list));
  EXPECT_EQ(active_count(_storage), _storage.block_count());
  EXPECT_EQ(0u, total_allocation_count(_storage));
  EXPECT_EQ(list_length(allocation_list), empty_block_count(_storage));
}

template<bool sorted>
class OopStorageTestBlockRelease : public OopStorageTestWithAllocation {
public:
  void SetUp() {
    size_t nrelease = _max_entries / 2;
    oop** to_release = NEW_C_HEAP_ARRAY(oop*, nrelease, mtInternal);

    for (size_t i = 0; i < nrelease; ++i) {
      to_release[i] = _entries[2 * i];
      *to_release[i] = NULL;
    }
    if (sorted) {
      QuickSort::sort(to_release, nrelease, PointerCompare(), false);
    }

    _storage.release(to_release, nrelease);
    EXPECT_EQ(_max_entries - nrelease, _storage.allocation_count());

    for (size_t i = 0; i < nrelease; ++i) {
      release_entry(_storage, _entries[2 * i + 1], false);
      EXPECT_EQ(_max_entries - nrelease - (i + 1), _storage.allocation_count());
    }
    EXPECT_TRUE(process_deferred_updates(_storage));

    EXPECT_EQ(_storage.block_count(), empty_block_count(_storage));

    FREE_C_HEAP_ARRAY(oop*, to_release);
  }

  struct PointerCompare {
    int operator()(const void* p, const void* q) const {
      return (p < q) ? -1 : int(p != q);
    }
  };
};

typedef OopStorageTestBlockRelease<true> OopStorageTestBlockReleaseSorted;
typedef OopStorageTestBlockRelease<false> OopStorageTestBlockReleaseUnsorted;

TEST_VM_F(OopStorageTestBlockReleaseSorted, block_release) {}
TEST_VM_F(OopStorageTestBlockReleaseUnsorted, block_release) {}

TEST_VM_F(OopStorageTest, bulk_allocation) {
  static const size_t max_entries = 1000;
  static const size_t zero = 0;
  oop* entries[max_entries] = {};

  AllocationList& allocation_list = TestAccess::allocation_list(_storage);

  EXPECT_EQ(0u, empty_block_count(_storage));
  size_t allocated = _storage.allocate(entries, max_entries);
  ASSERT_NE(allocated, zero);
  // ASSERT_LE would ODR-use the OopStorage constant.
  size_t bulk_allocate_limit = OopStorage::bulk_allocate_limit;
  ASSERT_LE(allocated, bulk_allocate_limit);
  ASSERT_LE(allocated, max_entries);
  for (size_t i = 0; i < allocated; ++i) {
    EXPECT_EQ(OopStorage::ALLOCATED_ENTRY, _storage.allocation_status(entries[i]));
  }
  for (size_t i = allocated; i < max_entries; ++i) {
    EXPECT_EQ(NULL, entries[i]);
  }
  _storage.release(entries, allocated);
  EXPECT_EQ(0u, _storage.allocation_count());
  for (size_t i = 0; i < allocated; ++i) {
    EXPECT_EQ(OopStorage::UNALLOCATED_ENTRY, _storage.allocation_status(entries[i]));
  }
}

#ifndef DISABLE_GARBAGE_ALLOCATION_STATUS_TESTS
TEST_VM_F(OopStorageTest, invalid_pointer) {
  {
    char* mem = NEW_C_HEAP_ARRAY(char, 1000, mtInternal);
    oop* ptr = reinterpret_cast<oop*>(align_down(mem + 250, sizeof(oop)));
    // Predicate returns false for some malloc'ed block.
    EXPECT_EQ(OopStorage::INVALID_ENTRY, _storage.allocation_status(ptr));
    FREE_C_HEAP_ARRAY(char, mem);
  }

  {
    oop obj;
    oop* ptr = &obj;
    // Predicate returns false for some "random" location.
    EXPECT_EQ(OopStorage::INVALID_ENTRY, _storage.allocation_status(ptr));
  }
}
#endif // DISABLE_GARBAGE_ALLOCATION_STATUS_TESTS

class OopStorageTest::CountingIterateClosure {
public:
  size_t _const_count;
  size_t _const_non_null;
  size_t _non_const_count;
  size_t _non_const_non_null;

  void do_oop(const oop* ptr) {
    ++_const_count;
    if (*ptr != NULL) {
      ++_const_non_null;
    }
  }

  void do_oop(oop* ptr) {
    ++_non_const_count;
    if (*ptr != NULL) {
      ++_non_const_non_null;
    }
  }

  CountingIterateClosure() :
    _const_count(0),
    _const_non_null(0),
    _non_const_count(0),
    _non_const_non_null(0)
  {}
};

template<bool is_const>
class OopStorageTest::VM_CountAtSafepoint : public VM_GTestExecuteAtSafepoint {
public:
  typedef typename Conditional<is_const,
                               const OopStorage,
                               OopStorage>::type Storage;

  VM_CountAtSafepoint(Storage* storage, CountingIterateClosure* cl) :
    _storage(storage), _cl(cl)
  {}

  void doit() { _storage->oops_do(_cl); }

private:
  Storage* _storage;
  CountingIterateClosure* _cl;
};

TEST_VM_F(OopStorageTest, simple_iterate) {
  // Dummy oop value.
  intptr_t dummy_oop_value = 0xbadbeaf;
  oop dummy_oop = reinterpret_cast<oopDesc*>(&dummy_oop_value);

  const size_t max_entries = 1000;
  oop* entries[max_entries];

  size_t allocated = 0;
  size_t entries_with_values = 0;
  for (size_t i = 0; i < max_entries; i += 10) {
    for ( ; allocated < i; ++allocated) {
      entries[allocated] = _storage.allocate();
      ASSERT_TRUE(entries[allocated] != NULL);
      if ((allocated % 3) != 0) {
        *entries[allocated] = dummy_oop;
        ++entries_with_values;
      }
    }

    {
      CountingIterateClosure cl;
      VM_CountAtSafepoint<false> op(&_storage, &cl);
      {
        ThreadInVMfromNative invm(JavaThread::current());
        VMThread::execute(&op);
      }
      EXPECT_EQ(allocated, cl._non_const_count);
      EXPECT_EQ(entries_with_values, cl._non_const_non_null);
      EXPECT_EQ(0u, cl._const_count);
      EXPECT_EQ(0u, cl._const_non_null);
    }

    {
      CountingIterateClosure cl;
      VM_CountAtSafepoint<true> op(&_storage, &cl);
      {
        ThreadInVMfromNative invm(JavaThread::current());
        VMThread::execute(&op);
      }
      EXPECT_EQ(allocated, cl._const_count);
      EXPECT_EQ(entries_with_values, cl._const_non_null);
      EXPECT_EQ(0u, cl._non_const_count);
      EXPECT_EQ(0u, cl._non_const_non_null);
    }
  }

  while (allocated > 0) {
    release_entry(_storage, entries[--allocated], false);
  }
  process_deferred_updates(_storage);
}

class OopStorageTestIteration : public OopStorageTestWithAllocation {
public:
  static const size_t _max_workers = 2;
  unsigned char _states[_max_workers][_max_entries];

  static const unsigned char mark_released  = 1u << 0;
  static const unsigned char mark_invalid   = 1u << 1;
  static const unsigned char mark_const     = 1u << 2;
  static const unsigned char mark_non_const = 1u << 3;

  virtual void SetUp() {
    OopStorageTestWithAllocation::SetUp();

    memset(_states, 0, sizeof(_states));

    size_t initial_release = 0;
    for ( ; empty_block_count(_storage) < 2; ++initial_release) {
      ASSERT_GT(_max_entries, initial_release);
      release_entry(_storage, _entries[initial_release]);
      _states[0][initial_release] = mark_released;
    }

    for (size_t i = initial_release; i < _max_entries; i += 3) {
      release_entry(_storage, _entries[i], false);
      _states[0][i] = mark_released;
    }
    process_deferred_updates(_storage);
  }

  class VerifyState;
  class VerifyFn;
  template<bool is_const> class VM_Verify;

  class VerifyClosure;
  class VM_VerifyUsingOopsDo;
};

const unsigned char OopStorageTestIteration::mark_released;
const unsigned char OopStorageTestIteration::mark_invalid;
const unsigned char OopStorageTestIteration::mark_const;
const unsigned char OopStorageTestIteration::mark_non_const;

class OopStorageTestIteration::VerifyState {
public:
  unsigned char _expected_mark;
  const oop* const* _entries;
  unsigned char (&_states)[_max_workers][_max_entries];

  VerifyState(unsigned char expected_mark,
              const oop* const* entries,
              unsigned char (&states)[_max_workers][_max_entries]) :
    _expected_mark(expected_mark),
    _entries(entries),
    _states(states)
  { }

  bool update(const oop* ptr, uint worker_id, unsigned char mark) const {
    size_t index = 0;
    bool found = find_entry(ptr, &index);
    EXPECT_TRUE(found);
    EXPECT_GT(_max_entries, index);
    EXPECT_GT(_max_workers, worker_id);
    if (!found) {
      return false;
    } else if (index >= _max_entries) {
      return false;
    } else if (worker_id >= _max_workers) {
      return false;
    } else {
      EXPECT_EQ(0, _states[worker_id][index]);
      if (_states[worker_id][index] != 0) {
        _states[worker_id][index] |= mark_invalid;
        return false;
      } else {
        _states[worker_id][index] |= mark;
        return true;
      }
    }
  }

  void check() const {
    for (size_t i = 0; i < _max_entries; ++i) {
      unsigned char mark = 0;
      for (size_t w = 0; w < _max_workers; ++w) {
        if (mark == 0) {
          mark = _states[w][i];
        } else {
          EXPECT_EQ(0u, _states[w][i]);
        }
      }
      if (mark == 0) {
        EXPECT_NE(0u, mark);
      } else if ((mark & mark_released) != 0) {
        EXPECT_EQ(mark_released, mark);
      } else {
        EXPECT_EQ(_expected_mark, mark);
      }
    }
  }

private:
  bool find_entry(const oop* ptr, size_t* index) const {
    for (size_t i = 0; i < _max_entries; ++i) {
      if (ptr == _entries[i]) {
        *index = i;
        return true;
      }
    }
    return false;
  }
};

class OopStorageTestIteration::VerifyFn {
public:
  VerifyFn(VerifyState* state, uint worker_id = 0) :
    _state(state),
    _worker_id(worker_id)
  {}

  bool operator()(      oop* ptr) const {
    return _state->update(ptr, _worker_id, mark_non_const);
  }

  bool operator()(const oop* ptr) const {
    return _state->update(ptr, _worker_id, mark_const);
  }

private:
  VerifyState* _state;
  uint _worker_id;
};

class OopStorageTestIteration::VerifyClosure {
public:
  VerifyClosure(VerifyState* state, uint worker_id = 0) :
    _state(state),
    _worker_id(worker_id)
  {}

  void do_oop(oop* ptr) {
    _state->update(ptr, _worker_id, mark_non_const);
  }

  void do_oop(const oop* ptr) {
    _state->update(ptr, _worker_id, mark_const);
  }

private:
  VerifyState* _state;
  uint _worker_id;
};

const size_t OopStorageTestIteration::_max_workers;

template<bool is_const>
class OopStorageTestIteration::VM_Verify : public VM_GTestExecuteAtSafepoint {
public:
  typedef typename Conditional<is_const,
                               const OopStorage,
                               OopStorage>::type Storage;

  VM_Verify(Storage* storage, VerifyState* vstate) :
    _storage(storage), _vstate(vstate), _result(false)
  {}

  void doit() {
    VerifyFn verifier(_vstate);
    _result = _storage->iterate_safepoint(verifier);
  }

  bool result() const { return _result; }

private:
  Storage* _storage;
  VerifyState* _vstate;
  bool _result;
};

class OopStorageTestIteration::VM_VerifyUsingOopsDo : public VM_GTestExecuteAtSafepoint {
public:
  VM_VerifyUsingOopsDo(OopStorage* storage, VerifyState* vstate) :
    _storage(storage), _vstate(vstate)
  {}

  void doit() {
    VerifyClosure verifier(_vstate);
    _storage->oops_do(&verifier);
  }

private:
  OopStorage* _storage;
  VerifyState* _vstate;
};

TEST_VM_F(OopStorageTestIteration, iterate_safepoint) {
  VerifyState vstate(mark_non_const, _entries, _states);
  VM_Verify<false> op(&_storage, &vstate);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  EXPECT_TRUE(op.result());
  vstate.check();
}

TEST_VM_F(OopStorageTestIteration, const_iterate_safepoint) {
  VerifyState vstate(mark_const, _entries, _states);
  VM_Verify<true> op(&_storage, &vstate);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  EXPECT_TRUE(op.result());
  vstate.check();
}

TEST_VM_F(OopStorageTestIteration, oops_do) {
  VerifyState vstate(mark_non_const, _entries, _states);
  VM_VerifyUsingOopsDo op(&_storage, &vstate);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  vstate.check();
}

class OopStorageTestParIteration : public OopStorageTestIteration {
public:
  WorkGang* workers();

  class VM_ParStateVerify;

  template<bool concurrent, bool is_const> class Task;
  template<bool concurrent, bool is_const> class TaskUsingOopsDo;

private:
  static WorkGang* _workers;
};

WorkGang* OopStorageTestParIteration::_workers = NULL;

WorkGang* OopStorageTestParIteration::workers() {
  if (_workers == NULL) {
    _workers = new WorkGang("OopStorageTestParIteration workers",
                            _max_workers,
                            false,
                            false);
    _workers->initialize_workers();
    _workers->update_active_workers(_max_workers);
  }
  return _workers;
}

template<bool concurrent, bool is_const>
class OopStorageTestParIteration::Task : public AbstractGangTask {
  typedef OopStorage::ParState<concurrent, is_const> StateType;

  typedef typename Conditional<is_const,
                               const OopStorage,
                               OopStorage>::type Storage;

public:
  Task(const char* name, Storage* storage, VerifyState* vstate) :
    AbstractGangTask(name),
    _state(storage),
    _vstate(vstate)
  {}

  virtual void work(uint worker_id) {
    VerifyFn verifier(_vstate, worker_id);
    _state.iterate(verifier);
  }

private:
  StateType _state;
  VerifyState* _vstate;
};

template<bool concurrent, bool is_const>
class OopStorageTestParIteration::TaskUsingOopsDo : public AbstractGangTask {
public:
  TaskUsingOopsDo(const char* name, OopStorage* storage, VerifyState* vstate) :
    AbstractGangTask(name),
    _state(storage),
    _vstate(vstate)
  {}

  virtual void work(uint worker_id) {
    VerifyClosure verifier(_vstate, worker_id);
    _state.oops_do(&verifier);
  }

private:
  OopStorage::ParState<concurrent, is_const> _state;
  VerifyState* _vstate;
};

class OopStorageTestParIteration::VM_ParStateVerify : public VM_GTestExecuteAtSafepoint {
public:
  VM_ParStateVerify(WorkGang* workers, AbstractGangTask* task) :
    _workers(workers), _task(task)
  {}

  void doit() {
    _workers->run_task(_task);
  }

private:
  WorkGang* _workers;
  AbstractGangTask* _task;
};

TEST_VM_F(OopStorageTestParIteration, par_state_safepoint_iterate) {
  VerifyState vstate(mark_non_const, _entries, _states);
  Task<false, false> task("test", &_storage, &vstate);
  VM_ParStateVerify op(workers(), &task);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_safepoint_const_iterate) {
  VerifyState vstate(mark_const, _entries, _states);
  Task<false, true> task("test", &_storage, &vstate);
  VM_ParStateVerify op(workers(), &task);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_safepoint_oops_do) {
  VerifyState vstate(mark_non_const, _entries, _states);
  TaskUsingOopsDo<false, false> task("test", &_storage, &vstate);
  VM_ParStateVerify op(workers(), &task);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_safepoint_const_oops_do) {
  VerifyState vstate(mark_const, _entries, _states);
  TaskUsingOopsDo<false, true> task("test", &_storage, &vstate);
  VM_ParStateVerify op(workers(), &task);
  {
    ThreadInVMfromNative invm(JavaThread::current());
    VMThread::execute(&op);
  }
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_concurrent_iterate) {
  VerifyState vstate(mark_non_const, _entries, _states);
  Task<true, false> task("test", &_storage, &vstate);
  workers()->run_task(&task);
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_concurrent_const_iterate) {
  VerifyState vstate(mark_const, _entries, _states);
  Task<true, true> task("test", &_storage, &vstate);
  workers()->run_task(&task);
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_concurrent_oops_do) {
  VerifyState vstate(mark_non_const, _entries, _states);
  TaskUsingOopsDo<true, false> task("test", &_storage, &vstate);
  workers()->run_task(&task);
  vstate.check();
}

TEST_VM_F(OopStorageTestParIteration, par_state_concurrent_const_oops_do) {
  VerifyState vstate(mark_const, _entries, _states);
  TaskUsingOopsDo<true, true> task("test", &_storage, &vstate);
  workers()->run_task(&task);
  vstate.check();
}

TEST_VM_F(OopStorageTestWithAllocation, delete_empty_blocks) {
  size_t initial_active_size = active_count(_storage);
  EXPECT_EQ(initial_active_size, _storage.block_count());
  ASSERT_LE(3u, initial_active_size); // Need at least 3 blocks for test

  for (size_t i = 0; empty_block_count(_storage) < 3; ++i) {
    ASSERT_GT(_max_entries, i);
    release_entry(_storage, _entries[i]);
  }

  EXPECT_EQ(initial_active_size, active_count(_storage));
  EXPECT_EQ(initial_active_size, _storage.block_count());
  EXPECT_EQ(3u, empty_block_count(_storage));
  {
    ThreadInVMfromNative invm(JavaThread::current());
    while (_storage.delete_empty_blocks()) {}
  }
  EXPECT_EQ(0u, empty_block_count(_storage));
  EXPECT_EQ(initial_active_size - 3, active_count(_storage));
  EXPECT_EQ(initial_active_size - 3, _storage.block_count());
}

TEST_VM_F(OopStorageTestWithAllocation, allocation_status) {
  oop* retained = _entries[200];
  oop* released = _entries[300];
  oop* garbage = reinterpret_cast<oop*>(1024 * 1024);
  release_entry(_storage, released);

  EXPECT_EQ(OopStorage::ALLOCATED_ENTRY, _storage.allocation_status(retained));
  EXPECT_EQ(OopStorage::UNALLOCATED_ENTRY, _storage.allocation_status(released));
  EXPECT_EQ(OopStorage::INVALID_ENTRY, _storage.allocation_status(garbage));

  for (size_t i = 0; i < _max_entries; ++i) {
    if ((_entries[i] != retained) && (_entries[i] != released)) {
      // Leave deferred release updates to block deletion.
      release_entry(_storage, _entries[i], false);
    }
  }

  {
    ThreadInVMfromNative invm(JavaThread::current());
    while (_storage.delete_empty_blocks()) {}
  }
  EXPECT_EQ(OopStorage::ALLOCATED_ENTRY, _storage.allocation_status(retained));
  EXPECT_EQ(OopStorage::INVALID_ENTRY, _storage.allocation_status(released));
  EXPECT_EQ(OopStorage::INVALID_ENTRY, _storage.allocation_status(garbage));
}

TEST_VM_F(OopStorageTest, usage_info) {
  size_t goal_blocks = 5;
  oop* entries[1000];
  size_t allocated = 0;

  EXPECT_EQ(0u, _storage.block_count());
  // There is non-block overhead, so always some usage.
  EXPECT_LT(0u, _storage.total_memory_usage());

  while (_storage.block_count() < goal_blocks) {
    size_t this_count = _storage.block_count();
    while (_storage.block_count() == this_count) {
      ASSERT_GT(ARRAY_SIZE(entries), allocated);
      entries[allocated] = _storage.allocate();
      ASSERT_TRUE(entries[allocated] != NULL);
      ++allocated;
    }
    EXPECT_NE(0u, _storage.block_count());
    EXPECT_NE(0u, _storage.total_memory_usage());
  }

  EXPECT_LT(TestAccess::memory_per_block() * _storage.block_count(),
            _storage.total_memory_usage());
}

#ifndef PRODUCT

TEST_VM_F(OopStorageTestWithAllocation, print_storage) {
  // Release the first 1/2
  for (size_t i = 0; i < (_max_entries / 2); ++i) {
    // Deferred updates don't affect print output.
    release_entry(_storage, _entries[i], false);
    _entries[i] = NULL;
  }
  // Release every other remaining
  for (size_t i = _max_entries / 2; i < _max_entries; i += 2) {
    // Deferred updates don't affect print output.
    release_entry(_storage, _entries[i], false);
    _entries[i] = NULL;
  }

  size_t expected_entries = _max_entries / 4;
  EXPECT_EQ(expected_entries, _storage.allocation_count());

  size_t entries_per_block = BitsPerWord;
  size_t expected_blocks = (_max_entries + entries_per_block - 1) / entries_per_block;
  EXPECT_EQ(expected_blocks, _storage.block_count());

  double expected_usage = (100.0 * expected_entries) / (expected_blocks * entries_per_block);

  {
    ResourceMark rm;
    stringStream expected_st;
    expected_st.print("Test Storage: " SIZE_FORMAT
                      " entries in " SIZE_FORMAT
                      " blocks (%.F%%), " SIZE_FORMAT " bytes",
                      expected_entries,
                      expected_blocks,
                      expected_usage,
                      _storage.total_memory_usage());
    stringStream st;
    _storage.print_on(&st);
    EXPECT_STREQ(expected_st.as_string(), st.as_string());
  }
}

#endif // !PRODUCT

class OopStorageBlockCollectionTest : public ::testing::Test {
protected:
  OopStorageBlockCollectionTest() {
    for (size_t i = 0; i < nvalues; ++i) {
      values[i] = OopBlock::new_block(pseudo_owner());
    }
  }

  ~OopStorageBlockCollectionTest() {
    for (size_t i = 0; i < nvalues; ++i) {
      OopBlock::delete_block(*values[i]);
    }
  }

public:
  static const size_t nvalues = 10;
  OopBlock* values[nvalues];

private:
  // The only thing we actually care about is the address of the owner.
  static const size_t pseudo_owner_size = sizeof(OopStorage) / sizeof(void*);
  static const void* const _pseudo_owner[pseudo_owner_size];
  static const OopStorage* pseudo_owner() {
    return reinterpret_cast<const OopStorage*>(&_pseudo_owner);
  }
};

const size_t OopStorageBlockCollectionTest::nvalues;
const void* const OopStorageBlockCollectionTest::_pseudo_owner[] = {};

class OopStorageAllocationListTest : public OopStorageBlockCollectionTest {};

TEST_F(OopStorageAllocationListTest, empty_list) {
  AllocationList list;

  EXPECT_TRUE(is_list_empty(list));
  EXPECT_EQ(NULL_BLOCK, list.head());
  EXPECT_EQ(NULL_BLOCK, list.chead());
  EXPECT_EQ(NULL_BLOCK, list.ctail());
}

TEST_F(OopStorageAllocationListTest, push_back) {
  AllocationList list;

  for (size_t i = 0; i < nvalues; ++i) {
    list.push_back(*values[i]);
    EXPECT_FALSE(is_list_empty(list));
    EXPECT_EQ(list.ctail(), values[i]);
  }

  EXPECT_EQ(list.chead(), list.head());
  EXPECT_EQ(list.chead(), values[0]);
  EXPECT_EQ(list.ctail(), values[nvalues - 1]);

  const OopBlock* block = list.chead();
  for (size_t i = 0; i < nvalues; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.next(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);

  block = list.ctail();
  for (size_t i = 0; i < nvalues; ++i) {
    EXPECT_EQ(block, values[nvalues - i - 1]);
    block = list.prev(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);

  clear_list(list);
}

TEST_F(OopStorageAllocationListTest, push_front) {
  AllocationList list;

  for (size_t i = 0; i < nvalues; ++i) {
    list.push_front(*values[i]);
    EXPECT_FALSE(is_list_empty(list));
    EXPECT_EQ(list.head(), values[i]);
  }

  EXPECT_EQ(list.chead(), list.head());
  EXPECT_EQ(list.chead(), values[nvalues - 1]);
  EXPECT_EQ(list.ctail(), values[0]);

  const OopBlock* block = list.chead();
  for (size_t i = 0; i < nvalues; ++i) {
    EXPECT_EQ(block, values[nvalues - i - 1]);
    block = list.next(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);

  block = list.ctail();
  for (size_t i = 0; i < nvalues; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.prev(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);

  clear_list(list);
}

class OopStorageAllocationListTestWithList : public OopStorageAllocationListTest {
public:
  OopStorageAllocationListTestWithList() : list() {
    for (size_t i = 0; i < nvalues; ++i) {
      list.push_back(*values[i]);
    }
  }

  ~OopStorageAllocationListTestWithList() {
    clear_list(list);
  }

  AllocationList list;
};

TEST_F(OopStorageAllocationListTestWithList, unlink_front) {
  EXPECT_EQ(list.chead(), values[0]);
  EXPECT_EQ(list.ctail(), values[nvalues - 1]);

  list.unlink(*values[0]);
  EXPECT_EQ(NULL_BLOCK, list.next(*values[0]));
  EXPECT_EQ(NULL_BLOCK, list.prev(*values[0]));
  EXPECT_EQ(list.chead(), values[1]);
  EXPECT_EQ(list.ctail(), values[nvalues - 1]);

  const OopBlock* block = list.chead();
  for (size_t i = 1; i < nvalues; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.next(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);
}

TEST_F(OopStorageAllocationListTestWithList, unlink_back) {
  EXPECT_EQ(list.chead(), values[0]);

  list.unlink(*values[nvalues - 1]);
  EXPECT_EQ(NULL_BLOCK, list.next(*values[nvalues - 1]));
  EXPECT_EQ(NULL_BLOCK, list.prev(*values[nvalues - 1]));
  EXPECT_EQ(list.chead(), values[0]);
  EXPECT_EQ(list.ctail(), values[nvalues - 2]);

  const OopBlock* block = list.chead();
  for (size_t i = 0; i < nvalues - 1; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.next(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);
}

TEST_F(OopStorageAllocationListTestWithList, unlink_middle) {
  EXPECT_EQ(list.chead(), values[0]);

  size_t index = nvalues / 2;

  list.unlink(*values[index]);
  EXPECT_EQ(NULL_BLOCK, list.next(*values[index]));
  EXPECT_EQ(NULL_BLOCK, list.prev(*values[index]));
  EXPECT_EQ(list.chead(), values[0]);
  EXPECT_EQ(list.ctail(), values[nvalues - 1]);

  const OopBlock* block = list.chead();
  for (size_t i = 0; i < index; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.next(*block);
  }
  for (size_t i = index + 1; i < nvalues; ++i) {
    EXPECT_EQ(block, values[i]);
    block = list.next(*block);
  }
  EXPECT_EQ(NULL_BLOCK, block);
}

TEST_F(OopStorageAllocationListTest, single) {
  AllocationList list;

  list.push_back(*values[0]);
  EXPECT_EQ(NULL_BLOCK, list.next(*values[0]));
  EXPECT_EQ(NULL_BLOCK, list.prev(*values[0]));
  EXPECT_EQ(list.chead(), values[0]);
  EXPECT_EQ(list.ctail(), values[0]);

  list.unlink(*values[0]);
  EXPECT_EQ(NULL_BLOCK, list.next(*values[0]));
  EXPECT_EQ(NULL_BLOCK, list.prev(*values[0]));
  EXPECT_EQ(NULL_BLOCK, list.chead());
  EXPECT_EQ(NULL_BLOCK, list.ctail());
}

class OopStorageActiveArrayTest : public OopStorageBlockCollectionTest {};

TEST_F(OopStorageActiveArrayTest, empty_array) {
  ActiveArray* a = ActiveArray::create(nvalues);

  EXPECT_EQ(nvalues, a->size());
  EXPECT_EQ(0u, a->block_count_acquire());
  TestAccess::block_array_set_block_count(a, 2);
  EXPECT_EQ(2u, a->block_count_acquire());
  TestAccess::block_array_set_block_count(a, 0);
  a->increment_refcount();
  a->increment_refcount();
  EXPECT_FALSE(a->decrement_refcount());
  EXPECT_TRUE(a->decrement_refcount());

  ActiveArray::destroy(a);
}

TEST_F(OopStorageActiveArrayTest, push) {
  ActiveArray* a = ActiveArray::create(nvalues - 1);

  for (size_t i = 0; i < nvalues - 1; ++i) {
    EXPECT_TRUE(a->push(values[i]));
    EXPECT_EQ(i + 1, a->block_count_acquire());
    EXPECT_EQ(values[i], a->at(i));
  }
  EXPECT_FALSE(a->push(values[nvalues - 1]));

  TestAccess::block_array_set_block_count(a, 0);
  ActiveArray::destroy(a);
}

class OopStorageActiveArrayTestWithArray : public OopStorageActiveArrayTest {
public:
  OopStorageActiveArrayTestWithArray() : a(ActiveArray::create(nvalues)) {
    for (size_t i = 0; i < nvalues; ++i) {
      a->push(values[i]);
    }
  }

  ~OopStorageActiveArrayTestWithArray() {
    TestAccess::block_array_set_block_count(a, 0);
    ActiveArray::destroy(a);
  }

  ActiveArray* a;
};

TEST_F(OopStorageActiveArrayTestWithArray, remove0) {
  a->remove(values[0]);
  EXPECT_EQ(nvalues - 1, a->block_count_acquire());
  EXPECT_EQ(values[nvalues - 1], a->at(0));
  for (size_t i = 1; i < nvalues - 1; ++i) {
    EXPECT_EQ(values[i], a->at(i));
  }
}

TEST_F(OopStorageActiveArrayTestWithArray, remove3) {
  a->remove(values[3]);
  EXPECT_EQ(nvalues - 1, a->block_count_acquire());
  for (size_t i = 0; i < 3; ++i) {
    EXPECT_EQ(values[i], a->at(i));
  }
  EXPECT_EQ(values[nvalues - 1], a->at(3));
  for (size_t i = 4; i < nvalues - 1; ++i) {
    EXPECT_EQ(values[i], a->at(i));
  }
}

TEST_F(OopStorageActiveArrayTestWithArray, remove_last) {
  a->remove(values[nvalues - 1]);
  EXPECT_EQ(nvalues - 1, a->block_count_acquire());
  for (size_t i = 0; i < nvalues - 1; ++i) {
    EXPECT_EQ(values[i], a->at(i));
  }
}
