/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/cardTable.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/space.inline.hpp"
#include "logging/log.hpp"
#include "memory/virtualspace.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"

size_t CardTable::compute_byte_map_size() {
  assert(_guard_index == cards_required(_whole_heap.word_size()) - 1,
                                        "uninitialized, check declaration order");
  assert(_page_size != 0, "uninitialized, check declaration order");
  const size_t granularity = os::vm_allocation_granularity();
  return align_up(_guard_index + 1, MAX2(_page_size, granularity));
}

CardTable::CardTable(MemRegion whole_heap) :
  _whole_heap(whole_heap),
  _guard_index(0),
  _last_valid_index(0),
  _page_size(os::vm_page_size()),
  _byte_map_size(0),
  _byte_map(NULL),
  _byte_map_base(NULL),
  _cur_covered_regions(0),
  _covered(MemRegion::create_array(_max_covered_regions, mtGC)),
  _committed(MemRegion::create_array(_max_covered_regions, mtGC)),
  _guard_region()
{
  assert((uintptr_t(_whole_heap.start())  & (card_size - 1))  == 0, "heap must start at card boundary");
  assert((uintptr_t(_whole_heap.end()) & (card_size - 1))  == 0, "heap must end at card boundary");

  assert(card_size <= 512, "card_size must be less than 512"); // why?
}

CardTable::~CardTable() {
  MemRegion::destroy_array(_covered, _max_covered_regions);
  MemRegion::destroy_array(_committed, _max_covered_regions);
}

void CardTable::initialize() {
  _guard_index = cards_required(_whole_heap.word_size()) - 1;
  _last_valid_index = _guard_index - 1;

  _byte_map_size = compute_byte_map_size();

  HeapWord* low_bound  = _whole_heap.start();
  HeapWord* high_bound = _whole_heap.end();

  _cur_covered_regions = 0;

  const size_t rs_align = _page_size == (size_t) os::vm_page_size() ? 0 :
    MAX2(_page_size, (size_t) os::vm_allocation_granularity());
  ReservedSpace heap_rs(_byte_map_size, rs_align, _page_size);

  MemTracker::record_virtual_memory_type((address)heap_rs.base(), mtGC);

  os::trace_page_sizes("Card Table", _guard_index + 1, _guard_index + 1,
                       _page_size, heap_rs.base(), heap_rs.size());
  if (!heap_rs.is_reserved()) {
    vm_exit_during_initialization("Could not reserve enough space for the "
                                  "card marking array");
  }

  // The assembler store_check code will do an unsigned shift of the oop,
  // then add it to _byte_map_base, i.e.
  //
  //   _byte_map = _byte_map_base + (uintptr_t(low_bound) >> card_shift)
  _byte_map = (CardValue*) heap_rs.base();
  _byte_map_base = _byte_map - (uintptr_t(low_bound) >> card_shift);
  assert(byte_for(low_bound) == &_byte_map[0], "Checking start of map");
  assert(byte_for(high_bound-1) <= &_byte_map[_last_valid_index], "Checking end of map");

  CardValue* guard_card = &_byte_map[_guard_index];
  HeapWord* guard_page = align_down((HeapWord*)guard_card, _page_size);
  _guard_region = MemRegion(guard_page, _page_size);
  os::commit_memory_or_exit((char*)guard_page, _page_size, _page_size,
                            !ExecMem, "card table last card");
  *guard_card = last_card;

  log_trace(gc, barrier)("CardTable::CardTable: ");
  log_trace(gc, barrier)("    &_byte_map[0]: " INTPTR_FORMAT "  &_byte_map[_last_valid_index]: " INTPTR_FORMAT,
                  p2i(&_byte_map[0]), p2i(&_byte_map[_last_valid_index]));
  log_trace(gc, barrier)("    _byte_map_base: " INTPTR_FORMAT, p2i(_byte_map_base));
}

int CardTable::find_covering_region_by_base(HeapWord* base) {
  int i;
  for (i = 0; i < _cur_covered_regions; i++) {
    if (_covered[i].start() == base) return i;
    if (_covered[i].start() > base) break;
  }
  // If we didn't find it, create a new one.
  assert(_cur_covered_regions < _max_covered_regions,
         "too many covered regions");
  // Move the ones above up, to maintain sorted order.
  for (int j = _cur_covered_regions; j > i; j--) {
    _covered[j] = _covered[j-1];
    _committed[j] = _committed[j-1];
  }
  int res = i;
  _cur_covered_regions++;
  _covered[res].set_start(base);
  _covered[res].set_word_size(0);
  CardValue* ct_start = byte_for(base);
  HeapWord* ct_start_aligned = align_down((HeapWord*)ct_start, _page_size);
  _committed[res].set_start(ct_start_aligned);
  _committed[res].set_word_size(0);
  return res;
}

int CardTable::find_covering_region_containing(HeapWord* addr) {
  for (int i = 0; i < _cur_covered_regions; i++) {
    if (_covered[i].contains(addr)) {
      return i;
    }
  }
  assert(0, "address outside of heap?");
  return -1;
}

HeapWord* CardTable::largest_prev_committed_end(int ind) const {
  HeapWord* max_end = NULL;
  for (int j = 0; j < ind; j++) {
    HeapWord* this_end = _committed[j].end();
    if (this_end > max_end) max_end = this_end;
  }
  return max_end;
}

MemRegion CardTable::committed_unique_to_self(int self, MemRegion mr) const {
  MemRegion result = mr;
  for (int r = 0; r < _cur_covered_regions; r += 1) {
    if (r != self) {
      result = result.minus(_committed[r]);
    }
  }
  // Never include the guard page.
  result = result.minus(_guard_region);
  return result;
}

void CardTable::resize_covered_region(MemRegion new_region) {
  // We don't change the start of a region, only the end.
  assert(_whole_heap.contains(new_region),
           "attempt to cover area not in reserved area");
  debug_only(verify_guard();)
  // collided is true if the expansion would push into another committed region
  debug_only(bool collided = false;)
  int const ind = find_covering_region_by_base(new_region.start());
  MemRegion const old_region = _covered[ind];
  assert(old_region.start() == new_region.start(), "just checking");
  if (new_region.word_size() != old_region.word_size()) {
    // Commit new or uncommit old pages, if necessary.
    MemRegion cur_committed = _committed[ind];
    // Extend the end of this _committed region
    // to cover the end of any lower _committed regions.
    // This forms overlapping regions, but never interior regions.
    HeapWord* const max_prev_end = largest_prev_committed_end(ind);
    if (max_prev_end > cur_committed.end()) {
      cur_committed.set_end(max_prev_end);
    }
    // Align the end up to a page size (starts are already aligned).
    HeapWord* new_end = (HeapWord*) byte_after(new_region.last());
    HeapWord* new_end_aligned = align_up(new_end, _page_size);
    assert(new_end_aligned >= new_end, "align up, but less");
    // Check the other regions (excludes "ind") to ensure that
    // the new_end_aligned does not intrude onto the committed
    // space of another region.
    int ri = 0;
    for (ri = ind + 1; ri < _cur_covered_regions; ri++) {
      if (new_end_aligned > _committed[ri].start()) {
        assert(new_end_aligned <= _committed[ri].end(),
               "An earlier committed region can't cover a later committed region");
        // Any region containing the new end
        // should start at or beyond the region found (ind)
        // for the new end (committed regions are not expected to
        // be proper subsets of other committed regions).
        assert(_committed[ri].start() >= _committed[ind].start(),
               "New end of committed region is inconsistent");
        new_end_aligned = _committed[ri].start();
        // new_end_aligned can be equal to the start of its
        // committed region (i.e., of "ind") if a second
        // region following "ind" also start at the same location
        // as "ind".
        assert(new_end_aligned >= _committed[ind].start(),
          "New end of committed region is before start");
        debug_only(collided = true;)
        // Should only collide with 1 region
        break;
      }
    }
#ifdef ASSERT
    for (++ri; ri < _cur_covered_regions; ri++) {
      assert(!_committed[ri].contains(new_end_aligned),
        "New end of committed region is in a second committed region");
    }
#endif
    // The guard page is always committed and should not be committed over.
    // "guarded" is used for assertion checking below and recalls the fact
    // that the would-be end of the new committed region would have
    // penetrated the guard page.
    HeapWord* new_end_for_commit = new_end_aligned;

    DEBUG_ONLY(bool guarded = false;)
    if (new_end_for_commit > _guard_region.start()) {
      new_end_for_commit = _guard_region.start();
      DEBUG_ONLY(guarded = true;)
    }

    if (new_end_for_commit > cur_committed.end()) {
      // Must commit new pages.
      MemRegion const new_committed =
        MemRegion(cur_committed.end(), new_end_for_commit);

      assert(!new_committed.is_empty(), "Region should not be empty here");
      os::commit_memory_or_exit((char*)new_committed.start(),
                                new_committed.byte_size(), _page_size,
                                !ExecMem, "card table expansion");
    // Use new_end_aligned (as opposed to new_end_for_commit) because
    // the cur_committed region may include the guard region.
    } else if (new_end_aligned < cur_committed.end()) {
      // Must uncommit pages.
      MemRegion const uncommit_region =
        committed_unique_to_self(ind, MemRegion(new_end_aligned,
                                                cur_committed.end()));
      if (!uncommit_region.is_empty()) {
        if (!os::uncommit_memory((char*)uncommit_region.start(),
                                 uncommit_region.byte_size())) {
          assert(false, "Card table contraction failed");
          // The call failed so don't change the end of the
          // committed region.  This is better than taking the
          // VM down.
          new_end_aligned = _committed[ind].end();
        }
      }
    }
    // In any case, we can reset the end of the current committed entry.
    _committed[ind].set_end(new_end_aligned);

#ifdef ASSERT
    // Check that the last card in the new region is committed according
    // to the tables.
    bool covered = false;
    for (int cr = 0; cr < _cur_covered_regions; cr++) {
      if (_committed[cr].contains(new_end - 1)) {
        covered = true;
        break;
      }
    }
    assert(covered, "Card for end of new region not committed");
#endif

    // The default of 0 is not necessarily clean cards.
    CardValue* entry;
    if (old_region.last() < _whole_heap.start()) {
      entry = byte_for(_whole_heap.start());
    } else {
      entry = byte_after(old_region.last());
    }
    assert(index_for(new_region.last()) <  _guard_index,
      "The guard card will be overwritten");
    // This line commented out cleans the newly expanded region and
    // not the aligned up expanded region.
    // CardValue* const end = byte_after(new_region.last());
    CardValue* const end = (CardValue*) new_end_for_commit;
    assert((end >= byte_after(new_region.last())) || collided || guarded,
      "Expect to be beyond new region unless impacting another region");
    // do nothing if we resized downward.
#ifdef ASSERT
    for (int ri = 0; ri < _cur_covered_regions; ri++) {
      if (ri != ind) {
        // The end of the new committed region should not
        // be in any existing region unless it matches
        // the start of the next region.
        assert(!_committed[ri].contains(end) ||
               (_committed[ri].start() == (HeapWord*) end),
               "Overlapping committed regions");
      }
    }
#endif
    if (entry < end) {
      memset(entry, clean_card, pointer_delta(end, entry, sizeof(CardValue)));
    }
  }
  // In any case, the covered size changes.
  _covered[ind].set_word_size(new_region.word_size());

  log_trace(gc, barrier)("CardTable::resize_covered_region: ");
  log_trace(gc, barrier)("    _covered[%d].start(): " INTPTR_FORMAT " _covered[%d].last(): " INTPTR_FORMAT,
                         ind, p2i(_covered[ind].start()), ind, p2i(_covered[ind].last()));
  log_trace(gc, barrier)("    _committed[%d].start(): " INTPTR_FORMAT "  _committed[%d].last(): " INTPTR_FORMAT,
                         ind, p2i(_committed[ind].start()), ind, p2i(_committed[ind].last()));
  log_trace(gc, barrier)("    byte_for(start): " INTPTR_FORMAT "  byte_for(last): " INTPTR_FORMAT,
                         p2i(byte_for(_covered[ind].start())),  p2i(byte_for(_covered[ind].last())));
  log_trace(gc, barrier)("    addr_for(start): " INTPTR_FORMAT "  addr_for(last): " INTPTR_FORMAT,
                         p2i(addr_for((CardValue*) _committed[ind].start())),  p2i(addr_for((CardValue*) _committed[ind].last())));

  // Touch the last card of the covered region to show that it
  // is committed (or SEGV).
  debug_only((void) (*byte_for(_covered[ind].last()));)
  debug_only(verify_guard();)
}

// Note that these versions are precise!  The scanning code has to handle the
// fact that the write barrier may be either precise or imprecise.
void CardTable::dirty_MemRegion(MemRegion mr) {
  assert(align_down(mr.start(), HeapWordSize) == mr.start(), "Unaligned start");
  assert(align_up  (mr.end(),   HeapWordSize) == mr.end(),   "Unaligned end"  );
  CardValue* cur  = byte_for(mr.start());
  CardValue* last = byte_after(mr.last());
  while (cur < last) {
    *cur = dirty_card;
    cur++;
  }
}

void CardTable::clear_MemRegion(MemRegion mr) {
  // Be conservative: only clean cards entirely contained within the
  // region.
  CardValue* cur;
  if (mr.start() == _whole_heap.start()) {
    cur = byte_for(mr.start());
  } else {
    assert(mr.start() > _whole_heap.start(), "mr is not covered.");
    cur = byte_after(mr.start() - 1);
  }
  CardValue* last = byte_after(mr.last());
  memset(cur, clean_card, pointer_delta(last, cur, sizeof(CardValue)));
}

void CardTable::clear(MemRegion mr) {
  for (int i = 0; i < _cur_covered_regions; i++) {
    MemRegion mri = mr.intersection(_covered[i]);
    if (!mri.is_empty()) clear_MemRegion(mri);
  }
}

void CardTable::dirty(MemRegion mr) {
  CardValue* first = byte_for(mr.start());
  CardValue* last  = byte_after(mr.last());
  memset(first, dirty_card, last-first);
}

// Unlike several other card table methods, dirty_card_iterate()
// iterates over dirty cards ranges in increasing address order.
void CardTable::dirty_card_iterate(MemRegion mr, MemRegionClosure* cl) {
  for (int i = 0; i < _cur_covered_regions; i++) {
    MemRegion mri = mr.intersection(_covered[i]);
    if (!mri.is_empty()) {
      CardValue *cur_entry, *next_entry, *limit;
      for (cur_entry = byte_for(mri.start()), limit = byte_for(mri.last());
           cur_entry <= limit;
           cur_entry  = next_entry) {
        next_entry = cur_entry + 1;
        if (*cur_entry == dirty_card) {
          size_t dirty_cards;
          // Accumulate maximal dirty card range, starting at cur_entry
          for (dirty_cards = 1;
               next_entry <= limit && *next_entry == dirty_card;
               dirty_cards++, next_entry++);
          MemRegion cur_cards(addr_for(cur_entry),
                              dirty_cards*card_size_in_words);
          cl->do_MemRegion(cur_cards);
        }
      }
    }
  }
}

MemRegion CardTable::dirty_card_range_after_reset(MemRegion mr,
                                                  bool reset,
                                                  int reset_val) {
  for (int i = 0; i < _cur_covered_regions; i++) {
    MemRegion mri = mr.intersection(_covered[i]);
    if (!mri.is_empty()) {
      CardValue* cur_entry, *next_entry, *limit;
      for (cur_entry = byte_for(mri.start()), limit = byte_for(mri.last());
           cur_entry <= limit;
           cur_entry  = next_entry) {
        next_entry = cur_entry + 1;
        if (*cur_entry == dirty_card) {
          size_t dirty_cards;
          // Accumulate maximal dirty card range, starting at cur_entry
          for (dirty_cards = 1;
               next_entry <= limit && *next_entry == dirty_card;
               dirty_cards++, next_entry++);
          MemRegion cur_cards(addr_for(cur_entry),
                              dirty_cards*card_size_in_words);
          if (reset) {
            for (size_t i = 0; i < dirty_cards; i++) {
              cur_entry[i] = reset_val;
            }
          }
          return cur_cards;
        }
      }
    }
  }
  return MemRegion(mr.end(), mr.end());
}

uintx CardTable::ct_max_alignment_constraint() {
  return card_size * os::vm_page_size();
}

void CardTable::verify_guard() {
  // For product build verification
  guarantee(_byte_map[_guard_index] == last_card,
            "card table guard has been modified");
}

void CardTable::invalidate(MemRegion mr) {
  assert(align_down(mr.start(), HeapWordSize) == mr.start(), "Unaligned start");
  assert(align_up  (mr.end(),   HeapWordSize) == mr.end(),   "Unaligned end"  );
  for (int i = 0; i < _cur_covered_regions; i++) {
    MemRegion mri = mr.intersection(_covered[i]);
    if (!mri.is_empty()) dirty_MemRegion(mri);
  }
}

void CardTable::verify() {
  verify_guard();
}

#ifndef PRODUCT
void CardTable::verify_region(MemRegion mr, CardValue val, bool val_equals) {
  CardValue* start    = byte_for(mr.start());
  CardValue* end      = byte_for(mr.last());
  bool failures = false;
  for (CardValue* curr = start; curr <= end; ++curr) {
    CardValue curr_val = *curr;
    bool failed = (val_equals) ? (curr_val != val) : (curr_val == val);
    if (failed) {
      if (!failures) {
        log_error(gc, verify)("== CT verification failed: [" INTPTR_FORMAT "," INTPTR_FORMAT "]", p2i(start), p2i(end));
        log_error(gc, verify)("==   %sexpecting value: %d", (val_equals) ? "" : "not ", val);
        failures = true;
      }
      log_error(gc, verify)("==   card " PTR_FORMAT " [" PTR_FORMAT "," PTR_FORMAT "], val: %d",
                            p2i(curr), p2i(addr_for(curr)),
                            p2i((HeapWord*) (((size_t) addr_for(curr)) + card_size)),
                            (int) curr_val);
    }
  }
  guarantee(!failures, "there should not have been any failures");
}

void CardTable::verify_not_dirty_region(MemRegion mr) {
  verify_region(mr, dirty_card, false /* val_equals */);
}

void CardTable::verify_dirty_region(MemRegion mr) {
  verify_region(mr, dirty_card, true /* val_equals */);
}
#endif

void CardTable::print_on(outputStream* st) const {
  st->print_cr("Card table byte_map: [" INTPTR_FORMAT "," INTPTR_FORMAT "] _byte_map_base: " INTPTR_FORMAT,
               p2i(_byte_map), p2i(_byte_map + _byte_map_size), p2i(_byte_map_base));
}
