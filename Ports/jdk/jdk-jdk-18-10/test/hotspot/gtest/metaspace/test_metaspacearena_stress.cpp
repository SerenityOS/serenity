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

#include "precompiled.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/metaspaceArena.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"
#include "metaspaceGtestSparseArray.hpp"

using metaspace::ArenaGrowthPolicy;
using metaspace::ChunkManager;
using metaspace::IntCounter;
using metaspace::MemRangeCounter;
using metaspace::MetaspaceArena;
using metaspace::SizeAtomicCounter;
using metaspace::ArenaStats;
using metaspace::InUseChunkStats;

// Little randomness helper
static bool fifty_fifty() {
  return IntRange(100).random_value() < 50;
}

// See metaspaceArena.cpp : needed for predicting commit sizes.
namespace metaspace {
  extern size_t get_raw_word_size_for_requested_word_size(size_t net_word_size);
}

// A MetaspaceArenaTestBed contains a single MetaspaceArena and its lock.
// It keeps track of allocations done from this MetaspaceArena.
class MetaspaceArenaTestBed : public CHeapObj<mtInternal> {

  MetaspaceArena* _arena;

  Mutex* _lock;

  const SizeRange _allocation_range;
  size_t _size_of_last_failed_allocation;

  // We keep track of all allocations done thru the MetaspaceArena to
  // later check for overwriters.
  struct allocation_t {
    allocation_t* next;
    MetaWord* p; // NULL if deallocated
    size_t word_size;
    void mark() {
      mark_range(p, word_size);
    }
    void verify() const {
      if (p != NULL) {
        check_marked_range(p, word_size);
      }
    }
  };

  allocation_t* _allocations;

  // We count how much we did allocate and deallocate
  MemRangeCounter _alloc_count;
  MemRangeCounter _dealloc_count;

  // Check statistics returned by MetaspaceArena::add_to_statistics() against what
  // we know we allocated. This is a bit flaky since MetaspaceArena has internal
  // overhead.
  void verify_arena_statistics() const {

    ArenaStats stats;
    _arena->add_to_statistics(&stats);
    InUseChunkStats in_use_stats = stats.totals();

    assert(_dealloc_count.total_size() <= _alloc_count.total_size() &&
           _dealloc_count.count() <= _alloc_count.count(), "Sanity");

    // Check consistency of stats
    ASSERT_GE(in_use_stats._word_size, in_use_stats._committed_words);
    ASSERT_EQ(in_use_stats._committed_words,
              in_use_stats._used_words + in_use_stats._free_words + in_use_stats._waste_words);
    ASSERT_GE(in_use_stats._used_words, stats._free_blocks_word_size);

    // Note: reasons why the outside alloc counter and the inside used counter can differ:
    // - alignment/padding of allocations
    // - inside used counter contains blocks in free list
    // - free block list splinter threshold

    // Since what we deallocated may have been given back to us in a following allocation,
    // we only know fore sure we allocated what we did not give back.
    const size_t at_least_allocated = _alloc_count.total_size() - _dealloc_count.total_size();

    // At most we allocated this:
    const size_t max_word_overhead_per_alloc = 4;
    const size_t at_most_allocated = _alloc_count.total_size() + max_word_overhead_per_alloc * _alloc_count.count();

    ASSERT_LE(at_least_allocated, in_use_stats._used_words - stats._free_blocks_word_size);
    ASSERT_GE(at_most_allocated, in_use_stats._used_words - stats._free_blocks_word_size);

  }

public:

  MetaspaceArena* arena() { return _arena; }

  MetaspaceArenaTestBed(ChunkManager* cm, const ArenaGrowthPolicy* alloc_sequence,
                        SizeAtomicCounter* used_words_counter, SizeRange allocation_range) :
    _arena(NULL),
    _lock(NULL),
    _allocation_range(allocation_range),
    _size_of_last_failed_allocation(0),
    _allocations(NULL),
    _alloc_count(),
    _dealloc_count()
  {
    _lock = new Mutex(Monitor::native, "gtest-MetaspaceArenaTestBed-lock", false, Monitor::_safepoint_check_never);
    // Lock during space creation, since this is what happens in the VM too
    //  (see ClassLoaderData::metaspace_non_null(), which we mimick here).
    MutexLocker ml(_lock,  Mutex::_no_safepoint_check_flag);
    _arena = new MetaspaceArena(cm, alloc_sequence, _lock, used_words_counter, "gtest-MetaspaceArenaTestBed-sm");
  }

  ~MetaspaceArenaTestBed() {

    verify_arena_statistics();

    allocation_t* a = _allocations;
    while (a != NULL) {
      allocation_t* b = a->next;
      a->verify();
      FREE_C_HEAP_OBJ(a);
      a = b;
    }

    DEBUG_ONLY(_arena->verify();)

    // Delete MetaspaceArena. That should clean up all metaspace.
    delete _arena;
    delete _lock;

  }

  size_t words_allocated() const        { return _alloc_count.total_size(); }
  int num_allocations() const           { return _alloc_count.count(); }

  size_t size_of_last_failed_allocation() const { return _size_of_last_failed_allocation; }

  // Allocate a random amount. Return false if the allocation failed.
  bool checked_random_allocate() {
    size_t word_size = 1 + _allocation_range.random_value();
    MetaWord* p = _arena->allocate(word_size);
    if (p != NULL) {
      EXPECT_TRUE(is_aligned(p, sizeof(MetaWord)));
      allocation_t* a = NEW_C_HEAP_OBJ(allocation_t, mtInternal);
      a->word_size = word_size;
      a->p = p;
      a->mark();
      a->next = _allocations;
      _allocations = a;
      _alloc_count.add(word_size);
      if ((_alloc_count.count() % 20) == 0) {
        verify_arena_statistics();
        DEBUG_ONLY(_arena->verify();)
      }
      return true;
    } else {
      _size_of_last_failed_allocation = word_size;
    }
    return false;
  }

  // Deallocate a random allocation
  void checked_random_deallocate() {
    allocation_t* a = _allocations;
    while (a && a->p != NULL && os::random() % 10 != 0) {
      a = a->next;
    }
    if (a != NULL && a->p != NULL) {
      a->verify();
      _arena->deallocate(a->p, a->word_size);
      _dealloc_count.add(a->word_size);
      a->p = NULL; a->word_size = 0;
      if ((_dealloc_count.count() % 20) == 0) {
        verify_arena_statistics();
        DEBUG_ONLY(_arena->verify();)
      }
    }
  }

}; // End: MetaspaceArenaTestBed

class MetaspaceArenaTest {

  MetaspaceGtestContext _context;

  SizeAtomicCounter _used_words_counter;

  SparseArray<MetaspaceArenaTestBed*> _testbeds;
  IntCounter _num_beds;

  //////// Bed creation, destruction ///////

  void create_new_test_bed_at(int slotindex, const ArenaGrowthPolicy* growth_policy, SizeRange allocation_range) {
    DEBUG_ONLY(_testbeds.check_slot_is_null(slotindex));
    MetaspaceArenaTestBed* bed = new MetaspaceArenaTestBed(&_context.cm(), growth_policy,
                                                       &_used_words_counter, allocation_range);
    _testbeds.set_at(slotindex, bed);
    _num_beds.increment();
  }

  void create_random_test_bed_at(int slotindex) {
    SizeRange allocation_range(1, 100); // randomize too?
    const ArenaGrowthPolicy* growth_policy = ArenaGrowthPolicy::policy_for_space_type(
        (fifty_fifty() ? Metaspace::StandardMetaspaceType : Metaspace::ReflectionMetaspaceType),
         fifty_fifty());
    create_new_test_bed_at(slotindex, growth_policy, allocation_range);
   }

  // Randomly create a random test bed at a random slot, and return its slot index
  // (returns false if we reached max number of test beds)
  bool create_random_test_bed() {
    const int slot = _testbeds.random_null_slot_index();
    if (slot != -1) {
      create_random_test_bed_at(slot);
    }
    return slot;
  }

  // Create test beds for all slots
  void create_all_test_beds() {
    for (int slot = 0; slot < _testbeds.size(); slot++) {
      if (_testbeds.slot_is_null(slot)) {
        create_random_test_bed_at(slot);
      }
    }
  }

  void delete_test_bed_at(int slotindex) {
    DEBUG_ONLY(_testbeds.check_slot_is_not_null(slotindex));
    MetaspaceArenaTestBed* bed = _testbeds.at(slotindex);
    delete bed; // This will return all its memory to the chunk manager
    _testbeds.set_at(slotindex, NULL);
    _num_beds.decrement();
  }

  // Randomly delete a random test bed at a random slot
  // Return false if there are no test beds to delete.
  bool delete_random_test_bed() {
    const int slotindex = _testbeds.random_non_null_slot_index();
    if (slotindex != -1) {
      delete_test_bed_at(slotindex);
      return true;
    }
    return false;
  }

  // Delete all test beds.
  void delete_all_test_beds() {
    for (int slot = _testbeds.first_non_null_slot(); slot != -1; slot = _testbeds.next_non_null_slot(slot)) {
      delete_test_bed_at(slot);
    }
  }

  //////// Allocating metaspace from test beds ///////

  bool random_allocate_from_testbed(int slotindex) {
    DEBUG_ONLY(_testbeds.check_slot_is_not_null(slotindex);)
    MetaspaceArenaTestBed* bed = _testbeds.at(slotindex);
    bool success = bed->checked_random_allocate();
    if (success == false) {
      // We must have hit a limit.
      EXPECT_LT(_context.commit_limiter().possible_expansion_words(),
                metaspace::get_raw_word_size_for_requested_word_size(bed->size_of_last_failed_allocation()));
    }
    return success;
  }

  // Allocate multiple times random sizes from a single MetaspaceArena.
  bool random_allocate_multiple_times_from_testbed(int slotindex, int num_allocations) {
    bool success = true;
    int n = 0;
    while (success && n < num_allocations) {
      success = random_allocate_from_testbed(slotindex);
      n++;
    }
    return success;
  }

  // Allocate multiple times random sizes from a single random MetaspaceArena.
  bool random_allocate_random_times_from_random_testbed() {
    int slot = _testbeds.random_non_null_slot_index();
    bool success = false;
    if (slot != -1) {
      const int n = IntRange(5, 20).random_value();
      success = random_allocate_multiple_times_from_testbed(slot, n);
    }
    return success;
  }

  /////// Deallocating from testbed ///////////////////

  void deallocate_from_testbed(int slotindex) {
    DEBUG_ONLY(_testbeds.check_slot_is_not_null(slotindex);)
    MetaspaceArenaTestBed* bed = _testbeds.at(slotindex);
    bed->checked_random_deallocate();
  }

  void deallocate_from_random_testbed() {
    int slot = _testbeds.random_non_null_slot_index();
    if (slot != -1) {
      deallocate_from_testbed(slot);
    }
  }

  /////// Stats ///////////////////////////////////////

  int get_total_number_of_allocations() const {
    int sum = 0;
    for (int i = _testbeds.first_non_null_slot(); i != -1; i = _testbeds.next_non_null_slot(i)) {
      sum += _testbeds.at(i)->num_allocations();
    }
    return sum;
  }

  size_t get_total_words_allocated() const {
    size_t sum = 0;
    for (int i = _testbeds.first_non_null_slot(); i != -1; i = _testbeds.next_non_null_slot(i)) {
      sum += _testbeds.at(i)->words_allocated();
    }
    return sum;
  }

public:

  MetaspaceArenaTest(size_t commit_limit, int num_testbeds)
    : _context(commit_limit),
      _testbeds(num_testbeds),
      _num_beds()
  {}

  ~MetaspaceArenaTest () {

    delete_all_test_beds();

  }

  //////////////// Tests ////////////////////////

  void test() {

    // In a big loop, randomly chose one of these actions
    // - creating a test bed (simulates a new loader creation)
    // - allocating from a test bed (simulates allocating metaspace for a loader)
    // - (rarely) deallocate (simulates metaspace deallocation, e.g. class redefinitions)
    // - delete a test bed (simulates collection of a loader and subsequent return of metaspace to freelists)

    const int iterations = 10000;

    // Lets have a ceiling on number of words allocated (this is independent from the commit limit)
    const size_t max_allocation_size = 8 * M;

    bool force_bed_deletion = false;

    for (int niter = 0; niter < iterations; niter++) {

      const int r = IntRange(100).random_value();

      if (force_bed_deletion || r < 10) {

        force_bed_deletion = false;
        delete_random_test_bed();

      } else if (r < 20 || _num_beds.get() < (unsigned)_testbeds.size() / 2) {

        create_random_test_bed();

      } else if (r < 95) {

        // If allocation fails, we hit the commit limit and should delete some beds first
        force_bed_deletion = ! random_allocate_random_times_from_random_testbed();

      } else {

        // Note: does not affect the used words counter.
        deallocate_from_random_testbed();

      }

      // If we are close to our quota, start bed deletion
      if (_used_words_counter.get() >= max_allocation_size) {

        force_bed_deletion = true;

      }

    }

  }

};

// 32 parallel MetaspaceArena objects, random allocating without commit limit
TEST_VM(metaspace, MetaspaceArena_random_allocs_32_beds_no_commit_limit) {
  MetaspaceArenaTest test(max_uintx, 32);
  test.test();
}

// 32 parallel Metaspace arena objects, random allocating with commit limit
TEST_VM(metaspace, MetaspaceArena_random_allocs_32_beds_with_commit_limit) {
  MetaspaceArenaTest test(2 * M, 32);
  test.test();
}

// A single MetaspaceArena, random allocating without commit limit. This should exercise
//  chunk enlargement since allocation is undisturbed.
TEST_VM(metaspace, MetaspaceArena_random_allocs_1_bed_no_commit_limit) {
  MetaspaceArenaTest test(max_uintx, 1);
  test.test();
}

