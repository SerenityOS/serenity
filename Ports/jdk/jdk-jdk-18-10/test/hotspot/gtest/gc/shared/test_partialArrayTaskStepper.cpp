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
#include "gc/shared/partialArrayTaskStepper.inline.hpp"
#include "memory/allStatic.hpp"
#include "unittest.hpp"

using Step = PartialArrayTaskStepper::Step;
using Stepper = PartialArrayTaskStepper;

class PartialArrayTaskStepper::TestSupport : AllStatic {
public:
  static Step start(const Stepper* stepper,
                    int length,
                    int* to_length_addr,
                    uint chunk_size) {
    return stepper->start_impl(length, to_length_addr, chunk_size);
  }

  static Step next(const Stepper* stepper,
                   int length,
                   int* to_length_addr,
                   uint chunk_size) {
    return stepper->next_impl(length, to_length_addr, chunk_size);
  }
};

using StepperSupport = PartialArrayTaskStepper::TestSupport;

static int simulate(const Stepper* stepper,
                    int length,
                    int* to_length_addr,
                    uint chunk_size) {
  Step init = StepperSupport::start(stepper, length, to_length_addr, chunk_size);
  uint queue_count = init._ncreate;
  int task = 0;
  for ( ; queue_count > 0; ++task) {
    --queue_count;
    Step step = StepperSupport::next(stepper, length, to_length_addr, chunk_size);
    queue_count += step._ncreate;
  }
  return task;
}

static void run_test(int length, int chunk_size, uint n_workers) {
  const PartialArrayTaskStepper stepper(n_workers);
  int to_length;
  int tasks = simulate(&stepper, length, &to_length, chunk_size);
  ASSERT_EQ(length, to_length);
  ASSERT_EQ(tasks, length / chunk_size);
}

TEST(PartialArrayTaskStepperTest, doit) {
  for (int chunk_size = 50; chunk_size <= 500; chunk_size += 50) {
    for (uint n_workers = 1; n_workers <= 256; n_workers = (n_workers * 3 / 2 + 1)) {
      for (int length = 0; length <= 1000000; length = (length * 2 + 1)) {
        run_test(length, chunk_size, n_workers);
      }
      // Ensure we hit boundary cases for length % chunk_size == 0.
      for (uint i = 0; i < 2 * n_workers; ++i) {
        run_test(i * chunk_size, chunk_size, n_workers);
      }
    }
  }
}
