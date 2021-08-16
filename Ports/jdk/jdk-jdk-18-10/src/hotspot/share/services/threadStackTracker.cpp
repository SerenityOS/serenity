/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

#include "runtime/threadCritical.hpp"
#include "services/mallocTracker.hpp"
#include "services/memTracker.hpp"
#include "services/virtualMemoryTracker.hpp"
#include "services/threadStackTracker.hpp"

volatile size_t ThreadStackTracker::_thread_count = 0;
SortedLinkedList<SimpleThreadStackSite, ThreadStackTracker::compare_thread_stack_base>* ThreadStackTracker::_simple_thread_stacks = NULL;

bool ThreadStackTracker::initialize(NMT_TrackingLevel level) {
  if (level == NMT_detail && !track_as_vm()) {
    _simple_thread_stacks = new (std::nothrow, ResourceObj::C_HEAP, mtNMT)
      SortedLinkedList<SimpleThreadStackSite, ThreadStackTracker::compare_thread_stack_base>();
    return (_simple_thread_stacks != NULL);
  }
  return true;
}

bool ThreadStackTracker::transition(NMT_TrackingLevel from, NMT_TrackingLevel to) {
  assert (from != NMT_minimal, "cannot convert from the lowest tracking level to anything");
  if (to == NMT_minimal) {
    assert(from == NMT_summary || from == NMT_detail, "Just check");
    ThreadCritical tc;
    if (_simple_thread_stacks != NULL) {
      delete _simple_thread_stacks;
      _simple_thread_stacks = NULL;
    }
  }
  return true;
}

int ThreadStackTracker::compare_thread_stack_base(const SimpleThreadStackSite& s1, const SimpleThreadStackSite& s2) {
  return s1.base() - s2.base();
}

void ThreadStackTracker::new_thread_stack(void* base, size_t size, const NativeCallStack& stack) {
  assert(MemTracker::tracking_level() >= NMT_summary, "Must be");
  assert(base != NULL, "Should have been filtered");
  if (track_as_vm()) {
    ThreadCritical tc;
    VirtualMemoryTracker::add_reserved_region((address)base, size, stack, mtThreadStack);
    _thread_count ++;
  } else {
    // Use a slot in mallocMemorySummary for thread stack bookkeeping
    MallocMemorySummary::record_malloc(size, mtThreadStack);
    if (MemTracker::tracking_level() == NMT_detail) {
      ThreadCritical tc;
      assert(_simple_thread_stacks != NULL, "Must be initialized");
      SimpleThreadStackSite site((address)base, size, stack);
      _simple_thread_stacks->add(site);
    }
  }
}

void ThreadStackTracker::delete_thread_stack(void* base, size_t size) {
  assert(MemTracker::tracking_level() >= NMT_summary, "Must be");
  assert(base != NULL, "Should have been filtered");
  if(track_as_vm()) {
    ThreadCritical tc;
    VirtualMemoryTracker::remove_released_region((address)base, size);
    _thread_count--;
  } else {
    // Use a slot in mallocMemorySummary for thread stack bookkeeping
    MallocMemorySummary::record_free(size, mtThreadStack);
    if (MemTracker::tracking_level() == NMT_detail) {
      ThreadCritical tc;
      assert(_simple_thread_stacks != NULL, "Must be initialized");
      SimpleThreadStackSite site((address)base, size, NativeCallStack::empty_stack()); // Fake object just to serve as compare target for delete
      bool removed = _simple_thread_stacks->remove(site);
      assert(removed, "Must exist");
    }
  }
}

bool ThreadStackTracker::walk_simple_thread_stack_site(MallocSiteWalker* walker) {
  if (!track_as_vm()) {
    LinkedListImpl<MallocSite> _sites;
    {
      ThreadCritical tc;
      assert(_simple_thread_stacks != NULL, "Must be initialized");
      LinkedListIterator<SimpleThreadStackSite> itr(_simple_thread_stacks->head());
      const SimpleThreadStackSite* ts = itr.next();
      // Consolidate sites and convert to MallocSites, so we can piggyback into
      // malloc snapshot
      while (ts != NULL) {
        MallocSite site(*ts->call_stack(), mtThreadStack);
        MallocSite* exist = _sites.find(site);
        if (exist != NULL) {
          exist->allocate(ts->size());
        } else {
          site.allocate(ts->size());
          _sites.add(site);
        }
        ts = itr.next();
      }
    }

    // Piggyback to malloc snapshot
    LinkedListIterator<MallocSite> site_itr(_sites.head());
    const MallocSite* s = site_itr.next();
    while (s != NULL) {
      walker->do_malloc_site(s);
      s = site_itr.next();
    }
  }
  return true;
}
