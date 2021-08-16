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

#ifndef SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_INLINE_HPP
#define SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_INLINE_HPP

#include "gc/shared/partialArrayTaskStepper.hpp"

#include "oops/arrayOop.hpp"
#include "runtime/atomic.hpp"

PartialArrayTaskStepper::Step
PartialArrayTaskStepper::start_impl(int length,
                                    int* to_length_addr,
                                    int chunk_size) const {
  assert(chunk_size > 0, "precondition");

  int end = length % chunk_size; // End of initial chunk.
  // Set to's length to end of initial chunk.  Partial tasks use that length
  // field as the start of the next chunk to process.  Must be done before
  // enqueuing partial scan tasks, in case other threads steal any of those
  // tasks.
  //
  // The value of end can be 0, either because of a 0-length array or
  // because length is a multiple of the chunk size.  Both of those are
  // relatively rare and handled in the normal course of the iteration, so
  // not worth doing anything special about here.
  *to_length_addr = end;

  // If the initial chunk is the complete array, then don't need any partial
  // tasks.  Otherwise, start with just one partial task; see new task
  // calculation in next().
  Step result = { end, (length > end) ? 1u : 0u };
  return result;
}

PartialArrayTaskStepper::Step
PartialArrayTaskStepper::start(arrayOop from, arrayOop to, int chunk_size) const {
  return start_impl(from->length(), to->length_addr(), chunk_size);
}

PartialArrayTaskStepper::Step
PartialArrayTaskStepper::next_impl(int length,
                                   int* to_length_addr,
                                   int chunk_size) const {
  assert(chunk_size > 0, "precondition");

  // The start of the next task is in the length field of the to-space object.
  // Atomically increment by the chunk size to claim the associated chunk.
  // Because we limit the number of enqueued tasks to being no more than the
  // number of remaining chunks to process, we can use an atomic add for the
  // claim, rather than a CAS loop.
  int start = Atomic::fetch_and_add(to_length_addr,
                                    chunk_size,
                                    memory_order_relaxed);

  assert(start < length, "invariant: start %d, length %d", start, length);
  assert(((length - start) % chunk_size) == 0,
         "invariant: start %d, length %d, chunk size %d",
         start, length, chunk_size);

  // Determine the number of new tasks to create.
  // Zero-based index for this partial task.  The initial task isn't counted.
  uint task_num = (start / chunk_size);
  // Number of tasks left to process, including this one.
  uint remaining_tasks = (length - start) / chunk_size;
  assert(remaining_tasks > 0, "invariant");
  // Compute number of pending tasks, including this one.  The maximum number
  // of tasks is a function of task_num (N) and _task_fanout (F).
  //   1    : current task
  //   N    : number of preceeding tasks
  //   F*N  : maximum created for preceeding tasks
  // => F*N - N + 1 : maximum number of tasks
  // => (F-1)*N + 1
  assert(_task_limit > 0, "precondition");
  assert(_task_fanout > 0, "precondition");
  uint max_pending = (_task_fanout - 1) * task_num + 1;

  // The actual pending may be less than that.  Bound by remaining_tasks to
  // not overrun.  Also bound by _task_limit to avoid spawning an excessive
  // number of tasks for a large array.  The +1 is to replace the current
  // task with a new task when _task_limit limited.  The pending value may
  // not be what's actually in the queues, because of concurrent task
  // processing.  That's okay; we just need to determine the correct number
  // of tasks to add for this task.
  uint pending = MIN3(max_pending, remaining_tasks, _task_limit);
  uint ncreate = MIN2(_task_fanout, MIN2(remaining_tasks, _task_limit + 1) - pending);
  Step result = { start, ncreate };
  return result;
}

PartialArrayTaskStepper::Step
PartialArrayTaskStepper::next(arrayOop from, arrayOop to, int chunk_size) const {
  return next_impl(from->length(), to->length_addr(), chunk_size);
}

#endif // SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_INLINE_HPP
