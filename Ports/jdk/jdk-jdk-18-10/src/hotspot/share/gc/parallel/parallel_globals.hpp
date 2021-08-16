/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PARALLEL_GLOBALS_HPP
#define SHARE_GC_PARALLEL_PARALLEL_GLOBALS_HPP

#define GC_PARALLEL_FLAGS(develop,                                          \
                          develop_pd,                                       \
                          product,                                          \
                          product_pd,                                       \
                          notproduct,                                       \
                          range,                                            \
                          constraint)                                       \
  product(uintx, HeapMaximumCompactionInterval, 20,                         \
          "How often should we maximally compact the heap (not allowing "   \
          "any dead space)")                                                \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, HeapFirstMaximumCompactionCount, 3,                        \
          "The collection count for the first maximum compaction")          \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, UseMaximumCompactionOnSystemGC, true,                       \
          "Use maximum compaction in the Parallel Old garbage collector "   \
          "for a system GC")                                                \
                                                                            \
  product(size_t, ParallelOldDeadWoodLimiterMean, 50,                       \
          "The mean used by the parallel compact dead wood "                \
          "limiter (a number between 0-100)")                               \
          range(0, 100)                                                     \
                                                                            \
  product(size_t, ParallelOldDeadWoodLimiterStdDev, 80,                     \
          "The standard deviation used by the parallel compact dead wood "  \
          "limiter (a number between 0-100)")                               \
          range(0, 100)                                                     \
                                                                            \
  develop(uintx, GCWorkerDelayMillis, 0,                                    \
          "Delay in scheduling GC workers (in milliseconds)")               \
                                                                            \
  product(bool, PSChunkLargeArrays, true,                                   \
          "Process large arrays in chunks")

// end of GC_PARALLEL_FLAGS

#endif // SHARE_GC_PARALLEL_PARALLEL_GLOBALS_HPP
