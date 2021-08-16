/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/partialArrayTaskStepper.hpp"
#include "oops/arrayOop.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"

static uint compute_task_limit(uint n_workers) {
  // Don't need more than n_workers tasks at a time.  But allowing up to
  // that maximizes available parallelism.
  return n_workers;
}

static uint compute_task_fanout(uint task_limit) {
  assert(task_limit > 0, "precondition");
  // There is a tradeoff between providing parallelism more quickly and
  // number of enqueued tasks.  A constant fanout may be too slow when
  // parallelism (and so task_limit) is large.  A constant fraction might
  // be overly eager.  Using log2 attempts to balance between those.
  uint result = log2i(task_limit);
  // result must be > 0.  result should be > 1 if task_limit > 1, to
  // provide some potentially parallel tasks.  But don't just +1 to
  // avoid otherwise increasing rate of task generation.
  if (result < 2) ++result;
  return result;
}

PartialArrayTaskStepper::PartialArrayTaskStepper(uint n_workers) :
  _task_limit(compute_task_limit(n_workers)),
  _task_fanout(compute_task_fanout(_task_limit))
{}
