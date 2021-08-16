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

#ifndef SHARE_GC_G1_G1HRPRINTER_HPP
#define SHARE_GC_G1_G1HRPRINTER_HPP

#include "gc/g1/heapRegion.hpp"
#include "logging/log.hpp"

#define SKIP_RETIRED_FULL_REGIONS 1

class FreeRegionList;

class G1HRPrinter {

private:

  // Print an action event.
  static void print(const char* action, HeapRegion* hr) {
    log_trace(gc, region)("G1HR %s(%s) [" PTR_FORMAT ", " PTR_FORMAT ", " PTR_FORMAT "]",
                          action, hr->get_type_str(), p2i(hr->bottom()), p2i(hr->top()), p2i(hr->end()));
  }

public:
  // In some places we iterate over a list in order to generate output
  // for the list's elements. By exposing this we can avoid this
  // iteration if the printer is not active.
  const bool is_active() { return log_is_enabled(Trace, gc, region); }

  // The methods below are convenient wrappers for the print() method.

  void alloc(HeapRegion* hr, bool force = false) {
    if (is_active()) {
      print((force) ? "ALLOC-FORCE" : "ALLOC", hr);
    }
  }

  void retire(HeapRegion* hr) {
    if (is_active()) {
      if (!SKIP_RETIRED_FULL_REGIONS || hr->top() < hr->end()) {
        print("RETIRE", hr);
      }
    }
  }

  void reuse(HeapRegion* hr) {
    if (is_active()) {
      print("REUSE", hr);
    }
  }

  void cset(HeapRegion* hr) {
    if (is_active()) {
      print("CSET", hr);
    }
  }

  void evac_failure(HeapRegion* hr) {
    if (is_active()) {
      print("EVAC-FAILURE", hr);
    }
  }

  void cleanup(HeapRegion* hr) {
    if (is_active()) {
      print("CLEANUP", hr);
    }
  }

  void cleanup(FreeRegionList* free_list);

  void post_compaction(HeapRegion* hr) {
    if (is_active()) {
      print("POST-COMPACTION", hr);
    }
  }

  void commit(HeapRegion* hr) {
    if (is_active()) {
      print("COMMIT", hr);
    }
  }

  void active(HeapRegion* hr) {
    if (is_active()) {
      print("ACTIVE", hr);
    }
  }

  void inactive(HeapRegion* hr) {
    if (is_active()) {
      print("INACTIVE", hr);
    }
  }

  void uncommit(HeapRegion* hr) {
    if (is_active()) {
      print("UNCOMMIT", hr);
    }
  }
};

#endif // SHARE_GC_G1_G1HRPRINTER_HPP
