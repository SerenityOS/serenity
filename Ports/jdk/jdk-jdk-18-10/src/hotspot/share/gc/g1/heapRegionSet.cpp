/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1NUMA.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "gc/g1/heapRegionSet.inline.hpp"

uint FreeRegionList::_unrealistically_long_length = 0;

#ifndef PRODUCT
void HeapRegionSetBase::verify_region(HeapRegion* hr) {
  assert(hr->containing_set() == this, "Inconsistent containing set for %u", hr->hrm_index());
  assert(!hr->is_young(), "Adding young region %u", hr->hrm_index()); // currently we don't use these sets for young regions
  assert(_checker == NULL || _checker->is_correct_type(hr), "Wrong type of region %u (%s) and set %s",
         hr->hrm_index(), hr->get_type_str(), name());
  assert(!hr->is_free() || hr->is_empty(), "Free region %u is not empty for set %s", hr->hrm_index(), name());
  assert(!hr->is_empty() || hr->is_free() || hr->is_archive(),
         "Empty region %u is not free or archive for set %s", hr->hrm_index(), name());
}
#endif

void HeapRegionSetBase::verify() {
  // It's important that we also observe the MT safety protocol even
  // for the verification calls. If we do verification without the
  // appropriate locks and the set changes underneath our feet
  // verification might fail and send us on a wild goose chase.
  check_mt_safety();

  guarantee_heap_region_set(( is_empty() && length() == 0) ||
                            (!is_empty() && length() > 0),
                            "invariant");
}

void HeapRegionSetBase::verify_start() {
  // See comment in verify() about MT safety and verification.
  check_mt_safety();
  assert_heap_region_set(!_verify_in_progress, "verification should not be in progress");

  // Do the basic verification first before we do the checks over the regions.
  HeapRegionSetBase::verify();

  _verify_in_progress = true;
}

void HeapRegionSetBase::verify_end() {
  // See comment in verify() about MT safety and verification.
  check_mt_safety();
  assert_heap_region_set(_verify_in_progress, "verification should be in progress");

  _verify_in_progress = false;
}

void HeapRegionSetBase::print_on(outputStream* out, bool print_contents) {
  out->cr();
  out->print_cr("Set: %s (" PTR_FORMAT ")", name(), p2i(this));
  out->print_cr("  Region Type         : %s", _checker->get_description());
  out->print_cr("  Length              : %14u", length());
}

HeapRegionSetBase::HeapRegionSetBase(const char* name, HeapRegionSetChecker* checker)
  : _checker(checker), _length(0), _name(name), _verify_in_progress(false)
{
}

void FreeRegionList::set_unrealistically_long_length(uint len) {
  guarantee(_unrealistically_long_length == 0, "should only be set once");
  _unrealistically_long_length = len;
}

void FreeRegionList::abandon() {
  check_mt_safety();
  clear();
  verify_optional();
}

void FreeRegionList::remove_all() {
  check_mt_safety();
  verify_optional();

  HeapRegion* curr = _head;
  while (curr != NULL) {
    verify_region(curr);

    HeapRegion* next = curr->next();
    curr->set_next(NULL);
    curr->set_prev(NULL);
    curr->set_containing_set(NULL);

    decrease_length(curr->node_index());

    curr = next;
  }
  clear();

  verify_optional();
}

void FreeRegionList::add_list_common_start(FreeRegionList* from_list) {
  check_mt_safety();
  from_list->check_mt_safety();
  verify_optional();
  from_list->verify_optional();

  if (from_list->is_empty()) {
    return;
  }

  if (_node_info != NULL && from_list->_node_info != NULL) {
    _node_info->add(from_list->_node_info);
  }

  #ifdef ASSERT
  FreeRegionListIterator iter(from_list);
  while (iter.more_available()) {
    HeapRegion* hr = iter.get_next();
    // In set_containing_set() we check that we either set the value
    // from NULL to non-NULL or vice versa to catch bugs. So, we have
    // to NULL it first before setting it to the value.
    hr->set_containing_set(NULL);
    hr->set_containing_set(this);
  }
  #endif // ASSERT
}

void FreeRegionList::add_list_common_end(FreeRegionList* from_list) {
  _length += from_list->length();
  from_list->clear();

  verify_optional();
  from_list->verify_optional();
}

void FreeRegionList::append_ordered(FreeRegionList* from_list) {
  add_list_common_start(from_list);

  if (from_list->is_empty()) {
    return;
  }

  if (is_empty()) {
    // Make from_list the current list.
    assert_free_region_list(length() == 0 && _tail == NULL, "invariant");
    _head = from_list->_head;
    _tail = from_list->_tail;
  } else {
    // Add the from_list to the end of the current list.
    assert(_tail->hrm_index() < from_list->_head->hrm_index(), "Should be sorted %u < %u",
           _tail->hrm_index(), from_list->_head->hrm_index());

    _tail->set_next(from_list->_head);
    from_list->_head->set_prev(_tail);
    _tail = from_list->_tail;
  }

  add_list_common_end(from_list);
}

void FreeRegionList::add_ordered(FreeRegionList* from_list) {
  add_list_common_start(from_list);

  if (from_list->is_empty()) {
    return;
  }

  if (is_empty()) {
    assert_free_region_list(length() == 0 && _tail == NULL, "invariant");
    _head = from_list->_head;
    _tail = from_list->_tail;
  } else {
    HeapRegion* curr_to = _head;
    HeapRegion* curr_from = from_list->_head;

    while (curr_from != NULL) {
      while (curr_to != NULL && curr_to->hrm_index() < curr_from->hrm_index()) {
        curr_to = curr_to->next();
      }

      if (curr_to == NULL) {
        // The rest of the from list should be added as tail
        _tail->set_next(curr_from);
        curr_from->set_prev(_tail);
        curr_from = NULL;
      } else {
        HeapRegion* next_from = curr_from->next();

        curr_from->set_next(curr_to);
        curr_from->set_prev(curr_to->prev());
        if (curr_to->prev() == NULL) {
          _head = curr_from;
        } else {
          curr_to->prev()->set_next(curr_from);
        }
        curr_to->set_prev(curr_from);

        curr_from = next_from;
      }
    }

    if (_tail->hrm_index() < from_list->_tail->hrm_index()) {
      _tail = from_list->_tail;
    }
  }

  add_list_common_end(from_list);
}

#ifdef ASSERT
void FreeRegionList::verify_region_to_remove(HeapRegion* curr, HeapRegion* next) {
  assert_free_region_list(_head != next, "invariant");
  if (next != NULL) {
    assert_free_region_list(next->prev() == curr, "invariant");
    assert_free_region_list(_tail != curr, "invariant");
  } else {
    assert_free_region_list(_tail == curr, "invariant");
  }
  HeapRegion* prev = curr->prev();
  if (prev == NULL) {
    assert_free_region_list(_head == curr, "invariant");
  } else {
    assert_free_region_list(_head != curr, "invariant");
  }
}
#endif

void FreeRegionList::remove_starting_at(HeapRegion* first, uint num_regions) {
  check_mt_safety();
  assert_free_region_list(num_regions >= 1, "pre-condition");
  assert_free_region_list(!is_empty(), "pre-condition");
  assert_free_region_list(length() >= num_regions, "pre-condition");

  verify_optional();
  DEBUG_ONLY(uint old_length = length();)

  // prev points to the node right before first or null when first == _head
  HeapRegion* const prev = first->prev();
  // next points to the node right after first or null when first == _tail,
  // and after the while loop below, next should point to the next node right
  // after the removed sublist, or null if the sublist contains _tail.
  HeapRegion* next = first->next();

  HeapRegion* curr = first;
  uint count = 0;
  while (count < num_regions) {
    verify_region(curr);
    next = curr->next();
    verify_region_to_remove(curr, next);

    if (_last == curr) {
      _last = NULL;
    }

    curr->set_next(NULL);
    curr->set_prev(NULL);
    remove(curr);

    count++;

    decrease_length(curr->node_index());

    curr = next;
  }

  if (prev == NULL) {
    _head = next;
  } else {
    prev->set_next(next);
  }
  if (next == NULL) {
    _tail = prev;
  } else {
    next->set_prev(prev);
  }

  assert(count == num_regions,
         "[%s] count: %u should be == num_regions: %u",
         name(), count, num_regions);
  assert(length() + num_regions == old_length,
         "[%s] new length should be consistent "
         "new length: %u old length: %u num_regions: %u",
         name(), length(), old_length, num_regions);

  verify_optional();
}

void FreeRegionList::verify() {
  // See comment in HeapRegionSetBase::verify() about MT safety and
  // verification.
  check_mt_safety();

  // This will also do the basic verification too.
  verify_start();

  verify_list();

  verify_end();
}

void FreeRegionList::clear() {
  _length = 0;
  _head = NULL;
  _tail = NULL;
  _last = NULL;

  if (_node_info!= NULL) {
    _node_info->clear();
  }
}

void FreeRegionList::verify_list() {
  HeapRegion* curr = _head;
  HeapRegion* prev1 = NULL;
  HeapRegion* prev0 = NULL;
  uint count = 0;
  size_t capacity = 0;
  uint last_index = 0;

  guarantee(_head == NULL || _head->prev() == NULL, "_head should not have a prev");
  while (curr != NULL) {
    verify_region(curr);

    count++;
    guarantee(count < _unrealistically_long_length,
              "[%s] the calculated length: %u seems very long, is there maybe a cycle? curr: " PTR_FORMAT " prev0: " PTR_FORMAT " " "prev1: " PTR_FORMAT " length: %u",
              name(), count, p2i(curr), p2i(prev0), p2i(prev1), length());

    if (curr->next() != NULL) {
      guarantee(curr->next()->prev() == curr, "Next or prev pointers messed up");
    }
    guarantee(curr->hrm_index() == 0 || curr->hrm_index() > last_index, "List should be sorted");
    last_index = curr->hrm_index();

    capacity += curr->capacity();

    prev1 = prev0;
    prev0 = curr;
    curr = curr->next();
  }

  guarantee(_tail == prev0, "Expected %s to end with %u but it ended with %u.", name(), _tail->hrm_index(), prev0->hrm_index());
  guarantee(_tail == NULL || _tail->next() == NULL, "_tail should not have a next");
  guarantee(length() == count, "%s count mismatch. Expected %u, actual %u.", name(), length(), count);
}


FreeRegionList::FreeRegionList(const char* name, HeapRegionSetChecker* checker):
  HeapRegionSetBase(name, checker),
  _node_info(G1NUMA::numa()->is_enabled() ? new NodeInfo() : NULL) {

  clear();
}

FreeRegionList::~FreeRegionList() {
  if (_node_info != NULL) {
    delete _node_info;
  }
}

FreeRegionList::NodeInfo::NodeInfo() : _numa(G1NUMA::numa()), _length_of_node(NULL),
                                       _num_nodes(_numa->num_active_nodes()) {
  assert(UseNUMA, "Invariant");

  _length_of_node = NEW_C_HEAP_ARRAY(uint, _num_nodes, mtGC);
}

FreeRegionList::NodeInfo::~NodeInfo() {
  FREE_C_HEAP_ARRAY(uint, _length_of_node);
}

void FreeRegionList::NodeInfo::clear() {
  for (uint i = 0; i < _num_nodes; ++i) {
    _length_of_node[i] = 0;
  }
}

void FreeRegionList::NodeInfo::add(NodeInfo* info) {
  for (uint i = 0; i < _num_nodes; ++i) {
    _length_of_node[i] += info->_length_of_node[i];
  }
}

