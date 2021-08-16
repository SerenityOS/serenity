/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_THREADHEAPSAMPLER_HPP
#define SHARE_RUNTIME_THREADHEAPSAMPLER_HPP

#include "memory/allocation.hpp"

class ThreadHeapSampler {
 private:
  size_t _bytes_until_sample;
  // Cheap random number generator
  static uint64_t _rnd;

  static volatile int _sampling_interval;

  void pick_next_geometric_sample();
  void pick_next_sample(size_t overflowed_bytes = 0);

  static double fast_log2(const double& d);
  uint64_t next_random(uint64_t rnd);

 public:
  ThreadHeapSampler() {
    _rnd = static_cast<uint32_t>(reinterpret_cast<uintptr_t>(this));
    if (_rnd == 0) {
      _rnd = 1;
    }

    // Call this after _rnd is initialized to initialize _bytes_until_sample.
    pick_next_sample();
  }

  size_t bytes_until_sample()                    { return _bytes_until_sample;   }
  void set_bytes_until_sample(size_t bytes)      { _bytes_until_sample = bytes;  }

  void check_for_sampling(oop obj, size_t size_in_bytes, size_t bytes_allocated_before);

  static void set_sampling_interval(int sampling_interval);
  static int get_sampling_interval();
};

#endif // SHARE_RUNTIME_THREADHEAPSAMPLER_HPP
