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

#ifndef SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_HPP
#define SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_HPP

#include "oops/arrayOop.hpp"
#include "utilities/globalDefinitions.hpp"

// Helper for handling PartialArrayTasks.
//
// When an array is large, we want to split it up into chunks that can be
// processed in parallel.  Each task (implicitly) represents such a chunk.
// We can enqueue multiple tasks at the same time.  We want to enqueue
// enough tasks to benefit from the available parallelism, while not so many
// as to substantially expand the task queues.
//
// A task directly refers to the from-space array.  The from-space array's
// forwarding pointer refers to the associated to-space array, and its
// length is the actual length. The to-space array's length field is used to
// indicate processing progress.  It is the starting index of the next chunk
// to process, or equals the actual length when there are no more chunks to
// be processed.
class PartialArrayTaskStepper {
public:
  PartialArrayTaskStepper(uint n_workers);

  struct Step {
    int _index;                 // Array index for the step.
    uint _ncreate;              // Number of new tasks to create.
  };

  // Set to's length to the end of the initial chunk, which is the start of
  // the first partial task if the array is large enough to need splitting.
  // Returns a Step with _index being that index and _ncreate being the
  // initial number of partial tasks to enqueue.
  inline Step start(arrayOop from, arrayOop to, int chunk_size) const;

  // Increment to's length by chunk_size to claim the next chunk.  Returns a
  // Step with _index being the starting index of the claimed chunk and
  // _ncreate being the number of additional partial tasks to enqueue.
  // precondition: chunk_size must be the same as used to start the task sequence.
  inline Step next(arrayOop from, arrayOop to, int chunk_size) const;

  class TestSupport;            // For unit tests

private:
  // Limit on the number of partial array tasks to create for a given array.
  uint _task_limit;
  // Maximum number of new tasks to create when processing an existing task.
  uint _task_fanout;

  // Split start/next into public part dealing with oops and private
  // impl dealing with lengths and pointers to lengths, for unit testing.
  // length is the actual length obtained from the from-space object.
  // to_length_addr is the address of the to-space object's length value.
  inline Step start_impl(int length, int* to_length_addr, int chunk_size) const;
  inline Step next_impl(int length, int* to_length_addr, int chunk_size) const;
};

#endif // SHARE_GC_SHARED_PARTIALARRAYTASKSTEPPER_HPP
