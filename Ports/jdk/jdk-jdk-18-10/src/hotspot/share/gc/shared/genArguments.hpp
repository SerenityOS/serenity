/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GENARGUMENTS_HPP
#define SHARE_GC_SHARED_GENARGUMENTS_HPP

#include "gc/shared/gcArguments.hpp"
#include "utilities/debug.hpp"

extern size_t MinNewSize;

extern size_t MinOldSize;
extern size_t MaxOldSize;

extern size_t GenAlignment;

class GenArguments : public GCArguments {
  friend class TestGenCollectorPolicy; // Testing
private:
  virtual void initialize_alignments();
  virtual void initialize_size_info();

  // Return the (conservative) maximum heap alignment
  virtual size_t conservative_max_heap_alignment();

  DEBUG_ONLY(void assert_flags();)
  DEBUG_ONLY(void assert_size_info();)

  static size_t scale_by_NewRatio_aligned(size_t base_size, size_t alignment);

protected:
  virtual void initialize_heap_flags_and_sizes();
};

#endif // SHARE_GC_SHARED_GENARGUMENTS_HPP
