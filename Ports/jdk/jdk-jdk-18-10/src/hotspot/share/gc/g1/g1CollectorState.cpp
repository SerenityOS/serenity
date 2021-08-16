/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectorState.hpp"
#include "gc/g1/g1GCPauseType.hpp"

G1GCPauseType G1CollectorState::young_gc_pause_type(bool concurrent_operation_is_full_mark) const {
  assert(!in_full_gc(), "must be");
  if (in_concurrent_start_gc()) {
    assert(!in_young_gc_before_mixed(), "must be");
    return concurrent_operation_is_full_mark ? G1GCPauseType::ConcurrentStartMarkGC :
                                               G1GCPauseType::ConcurrentStartUndoGC;
  } else if (in_young_gc_before_mixed()) {
    assert(!in_concurrent_start_gc(), "must be");
    return G1GCPauseType::LastYoungGC;
  } else if (in_mixed_phase()) {
    assert(!in_concurrent_start_gc(), "must be");
    assert(!in_young_gc_before_mixed(), "must be");
    return G1GCPauseType::MixedGC;
  } else {
    assert(!in_concurrent_start_gc(), "must be");
    assert(!in_young_gc_before_mixed(), "must be");
    return G1GCPauseType::YoungGC;
  }
}
