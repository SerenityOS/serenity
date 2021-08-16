/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_AGETABLE_HPP
#define SHARE_GC_SHARED_AGETABLE_HPP

#include "oops/markWord.hpp"
#include "oops/oop.hpp"
#include "runtime/perfDataTypes.hpp"

/* Copyright (c) 1992, 2021, Oracle and/or its affiliates, and Stanford University.
   See the LICENSE file for license information. */

// Age table for adaptive feedback-mediated tenuring (scavenging)
//
// Note: all sizes are in oops

class AgeTable {
  friend class VMStructs;

 public:
  // constants
  enum { table_size = markWord::max_age + 1 };

  // instance variables
  size_t sizes[table_size];

  // constructor.  "global" indicates that this is the global age table
  // (as opposed to gc-thread-local)
  AgeTable(bool global = true);

  // clear table
  void clear();

  // add entry
  inline void add(oop p, size_t oop_size);

  void add(uint age, size_t oop_size) {
    assert(age > 0 && age < table_size, "invalid age of object");
    sizes[age] += oop_size;
  }

  // Merge another age table with the current one.  Used
  // for parallel young generation gc.
  void merge(AgeTable* subTable);

  // Calculate new tenuring threshold based on age information.
  uint compute_tenuring_threshold(size_t desired_survivor_size);
  void print_age_table(uint tenuring_threshold);

 private:

  PerfVariable* _perf_sizes[table_size];
};

#endif // SHARE_GC_SHARED_AGETABLE_HPP
