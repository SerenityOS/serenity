/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GC_GLOBALS_HPP
#define SHARE_GC_SHARED_GC_GLOBALS_HPP

#include "runtime/globals_shared.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_EPSILONGC
#include "gc/epsilon/epsilon_globals.hpp"
#endif
#if INCLUDE_G1GC
#include "gc/g1/g1_globals.hpp"
#endif
#if INCLUDE_PARALLELGC
#include "gc/parallel/parallel_globals.hpp"
#endif
#if INCLUDE_SERIALGC
#include "gc/serial/serial_globals.hpp"
#endif
#if INCLUDE_SHENANDOAHGC
#include "gc/shenandoah/shenandoah_globals.hpp"
#endif
#if INCLUDE_ZGC
#include "gc/z/z_globals.hpp"
#endif

#define GC_FLAGS(develop,                                                   \
                 develop_pd,                                                \
                 product,                                                   \
                 product_pd,                                                \
                 notproduct,                                                \
                 range,                                                     \
                 constraint)                                                \
                                                                            \
  EPSILONGC_ONLY(GC_EPSILON_FLAGS(                                          \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  G1GC_ONLY(GC_G1_FLAGS(                                                    \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  PARALLELGC_ONLY(GC_PARALLEL_FLAGS(                                        \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  SERIALGC_ONLY(GC_SERIAL_FLAGS(                                            \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  SHENANDOAHGC_ONLY(GC_SHENANDOAH_FLAGS(                                    \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  ZGC_ONLY(GC_Z_FLAGS(                                                      \
    develop,                                                                \
    develop_pd,                                                             \
    product,                                                                \
    product_pd,                                                             \
    notproduct,                                                             \
    range,                                                                  \
    constraint))                                                            \
                                                                            \
  /* gc */                                                                  \
                                                                            \
  product(bool, UseSerialGC, false,                                         \
          "Use the Serial garbage collector")                               \
                                                                            \
  product(bool, UseG1GC, false,                                             \
          "Use the Garbage-First garbage collector")                        \
                                                                            \
  product(bool, UseParallelGC, false,                                       \
          "Use the Parallel garbage collector.")                            \
                                                                            \
  product(bool, UseEpsilonGC, false, EXPERIMENTAL,                          \
          "Use the Epsilon (no-op) garbage collector")                      \
                                                                            \
  product(bool, UseZGC, false,                                              \
          "Use the Z garbage collector")                                    \
                                                                            \
  product(bool, UseShenandoahGC, false,                                     \
          "Use the Shenandoah garbage collector")                           \
                                                                            \
  product(uint, ParallelGCThreads, 0,                                       \
          "Number of parallel threads parallel gc will use")                \
          constraint(ParallelGCThreadsConstraintFunc,AfterErgo)             \
                                                                            \
  product(bool, UseDynamicNumberOfGCThreads, true,                          \
          "Dynamically choose the number of threads up to a maximum of "    \
          "ParallelGCThreads parallel collectors will use for garbage "     \
          "collection work")                                                \
                                                                            \
  product(bool, InjectGCWorkerCreationFailure, false, DIAGNOSTIC,           \
             "Inject thread creation failures for "                         \
             "UseDynamicNumberOfGCThreads")                                 \
                                                                            \
  product(size_t, HeapSizePerGCThread, ScaleForWordSize(32*M),              \
          "Size of heap (bytes) per GC thread used in calculating the "     \
          "number of GC threads")                                           \
          constraint(VMPageSizeConstraintFunc, AtParse)                     \
                                                                            \
  product(uint, ConcGCThreads, 0,                                           \
          "Number of threads concurrent gc will use")                       \
                                                                            \
  product(bool, AlwaysTenure, false,                                        \
          "Always tenure objects in eden (ParallelGC only)")                \
                                                                            \
  product(bool, NeverTenure, false,                                         \
          "Never tenure objects in eden, may tenure on overflow "           \
          "(ParallelGC only)")                                              \
                                                                            \
  product(bool, ScavengeBeforeFullGC, true,                                 \
          "Scavenge youngest generation before each full GC.")              \
                                                                            \
  product(bool, ExplicitGCInvokesConcurrent, false,                         \
          "A System.gc() request invokes a concurrent collection; "         \
          "(effective only when using concurrent collectors)")              \
                                                                            \
  product(uintx, GCLockerEdenExpansionPercent, 5,                           \
          "How much the GC can expand the eden by while the GC locker "     \
          "is active (as a percentage)")                                    \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, GCLockerRetryAllocationCount, 2, DIAGNOSTIC,               \
          "Number of times to retry allocations when "                      \
          "blocked by the GC locker")                                       \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, ParallelGCBufferWastePct, 10,                              \
          "Wasted fraction of parallel allocation buffer")                  \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, TargetPLABWastePct, 10,                                    \
          "Target wasted space in last buffer as percent of overall "       \
          "allocation")                                                     \
          range(1, 100)                                                     \
                                                                            \
  product(uintx, PLABWeight, 75,                                            \
          "Percentage (0-100) used to weight the current sample when "      \
          "computing exponentially decaying average for ResizePLAB")        \
          range(0, 100)                                                     \
                                                                            \
  product(bool, ResizePLAB, true,                                           \
          "Dynamically resize (survivor space) promotion LAB's")            \
                                                                            \
  product(int, ParGCArrayScanChunk, 50,                                     \
          "Scan a subset of object array and push remainder, if array is "  \
          "bigger than this")                                               \
          range(1, max_jint/3)                                              \
                                                                            \
                                                                            \
  product(bool, AlwaysPreTouch, false,                                      \
          "Force all freshly committed pages to be pre-touched")            \
                                                                            \
  product_pd(size_t, PreTouchParallelChunkSize,                             \
          "Per-thread chunk size for parallel memory pre-touch.")           \
          range(4*K, SIZE_MAX / 2)                                          \
                                                                            \
  /* where does the range max value of (max_jint - 1) come from? */         \
  product(size_t, MarkStackSizeMax, NOT_LP64(4*M) LP64_ONLY(512*M),         \
          "Maximum size of marking stack")                                  \
          range(1, (max_jint - 1))                                          \
                                                                            \
  product(size_t, MarkStackSize, NOT_LP64(32*K) LP64_ONLY(4*M),             \
          "Size of marking stack")                                          \
          constraint(MarkStackSizeConstraintFunc,AfterErgo)                 \
          range(1, (max_jint - 1))                                          \
                                                                            \
  product(intx, RefDiscoveryPolicy, 0,                                      \
          "Select type of reference discovery policy: "                     \
          "reference-based(0) or referent-based(1)")                        \
          range(ReferenceProcessor::DiscoveryPolicyMin,                     \
                ReferenceProcessor::DiscoveryPolicyMax)                     \
                                                                            \
  product(bool, ParallelRefProcEnabled, false,                              \
          "Enable parallel reference processing whenever possible")         \
                                                                            \
  product(bool, ParallelRefProcBalancingEnabled, true,                      \
          "Enable balancing of reference processing queues")                \
                                                                            \
  product(size_t, ReferencesPerThread, 1000, EXPERIMENTAL,                  \
               "Ergonomically start one thread for this amount of "         \
               "references for reference processing if "                    \
               "ParallelRefProcEnabled is true. Specify 0 to disable and "  \
               "use all threads.")                                          \
                                                                            \
  product(uintx, InitiatingHeapOccupancyPercent, 45,                        \
          "The percent occupancy (IHOP) of the current old generation "     \
          "capacity above which a concurrent mark cycle will be initiated " \
          "Its value may change over time if adaptive IHOP is enabled, "    \
          "otherwise the value remains constant. "                          \
          "In the latter case a value of 0 will result as frequent as "     \
          "possible concurrent marking cycles. A value of 100 disables "    \
          "concurrent marking. "                                            \
          "Fragmentation waste in the old generation is not considered "    \
          "free space in this calculation. (G1 collector only)")            \
          range(0, 100)                                                     \
                                                                            \
  notproduct(bool, ScavengeALot, false,                                     \
          "Force scavenge at every Nth exit from the runtime system "       \
          "(N=ScavengeALotInterval)")                                       \
                                                                            \
  develop(bool, FullGCALot, false,                                          \
          "Force full gc at every Nth exit from the runtime system "        \
          "(N=FullGCALotInterval)")                                         \
                                                                            \
  notproduct(bool, GCALotAtAllSafepoints, false,                            \
          "Enforce ScavengeALot/GCALot at all potential safepoints")        \
                                                                            \
  notproduct(bool, PromotionFailureALot, false,                             \
          "Use promotion failure handling on every youngest generation "    \
          "collection")                                                     \
                                                                            \
  develop(uintx, PromotionFailureALotCount, 1000,                           \
          "Number of promotion failures occurring at PLAB promotion "       \
          "attempts at young collectors")                                   \
                                                                            \
  develop(uintx, PromotionFailureALotInterval, 5,                           \
          "Total collections between promotion failures a lot")             \
                                                                            \
  product(uintx, WorkStealingSleepMillis, 1, EXPERIMENTAL,                  \
          "Sleep time when sleep is used for yields")                       \
                                                                            \
  product(uintx, WorkStealingYieldsBeforeSleep, 5000, EXPERIMENTAL,         \
          "Number of yields before a sleep is done during work stealing")   \
                                                                            \
  product(uintx, WorkStealingHardSpins, 4096, EXPERIMENTAL,                 \
          "Number of iterations in a spin loop between checks on "          \
          "time out of hard spin")                                          \
                                                                            \
  product(uintx, WorkStealingSpinToYieldRatio, 10, EXPERIMENTAL,            \
          "Ratio of hard spins to calls to yield")                          \
                                                                            \
  develop(uintx, ObjArrayMarkingStride, 2048,                               \
          "Number of object array elements to push onto the marking stack " \
          "before pushing a continuation entry")                            \
                                                                            \
  develop(bool, MetadataAllocationFailALot, false,                          \
          "Fail metadata allocations at intervals controlled by "           \
          "MetadataAllocationFailALotInterval")                             \
                                                                            \
  develop(uintx, MetadataAllocationFailALotInterval, 1000,                  \
          "Metadata allocation failure a lot interval")                     \
                                                                            \
  product_pd(bool, NeverActAsServerClassMachine,                            \
          "Never act like a server-class machine")                          \
                                                                            \
  product(bool, AlwaysActAsServerClassMachine, false,                       \
          "Always act like a server-class machine")                         \
                                                                            \
  product_pd(uint64_t, MaxRAM,                                              \
          "Real memory size (in bytes) used to set maximum heap size")      \
          range(0, 0XFFFFFFFFFFFFFFFF)                                      \
                                                                            \
  product(bool, AggressiveHeap, false,                                      \
          "Optimize heap options for long-running memory intensive apps")   \
                                                                            \
  product(size_t, ErgoHeapSizeLimit, 0,                                     \
          "Maximum ergonomically set heap size (in bytes); zero means use " \
          "MaxRAM * MaxRAMPercentage / 100")                                \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, MaxRAMFraction, 4,                                         \
          "Maximum fraction (1/n) of real memory used for maximum heap "    \
          "size. "                                                          \
          "Deprecated, use MaxRAMPercentage instead")                       \
          range(1, max_uintx)                                               \
                                                                            \
  product(uintx, MinRAMFraction, 2,                                         \
          "Minimum fraction (1/n) of real memory used for maximum heap "    \
          "size on systems with small physical memory size. "               \
          "Deprecated, use MinRAMPercentage instead")                       \
          range(1, max_uintx)                                               \
                                                                            \
  product(uintx, InitialRAMFraction, 64,                                    \
          "Fraction (1/n) of real memory used for initial heap size. "      \
          "Deprecated, use InitialRAMPercentage instead")                   \
          range(1, max_uintx)                                               \
                                                                            \
  product(double, MaxRAMPercentage, 25.0,                                   \
          "Maximum percentage of real memory used for maximum heap size")   \
          range(0.0, 100.0)                                                 \
                                                                            \
  product(double, MinRAMPercentage, 50.0,                                   \
          "Minimum percentage of real memory used for maximum heap"         \
          "size on systems with small physical memory size")                \
          range(0.0, 100.0)                                                 \
                                                                            \
  product(double, InitialRAMPercentage, 1.5625,                             \
          "Percentage of real memory used for initial heap size")           \
          range(0.0, 100.0)                                                 \
                                                                            \
  product(int, ActiveProcessorCount, -1,                                    \
          "Specify the CPU count the VM should use and report as active")   \
                                                                            \
  develop(uintx, MaxVirtMemFraction, 2,                                     \
          "Maximum fraction (1/n) of virtual memory used for ergonomically "\
          "determining maximum heap size")                                  \
          range(1, max_uintx)                                               \
                                                                            \
  product(bool, UseAdaptiveSizePolicy, true,                                \
          "Use adaptive generation sizing policies")                        \
                                                                            \
  product(bool, UsePSAdaptiveSurvivorSizePolicy, true,                      \
          "Use adaptive survivor sizing policies")                          \
                                                                            \
  product(bool, UseAdaptiveGenerationSizePolicyAtMinorCollection, true,     \
          "Use adaptive young-old sizing policies at minor collections")    \
                                                                            \
  product(bool, UseAdaptiveGenerationSizePolicyAtMajorCollection, true,     \
          "Use adaptive young-old sizing policies at major collections")    \
                                                                            \
  product(bool, UseAdaptiveSizePolicyWithSystemGC, false,                   \
          "Include statistics from System.gc() for adaptive size policy")   \
                                                                            \
  develop(intx, PSAdaptiveSizePolicyResizeVirtualSpaceAlot, -1,             \
          "Resize the virtual spaces of the young or old generations")      \
          range(-1, 1)                                                      \
                                                                            \
  product(uintx, AdaptiveSizeThroughPutPolicy, 0,                           \
          "Policy for changing generation size for throughput goals")       \
          range(0, 1)                                                       \
                                                                            \
  product(uintx, AdaptiveSizePolicyInitializingSteps, 20,                   \
          "Number of steps where heuristics is used before data is used")   \
          range(0, max_uintx)                                               \
                                                                            \
  develop(uintx, AdaptiveSizePolicyReadyThreshold, 5,                       \
          "Number of collections before the adaptive sizing is started")    \
                                                                            \
  product(uintx, AdaptiveSizePolicyOutputInterval, 0,                       \
          "Collection interval for printing information; zero means never") \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, UseAdaptiveSizePolicyFootprintGoal, true,                   \
          "Use adaptive minimum footprint as a goal")                       \
                                                                            \
  product(uintx, AdaptiveSizePolicyWeight, 10,                              \
          "Weight given to exponential resizing, between 0 and 100")        \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, AdaptiveTimeWeight,       25,                              \
          "Weight given to time in adaptive policy, between 0 and 100")     \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, PausePadding, 1,                                           \
          "How much buffer to keep for pause time")                         \
          range(0, max_juint)                                               \
                                                                            \
  product(uintx, PromotedPadding, 3,                                        \
          "How much buffer to keep for promotion failure")                  \
          range(0, max_juint)                                               \
                                                                            \
  product(uintx, SurvivorPadding, 3,                                        \
          "How much buffer to keep for survivor overflow")                  \
          range(0, max_juint)                                               \
                                                                            \
  product(uintx, ThresholdTolerance, 10,                                    \
          "Allowed collection cost difference between generations")         \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, AdaptiveSizePolicyCollectionCostMargin, 50,                \
          "If collection costs are within margin, reduce both by full "     \
          "delta")                                                          \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, YoungGenerationSizeIncrement, 20,                          \
          "Adaptive size percentage change in young generation")            \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, YoungGenerationSizeSupplement, 80,                         \
          "Supplement to YoungedGenerationSizeIncrement used at startup")   \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, YoungGenerationSizeSupplementDecay, 8,                     \
          "Decay factor to YoungedGenerationSizeSupplement")                \
          range(1, max_uintx)                                               \
                                                                            \
  product(uintx, TenuredGenerationSizeIncrement, 20,                        \
          "Adaptive size percentage change in tenured generation")          \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, TenuredGenerationSizeSupplement, 80,                       \
          "Supplement to TenuredGenerationSizeIncrement used at startup")   \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, TenuredGenerationSizeSupplementDecay, 2,                   \
          "Decay factor to TenuredGenerationSizeIncrement")                 \
          range(1, max_uintx)                                               \
                                                                            \
  product(uintx, MaxGCPauseMillis, max_uintx - 1,                           \
          "Adaptive size policy maximum GC pause time goal in millisecond, "\
          "or (G1 Only) the maximum GC time per MMU time slice")            \
          range(1, max_uintx - 1)                                           \
          constraint(MaxGCPauseMillisConstraintFunc,AfterErgo)              \
                                                                            \
  product(uintx, GCPauseIntervalMillis, 0,                                  \
          "Time slice for MMU specification")                               \
          constraint(GCPauseIntervalMillisConstraintFunc,AfterErgo)         \
                                                                            \
  product(uintx, MaxGCMinorPauseMillis, max_uintx,                          \
          "Adaptive size policy maximum GC minor pause time goal "          \
          "in millisecond")                                                 \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, GCTimeRatio, 99,                                           \
          "Adaptive size policy application time to GC time ratio")         \
          range(0, max_juint)                                               \
                                                                            \
  product(uintx, AdaptiveSizeDecrementScaleFactor, 4,                       \
          "Adaptive size scale down factor for shrinking")                  \
          range(1, max_uintx)                                               \
                                                                            \
  product(bool, UseAdaptiveSizeDecayMajorGCCost, true,                      \
          "Adaptive size decays the major cost for long major intervals")   \
                                                                            \
  product(uintx, AdaptiveSizeMajorGCDecayTimeScale, 10,                     \
          "Time scale over which major costs decay")                        \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, MinSurvivorRatio, 3,                                       \
          "Minimum ratio of young generation/survivor space size")          \
          range(3, max_uintx)                                               \
                                                                            \
  product(uintx, InitialSurvivorRatio, 8,                                   \
          "Initial ratio of young generation/survivor space size")          \
          range(0, max_uintx)                                               \
                                                                            \
  product(size_t, BaseFootPrintEstimate, 256*M,                             \
          "Estimate of footprint other than Java Heap")                     \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, UseGCOverheadLimit, true,                                   \
          "Use policy to limit of proportion of time spent in GC "          \
          "before an OutOfMemory error is thrown")                          \
                                                                            \
  product(uintx, GCTimeLimit, 98,                                           \
          "Limit of the proportion of time spent in GC before "             \
          "an OutOfMemoryError is thrown (used with GCHeapFreeLimit)")      \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, GCHeapFreeLimit, 2,                                        \
          "Minimum percentage of free space after a full GC before an "     \
          "OutOfMemoryError is thrown (used with GCTimeLimit)")             \
          range(0, 100)                                                     \
                                                                            \
  develop(uintx, GCOverheadLimitThreshold, 5,                               \
          "Number of consecutive collections before gc time limit fires")   \
          range(1, max_uintx)                                               \
                                                                            \
  product(intx, PrefetchCopyIntervalInBytes, -1,                            \
          "How far ahead to prefetch destination area (<= 0 means off)")    \
          range(-1, max_jint)                                               \
                                                                            \
  product(intx, PrefetchScanIntervalInBytes, -1,                            \
          "How far ahead to prefetch scan area (<= 0 means off)")           \
          range(-1, max_jint)                                               \
                                                                            \
  product(intx, PrefetchFieldsAhead, -1,                                    \
          "How many fields ahead to prefetch in oop scan (<= 0 means off)") \
          range(-1, max_jint)                                               \
                                                                            \
  product(bool, VerifyDuringStartup, false, DIAGNOSTIC,                     \
          "Verify memory system before executing any Java code "            \
          "during VM initialization")                                       \
                                                                            \
  product(bool, VerifyBeforeExit, trueInDebug, DIAGNOSTIC,                  \
          "Verify system before exiting")                                   \
                                                                            \
  product(bool, VerifyBeforeGC, false, DIAGNOSTIC,                          \
          "Verify memory system before GC")                                 \
                                                                            \
  product(bool, VerifyAfterGC, false, DIAGNOSTIC,                           \
          "Verify memory system after GC")                                  \
                                                                            \
  product(bool, VerifyDuringGC, false, DIAGNOSTIC,                          \
          "Verify memory system during GC (between phases)")                \
                                                                            \
  product(bool, VerifyArchivedFields, trueInDebug, DIAGNOSTIC,              \
          "Verify memory when archived oop fields are loaded from CDS)")    \
                                                                            \
  product(ccstrlist, VerifyGCType, "", DIAGNOSTIC,                          \
             "GC type(s) to verify when Verify*GC is enabled."              \
             "Available types are collector specific.")                     \
                                                                            \
  product(ccstrlist, VerifySubSet, "", DIAGNOSTIC,                          \
          "Memory sub-systems to verify when Verify*GC flag(s) "            \
          "are enabled. One or more sub-systems can be specified "          \
          "in a comma separated string. Sub-systems are: "                  \
          "threads, heap, symbol_table, string_table, codecache, "          \
          "dictionary, classloader_data_graph, metaspace, jni_handles, "    \
          "codecache_oops, resolved_method_table, stringdedup")             \
                                                                            \
  product(bool, GCParallelVerificationEnabled, true, DIAGNOSTIC,            \
          "Enable parallel memory system verification")                     \
                                                                            \
  product(bool, DeferInitialCardMark, false, DIAGNOSTIC,                    \
          "When +ReduceInitialCardMarks, explicitly defer any that "        \
          "may arise from new_pre_store_barrier")                           \
                                                                            \
  product(bool, UseCondCardMark, false,                                     \
          "Check for already marked card before updating card table")       \
                                                                            \
  product(bool, VerifyRememberedSets, false, DIAGNOSTIC,                    \
          "Verify GC remembered sets")                                      \
                                                                            \
  product(bool, VerifyObjectStartArray, true, DIAGNOSTIC,                   \
          "Verify GC object start array if verify before/after")            \
                                                                            \
  product(bool, DisableExplicitGC, false,                                   \
          "Ignore calls to System.gc()")                                    \
                                                                            \
  product(bool, PrintGC, false,                                             \
          "Print message at garbage collection. "                           \
          "Deprecated, use -Xlog:gc instead.")                              \
                                                                            \
  product(bool, PrintGCDetails, false,                                      \
          "Print more details at garbage collection. "                      \
          "Deprecated, use -Xlog:gc* instead.")                             \
                                                                            \
  develop(intx, ConcGCYieldTimeout, 0,                                      \
          "If non-zero, assert that GC threads yield within this "          \
          "number of milliseconds")                                         \
          range(0, max_intx)                                                \
                                                                            \
  notproduct(intx, ScavengeALotInterval,     1,                             \
          "Interval between which scavenge will occur with +ScavengeALot")  \
                                                                            \
  notproduct(intx, FullGCALotInterval,     1,                               \
          "Interval between which full gc will occur with +FullGCALot")     \
                                                                            \
  notproduct(intx, FullGCALotStart,     0,                                  \
          "For which invocation to start FullGCAlot")                       \
                                                                            \
  notproduct(intx, FullGCALotDummies,  32*K,                                \
          "Dummy object allocated with +FullGCALot, forcing all objects "   \
          "to move")                                                        \
                                                                            \
  /* gc parameters */                                                       \
  product(size_t, MinHeapSize, 0,                                           \
          "Minimum heap size (in bytes); zero means use ergonomics")        \
          constraint(MinHeapSizeConstraintFunc,AfterErgo)                   \
                                                                            \
  product(size_t, InitialHeapSize, 0,                                       \
          "Initial heap size (in bytes); zero means use ergonomics")        \
          constraint(InitialHeapSizeConstraintFunc,AfterErgo)               \
                                                                            \
  product(size_t, MaxHeapSize, ScaleForWordSize(96*M),                      \
          "Maximum heap size (in bytes)")                                   \
          constraint(MaxHeapSizeConstraintFunc,AfterErgo)                   \
                                                                            \
  product(size_t, SoftMaxHeapSize, 0, MANAGEABLE,                           \
          "Soft limit for maximum heap size (in bytes)")                    \
          constraint(SoftMaxHeapSizeConstraintFunc,AfterMemoryInit)         \
                                                                            \
  product(size_t, OldSize, ScaleForWordSize(4*M),                           \
          "Initial tenured generation size (in bytes)")                     \
          range(0, max_uintx)                                               \
                                                                            \
  product(size_t, NewSize, ScaleForWordSize(1*M),                           \
          "Initial new generation size (in bytes)")                         \
          constraint(NewSizeConstraintFunc,AfterErgo)                       \
                                                                            \
  product(size_t, MaxNewSize, max_uintx,                                    \
          "Maximum new generation size (in bytes), max_uintx means set "    \
          "ergonomically")                                                  \
          range(0, max_uintx)                                               \
                                                                            \
  product_pd(size_t, HeapBaseMinAddress,                                    \
          "OS specific low limit for heap base address")                    \
          constraint(HeapBaseMinAddressConstraintFunc,AfterErgo)            \
                                                                            \
  product(size_t, PretenureSizeThreshold, 0,                                \
          "Maximum size in bytes of objects allocated in DefNew "           \
          "generation; zero means no maximum")                              \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, SurvivorRatio, 8,                                          \
          "Ratio of eden/survivor space size")                              \
          range(1, max_uintx-2)                                             \
          constraint(SurvivorRatioConstraintFunc,AfterMemoryInit)           \
                                                                            \
  product(uintx, NewRatio, 2,                                               \
          "Ratio of old/new generation sizes")                              \
          range(0, max_uintx-1)                                             \
                                                                            \
  product_pd(size_t, NewSizeThreadIncrease,                                 \
          "Additional size added to desired new generation size per "       \
          "non-daemon thread (in bytes)")                                   \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, QueuedAllocationWarningCount, 0,                           \
          "Number of times an allocation that queues behind a GC "          \
          "will retry before printing a warning")                           \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, VerifyGCStartAt,   0, DIAGNOSTIC,                          \
          "GC invoke count where +VerifyBefore/AfterGC kicks in")           \
          range(0, max_uintx)                                               \
                                                                            \
  product(intx, VerifyGCLevel,     0, DIAGNOSTIC,                           \
          "Generation level at which to start +VerifyBefore/AfterGC")       \
          range(0, 1)                                                       \
                                                                            \
  product(uintx, MaxTenuringThreshold,    15,                               \
          "Maximum value for tenuring threshold")                           \
          range(0, markWord::max_age + 1)                                   \
          constraint(MaxTenuringThresholdConstraintFunc,AfterErgo)          \
                                                                            \
  product(uintx, InitialTenuringThreshold,    7,                            \
          "Initial value for tenuring threshold")                           \
          range(0, markWord::max_age + 1)                                   \
          constraint(InitialTenuringThresholdConstraintFunc,AfterErgo)      \
                                                                            \
  product(uintx, TargetSurvivorRatio,    50,                                \
          "Desired percentage of survivor space used after scavenge")       \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, MarkSweepDeadRatio,     5,                                 \
          "Percentage (0-100) of the old gen allowed as dead wood. "        \
          "Serial mark sweep treats this as both the minimum and maximum "  \
          "value. "                                                         \
          "Par compact uses a variable scale based on the density of the "  \
          "generation and treats this as the maximum value when the heap "  \
          "is either completely full or completely empty.  Par compact "    \
          "also has a smaller default value; see arguments.cpp. "           \
          "G1 full gc treats this as an allowed garbage threshold to skip " \
          "compaction of heap regions, i.e. if a heap region has less "     \
          "garbage than this value, then the region will not be compacted"  \
          "during G1 full GC.")                                             \
          range(0, 100)                                                     \
                                                                            \
  product(uint, MarkSweepAlwaysCompactCount,     4,                         \
          "How often should we fully compact the heap (ignoring the dead "  \
          "space parameters)")                                              \
          range(1, max_juint)                                               \
                                                                            \
  develop(uintx, GCExpandToAllocateDelayMillis, 0,                          \
          "Delay between expansion and allocation (in milliseconds)")       \
                                                                            \
  product(uintx, GCDrainStackTargetSize, 64,                                \
          "Number of entries we will try to leave on the stack "            \
          "during parallel gc")                                             \
          range(0, max_juint)

// end of GC_FLAGS

DECLARE_FLAGS(GC_FLAGS)

#endif // SHARE_GC_SHARED_GC_GLOBALS_HPP
