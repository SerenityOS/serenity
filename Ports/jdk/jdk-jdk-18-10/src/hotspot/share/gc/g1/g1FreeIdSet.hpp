/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FREEIDSET_HPP
#define SHARE_GC_G1_G1FREEIDSET_HPP

#include "runtime/semaphore.hpp"
#include "utilities/globalDefinitions.hpp"

// Represents a set of small integer ids, from which elements can be
// temporarily allocated for exclusive use.  The ids are in a
// contiguous range from 'start' to 'start + size'.  Used to obtain a
// distinct worker_id value for a mutator thread that doesn't normally
// have such an id.
class G1FreeIdSet {
  Semaphore _sem;
  uint* _next;
  uint _start;
  uint _size;
  uintx _head_index_mask;
  volatile uintx _head;

  uint head_index(uintx head) const;
  uintx make_head(uint index, uintx old_head) const;

  NONCOPYABLE(G1FreeIdSet);

public:
  G1FreeIdSet(uint start, uint size);
  ~G1FreeIdSet();

  // Returns an unclaimed parallel id (waiting for one to be released if
  // necessary).  Must not safepoint while holding a claimed id.
  uint claim_par_id();

  void release_par_id(uint id);

  struct TestSupport;           // For unit test access.
};

#endif // SHARE_GC_G1_G1FREEIDSET_HPP
