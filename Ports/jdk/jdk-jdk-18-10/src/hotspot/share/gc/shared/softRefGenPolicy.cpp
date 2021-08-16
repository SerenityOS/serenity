/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/adaptiveSizePolicy.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/softRefGenPolicy.hpp"

void SoftRefGenPolicy::cleared_all_soft_refs() {
  // If near gc overhear limit, continue to clear SoftRefs.  SoftRefs may
  // have been cleared in the last collection but if the gc overhear
  // limit continues to be near, SoftRefs should still be cleared.
  AdaptiveSizePolicy* size_policy = GenCollectedHeap::heap()->size_policy();
  if (size_policy != NULL) {
    set_should_clear_all_soft_refs(size_policy->gc_overhead_limit_near());
  }

  SoftRefPolicy::cleared_all_soft_refs();
}
