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

#ifndef SHARE_GC_G1_HEAPREGIONSET_HPP
#define SHARE_GC_G1_HEAPREGIONSET_HPP

#include "gc/g1/heapRegion.hpp"
#include "utilities/macros.hpp"

#define assert_heap_region_set(p, message) \
  do {                                     \
    assert((p), "[%s] %s ln: %u",          \
           name(), message, length());     \
  } while (0)

#define guarantee_heap_region_set(p, message) \
  do {                                        \
    guarantee((p), "[%s] %s ln: %u",          \
              name(), message, length());     \
  } while (0)

#define assert_free_region_list(p, message)                          \
  do {                                                               \
    assert((p), "[%s] %s ln: %u hd: " PTR_FORMAT " tl: " PTR_FORMAT, \
           name(), message, length(), p2i(_head), p2i(_tail));       \
  } while (0)


// Interface collecting various instance specific verification methods of
// HeapRegionSets.
class HeapRegionSetChecker : public CHeapObj<mtGC> {
public:
  // Verify MT safety for this HeapRegionSet.
  virtual void check_mt_safety() = 0;
  // Returns true if the given HeapRegion is of the correct type for this HeapRegionSet.
  virtual bool is_correct_type(HeapRegion* hr) = 0;
  // Return a description of the type of regions this HeapRegionSet contains.
  virtual const char* get_description() = 0;
};

// Base class for all the classes that represent heap region sets. It
// contains the basic attributes that each set needs to maintain
// (e.g., length, region num, used bytes sum) plus any shared
// functionality (e.g., verification).

class HeapRegionSetBase {
  friend class VMStructs;

  HeapRegionSetChecker* _checker;

protected:
  // The number of regions in to the set.
  uint _length;

  const char* _name;

  bool _verify_in_progress;

  // verify_region() is used to ensure that the contents of a region
  // added to / removed from a set are consistent.
  void verify_region(HeapRegion* hr) PRODUCT_RETURN;

  void check_mt_safety() {
    if (_checker != NULL) {
      _checker->check_mt_safety();
    }
  }

  HeapRegionSetBase(const char* name, HeapRegionSetChecker* verifier);

public:
  const char* name() { return _name; }

  uint length() const { return _length; }

  bool is_empty() { return _length == 0; }

  // It updates the fields of the set to reflect hr being added to
  // the set and tags the region appropriately.
  inline void add(HeapRegion* hr);

  // It updates the fields of the set to reflect hr being removed
  // from the set and tags the region appropriately.
  inline void remove(HeapRegion* hr);

  virtual void verify();
  void verify_start();
  void verify_next_region(HeapRegion* hr);
  void verify_end();

  void verify_optional() { DEBUG_ONLY(verify();) }

  virtual void print_on(outputStream* out, bool print_contents = false);
};

// This class represents heap region sets whose members are not
// explicitly tracked. It's helpful to group regions using such sets
// so that we can reason about all the region groups in the heap using
// the same interface (namely, the HeapRegionSetBase API).

class HeapRegionSet : public HeapRegionSetBase {
public:
  HeapRegionSet(const char* name, HeapRegionSetChecker* checker):
    HeapRegionSetBase(name, checker) {
  }

  void bulk_remove(const uint removed) {
    _length -= removed;
  }
};

// A set that links all the regions added to it in a doubly-linked
// sorted list. We should try to avoid doing operations that iterate over
// such lists in performance critical paths. Typically we should
// add / remove one region at a time or concatenate two lists.

class FreeRegionListIterator;
class G1NUMA;

class FreeRegionList : public HeapRegionSetBase {
  friend class FreeRegionListIterator;

private:

  // This class is only initialized if there are multiple active nodes.
  class NodeInfo : public CHeapObj<mtGC> {
    G1NUMA* _numa;
    uint*   _length_of_node;
    uint    _num_nodes;

  public:
    NodeInfo();
    ~NodeInfo();

    inline void increase_length(uint node_index);
    inline void decrease_length(uint node_index);

    inline uint length(uint index) const;

    void clear();

    void add(NodeInfo* info);
  };

  HeapRegion* _head;
  HeapRegion* _tail;

  // _last is used to keep track of where we added an element the last
  // time. It helps to improve performance when adding several ordered items in a row.
  HeapRegion* _last;

  NodeInfo*   _node_info;

  static uint _unrealistically_long_length;

  inline HeapRegion* remove_from_head_impl();
  inline HeapRegion* remove_from_tail_impl();

  inline void increase_length(uint node_index);
  inline void decrease_length(uint node_index);

  // Common checks for adding a list.
  void add_list_common_start(FreeRegionList* from_list);
  void add_list_common_end(FreeRegionList* from_list);

  void verify_region_to_remove(HeapRegion* curr, HeapRegion* next) NOT_DEBUG_RETURN;
protected:
  // See the comment for HeapRegionSetBase::clear()
  virtual void clear();

public:
  FreeRegionList(const char* name, HeapRegionSetChecker* checker = NULL);
  ~FreeRegionList();

  void verify_list();

#ifdef ASSERT
  bool contains(HeapRegion* hr) const {
    return hr->containing_set() == this;
  }
#endif

  static void set_unrealistically_long_length(uint len);

  // Add hr to the list. The region should not be a member of another set.
  // Assumes that the list is ordered and will preserve that order. The order
  // is determined by hrm_index.
  inline void add_ordered(HeapRegion* hr);
  // Same restrictions as above, but adds the region last in the list.
  inline void add_to_tail(HeapRegion* region_to_add);

  // Removes from head or tail based on the given argument.
  HeapRegion* remove_region(bool from_head);

  HeapRegion* remove_region_with_node_index(bool from_head,
                                            uint requested_node_index);

  // Merge two ordered lists. The result is also ordered. The order is
  // determined by hrm_index.
  void add_ordered(FreeRegionList* from_list);
  void append_ordered(FreeRegionList* from_list);

  // It empties the list by removing all regions from it.
  void remove_all();

  // Abandon current free list. Requires that all regions in the current list
  // are taken care of separately, to allow a rebuild.
  void abandon();

  // Remove all (contiguous) regions from first to first + num_regions -1 from
  // this list.
  // Num_regions must be >= 1.
  void remove_starting_at(HeapRegion* first, uint num_regions);

  virtual void verify();

  using HeapRegionSetBase::length;
  uint length(uint node_index) const;
};

// Iterator class that provides a convenient way to iterate over the
// regions of a FreeRegionList.

class FreeRegionListIterator : public StackObj {
private:
  FreeRegionList* _list;
  HeapRegion*     _curr;

public:
  bool more_available() {
    return _curr != NULL;
  }

  HeapRegion* get_next() {
    assert(more_available(),
           "get_next() should be called when more regions are available");

    // If we are going to introduce a count in the iterator we should
    // do the "cycle" check.

    HeapRegion* hr = _curr;
    _list->verify_region(hr);
    _curr = hr->next();
    return hr;
  }

  FreeRegionListIterator(FreeRegionList* list)
  : _list(list),
    _curr(list->_head) {
  }
};

#endif // SHARE_GC_G1_HEAPREGIONSET_HPP
