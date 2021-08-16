/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAH_GLOBALS_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAH_GLOBALS_HPP

#define GC_SHENANDOAH_FLAGS(develop,                                        \
                            develop_pd,                                     \
                            product,                                        \
                            product_pd,                                     \
                            notproduct,                                     \
                            range,                                          \
                            constraint)                                     \
                                                                            \
  product(size_t, ShenandoahRegionSize, 0, EXPERIMENTAL,                    \
          "Static heap region size. Set zero to enable automatic sizing.")  \
                                                                            \
  product(size_t, ShenandoahTargetNumRegions, 2048, EXPERIMENTAL,           \
          "With automatic region sizing, this is the approximate number "   \
          "of regions that would be used, within min/max region size "      \
          "limits.")                                                        \
                                                                            \
  product(size_t, ShenandoahMinRegionSize, 256 * K, EXPERIMENTAL,           \
          "With automatic region sizing, the regions would be at least "    \
          "this large.")                                                    \
                                                                            \
  product(size_t, ShenandoahMaxRegionSize, 32 * M, EXPERIMENTAL,            \
          "With automatic region sizing, the regions would be at most "     \
          "this large.")                                                    \
                                                                            \
  product(intx, ShenandoahHumongousThreshold, 100, EXPERIMENTAL,            \
          "Humongous objects are allocated in separate regions. "           \
          "This setting defines how large the object should be to be "      \
          "deemed humongous. Value is in  percents of heap region size. "   \
          "This also caps the maximum TLAB size.")                          \
          range(1, 100)                                                     \
                                                                            \
  product(ccstr, ShenandoahGCMode, "satb",                                  \
          "GC mode to use.  Among other things, this defines which "        \
          "barriers are in in use. Possible values are:"                    \
          " satb - snapshot-at-the-beginning concurrent GC (three pass mark-evac-update);"  \
          " iu - incremental-update concurrent GC (three pass mark-evac-update);"  \
          " passive - stop the world GC only (either degenerated or full)") \
                                                                            \
  product(ccstr, ShenandoahGCHeuristics, "adaptive",                        \
          "GC heuristics to use. This fine-tunes the GC mode selected, "    \
          "by choosing when to start the GC, how much to process on each "  \
          "cycle, and what other features to automatically enable. "        \
          "Possible values are:"                                            \
          " adaptive - adapt to maintain the given amount of free heap "    \
          "at all times, even during the GC cycle;"                         \
          " static -  trigger GC when free heap falls below the threshold;" \
          " aggressive - run GC continuously, try to evacuate everything;"  \
          " compact - run GC more frequently and with deeper targets to "   \
          "free up more memory.")                                           \
                                                                            \
  product(uintx, ShenandoahUnloadClassesFrequency, 1, EXPERIMENTAL,         \
          "Unload the classes every Nth cycle. Normally affects concurrent "\
          "GC cycles, as degenerated and full GCs would try to unload "     \
          "classes regardless. Set to zero to disable class unloading.")    \
                                                                            \
  product(uintx, ShenandoahGarbageThreshold, 25, EXPERIMENTAL,              \
          "How much garbage a region has to contain before it would be "    \
          "taken for collection. This a guideline only, as GC heuristics "  \
          "may select the region for collection even if it has little "     \
          "garbage. This also affects how much internal fragmentation the " \
          "collector accepts. In percents of heap region size.")            \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahInitFreeThreshold, 70, EXPERIMENTAL,             \
          "How much heap should be free before some heuristics trigger the "\
          "initial (learning) cycles. Affects cycle frequency on startup "  \
          "and after drastic state changes, e.g. after degenerated/full "   \
          "GC cycles. In percents of (soft) max heap size.")                \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahMinFreeThreshold, 10, EXPERIMENTAL,              \
          "How much heap should be free before most heuristics trigger the "\
          "collection, even without other triggers. Provides the safety "   \
          "margin for many heuristics. In percents of (soft) max heap size.")\
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahAllocationThreshold, 0, EXPERIMENTAL,            \
          "How many new allocations should happen since the last GC cycle " \
          "before some heuristics trigger the collection. In percents of "  \
          "(soft) max heap size. Set to zero to effectively disable.")      \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahAllocSpikeFactor, 5, EXPERIMENTAL,               \
          "How much of heap should some heuristics reserve for absorbing "  \
          "the allocation spikes. Larger value wastes more memory in "      \
          "non-emergency cases, but provides more safety in emergency "     \
          "cases. In percents of (soft) max heap size.")                    \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahLearningSteps, 5, EXPERIMENTAL,                  \
          "The number of cycles some heuristics take to collect in order "  \
          "to learn application and GC performance.")                       \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahImmediateThreshold, 90, EXPERIMENTAL,            \
          "The cycle may shortcut when enough garbage can be reclaimed "    \
          "from the immediate garbage (completely garbage regions). "       \
          "In percents of total garbage found. Setting this threshold "     \
          "to 100 effectively disables the shortcut.")                      \
          range(0,100)                                                      \
                                                                            \
  product(uintx, ShenandoahAdaptiveSampleFrequencyHz, 10, EXPERIMENTAL,     \
          "The number of times per second to update the allocation rate "   \
          "moving average.")                                                \
                                                                            \
  product(uintx, ShenandoahAdaptiveSampleSizeSeconds, 10, EXPERIMENTAL,     \
          "The size of the moving window over which the average "           \
          "allocation rate is maintained. The total number of samples "     \
          "is the product of this number and the sample frequency.")        \
                                                                            \
  product(double, ShenandoahAdaptiveInitialConfidence, 1.8, EXPERIMENTAL,   \
          "The number of standard deviations used to determine an initial " \
          "margin of error for the average cycle time and average "         \
          "allocation rate. Increasing this value will cause the "          \
          "heuristic to initiate more concurrent cycles." )                 \
                                                                            \
  product(double, ShenandoahAdaptiveInitialSpikeThreshold, 1.8, EXPERIMENTAL, \
          "If the most recently sampled allocation rate is more than "      \
          "this many standard deviations away from the moving average, "    \
          "then a cycle is initiated. This value controls how sensitive "   \
          "the heuristic is to allocation spikes. Decreasing this number "  \
          "increases the sensitivity. ")                                    \
                                                                            \
  product(double, ShenandoahAdaptiveDecayFactor, 0.5, EXPERIMENTAL,         \
          "The decay factor (alpha) used for values in the weighted "       \
          "moving average of cycle time and allocation rate. "              \
          "Larger values give more weight to recent values.")               \
          range(0,1.0)                                                      \
                                                                            \
  product(uintx, ShenandoahGuaranteedGCInterval, 5*60*1000, EXPERIMENTAL,   \
          "Many heuristics would guarantee a concurrent GC cycle at "       \
          "least with this interval. This is useful when large idle "       \
          "intervals are present, where GC can run without stealing "       \
          "time from active application. Time is in milliseconds. "         \
          "Setting this to 0 disables the feature.")                        \
                                                                            \
  product(bool, ShenandoahAlwaysClearSoftRefs, false, EXPERIMENTAL,         \
          "Unconditionally clear soft references, instead of using any "    \
          "other cleanup policy. This minimizes footprint at expense of"    \
          "more soft reference churn in applications.")                     \
                                                                            \
  product(bool, ShenandoahUncommit, true, EXPERIMENTAL,                     \
          "Allow to uncommit memory under unused regions and metadata. "    \
          "This optimizes footprint at expense of allocation latency in "   \
          "regions that require committing back. Uncommits would be "       \
          "disabled by some heuristics, or with static heap size.")         \
                                                                            \
  product(uintx, ShenandoahUncommitDelay, 5*60*1000, EXPERIMENTAL,          \
          "Uncommit memory for regions that were not used for more than "   \
          "this time. First use after that would incur allocation stalls. " \
          "Actively used regions would never be uncommitted, because they " \
          "do not become unused longer than this delay. Time is in "        \
          "milliseconds. Setting this delay to 0 effectively uncommits "    \
          "regions almost immediately after they become unused.")           \
                                                                            \
  product(bool, ShenandoahRegionSampling, false, EXPERIMENTAL,              \
          "Provide heap region sampling data via jvmstat.")                 \
                                                                            \
  product(int, ShenandoahRegionSamplingRate, 40, EXPERIMENTAL,              \
          "Sampling rate for heap region sampling. In milliseconds between "\
          "the samples. Higher values provide more fidelity, at expense "   \
          "of more sampling overhead.")                                     \
                                                                            \
  product(uintx, ShenandoahControlIntervalMin, 1, EXPERIMENTAL,             \
          "The minimum sleep interval for the control loop that drives "    \
          "the cycles. Lower values would increase GC responsiveness "      \
          "to changing heap conditions, at the expense of higher perf "     \
          "overhead. Time is in milliseconds.")                             \
                                                                            \
  product(uintx, ShenandoahControlIntervalMax, 10, EXPERIMENTAL,            \
          "The maximum sleep interval for control loop that drives "        \
          "the cycles. Lower values would increase GC responsiveness "      \
          "to changing heap conditions, at the expense of higher perf "     \
          "overhead. Time is in milliseconds.")                             \
                                                                            \
  product(uintx, ShenandoahControlIntervalAdjustPeriod, 1000, EXPERIMENTAL, \
          "The time period for one step in control loop interval "          \
          "adjustment. Lower values make adjustments faster, at the "       \
          "expense of higher perf overhead. Time is in milliseconds.")      \
                                                                            \
  product(bool, ShenandoahVerify, false, DIAGNOSTIC,                        \
          "Enable internal verification. This would catch many GC bugs, "   \
          "but it would also stall the collector during the verification, " \
          "which prolongs the pauses and might hide other bugs.")           \
                                                                            \
  product(intx, ShenandoahVerifyLevel, 4, DIAGNOSTIC,                       \
          "Verification level, higher levels check more, taking more time. "\
          "Accepted values are:"                                            \
          " 0 = basic heap checks; "                                        \
          " 1 = previous level, plus basic region checks; "                 \
          " 2 = previous level, plus all roots; "                           \
          " 3 = previous level, plus all reachable objects; "               \
          " 4 = previous level, plus all marked objects")                   \
                                                                            \
  product(bool, ShenandoahElasticTLAB, true, DIAGNOSTIC,                    \
          "Use Elastic TLABs with Shenandoah")                              \
                                                                            \
  product(uintx, ShenandoahEvacReserve, 5, EXPERIMENTAL,                    \
          "How much of heap to reserve for evacuations. Larger values make "\
          "GC evacuate more live objects on every cycle, while leaving "    \
          "less headroom for application to allocate in. In percents of "   \
          "total heap size.")                                               \
          range(1,100)                                                      \
                                                                            \
  product(double, ShenandoahEvacWaste, 1.2, EXPERIMENTAL,                   \
          "How much waste evacuations produce within the reserved space. "  \
          "Larger values make evacuations more resilient against "          \
          "evacuation conflicts, at expense of evacuating less on each "    \
          "GC cycle.")                                                      \
          range(1.0,100.0)                                                  \
                                                                            \
  product(bool, ShenandoahEvacReserveOverflow, true, EXPERIMENTAL,          \
          "Allow evacuations to overflow the reserved space. Enabling it "  \
          "will make evacuations more resilient when evacuation "           \
          "reserve/waste is incorrect, at the risk that application "       \
          "runs out of memory too early.")                                  \
                                                                            \
  product(bool, ShenandoahPacing, true, EXPERIMENTAL,                       \
          "Pace application allocations to give GC chance to start "        \
          "and complete before allocation failure is reached.")             \
                                                                            \
  product(uintx, ShenandoahPacingMaxDelay, 10, EXPERIMENTAL,                \
          "Max delay for pacing application allocations. Larger values "    \
          "provide more resilience against out of memory, at expense at "   \
          "hiding the GC latencies in the allocation path. Time is in "     \
          "milliseconds. Setting it to arbitrarily large value makes "      \
          "GC effectively stall the threads indefinitely instead of going " \
          "to degenerated or Full GC.")                                     \
                                                                            \
  product(uintx, ShenandoahPacingIdleSlack, 2, EXPERIMENTAL,                \
          "How much of heap counted as non-taxable allocations during idle "\
          "phases. Larger value makes the pacing milder when collector is " \
          "idle, requiring less rendezvous with control thread. Lower "     \
          "value makes the pacing control less responsive to out-of-cycle " \
          "allocs. In percent of total heap size.")                         \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, ShenandoahPacingCycleSlack, 10, EXPERIMENTAL,              \
          "How much of free space to take as non-taxable allocations "      \
          "the GC cycle. Larger value makes the pacing milder at the "      \
          "beginning of the GC cycle. Lower value makes the pacing less "   \
          "uniform during the cycle. In percent of free space.")            \
          range(0, 100)                                                     \
                                                                            \
  product(double, ShenandoahPacingSurcharge, 1.1, EXPERIMENTAL,             \
          "Additional pacing tax surcharge to help unclutter the heap. "    \
          "Larger values makes the pacing more aggressive. Lower values "   \
          "risk GC cycles finish with less memory than were available at "  \
          "the beginning of it.")                                           \
          range(1.0, 100.0)                                                 \
                                                                            \
  product(uintx, ShenandoahCriticalFreeThreshold, 1, EXPERIMENTAL,          \
          "How much of the heap needs to be free after recovery cycles, "   \
          "either Degenerated or Full GC to be claimed successful. If this "\
          "much space is not available, next recovery step would be "       \
          "triggered.")                                                     \
          range(0, 100)                                                     \
                                                                            \
  product(bool, ShenandoahDegeneratedGC, true, DIAGNOSTIC,                  \
          "Enable Degenerated GC as the graceful degradation step. "        \
          "Disabling this option leads to degradation to Full GC instead. " \
          "When running in passive mode, this can be toggled to measure "   \
          "either Degenerated GC or Full GC costs.")                        \
                                                                            \
  product(uintx, ShenandoahFullGCThreshold, 3, EXPERIMENTAL,                \
          "How many back-to-back Degenerated GCs should happen before "     \
          "going to a Full GC.")                                            \
                                                                            \
  product(bool, ShenandoahImplicitGCInvokesConcurrent, false, EXPERIMENTAL, \
          "Should internally-caused GC requests invoke concurrent cycles, " \
          "should they do the stop-the-world (Degenerated / Full GC)? "     \
          "Many heuristics automatically enable this. This option is "      \
          "similar to global ExplicitGCInvokesConcurrent.")                 \
                                                                            \
  product(bool, ShenandoahHumongousMoves, true, DIAGNOSTIC,                 \
          "Allow moving humongous regions. This makes GC more resistant "   \
          "to external fragmentation that may otherwise fail other "        \
          "humongous allocations, at the expense of higher GC copying "     \
          "costs. Currently affects stop-the-world (Full) cycle only.")     \
                                                                            \
  product(bool, ShenandoahOOMDuringEvacALot, false, DIAGNOSTIC,             \
          "Testing: simulate OOM during evacuation.")                       \
                                                                            \
  product(bool, ShenandoahAllocFailureALot, false, DIAGNOSTIC,              \
          "Testing: make lots of artificial allocation failures.")          \
                                                                            \
  product(intx, ShenandoahMarkScanPrefetch, 32, EXPERIMENTAL,               \
          "How many objects to prefetch ahead when traversing mark bitmaps."\
          "Set to 0 to disable prefetching.")                               \
          range(0, 256)                                                     \
                                                                            \
  product(uintx, ShenandoahMarkLoopStride, 1000, EXPERIMENTAL,              \
          "How many items to process during one marking iteration before "  \
          "checking for cancellation, yielding, etc. Larger values improve "\
          "marking performance at expense of responsiveness.")              \
                                                                            \
  product(uintx, ShenandoahParallelRegionStride, 1024, EXPERIMENTAL,        \
          "How many regions to process at once during parallel region "     \
          "iteration. Affects heaps with lots of regions.")                 \
                                                                            \
  product(size_t, ShenandoahSATBBufferSize, 1 * K, EXPERIMENTAL,            \
          "Number of entries in an SATB log buffer.")                       \
          range(1, max_uintx)                                               \
                                                                            \
  product(uintx, ShenandoahMaxSATBBufferFlushes, 5, EXPERIMENTAL,           \
          "How many times to maximum attempt to flush SATB buffers at the " \
          "end of concurrent marking.")                                     \
                                                                            \
  product(bool, ShenandoahSuspendibleWorkers, false, EXPERIMENTAL,          \
          "Suspend concurrent GC worker threads at safepoints")             \
                                                                            \
  product(bool, ShenandoahSATBBarrier, true, DIAGNOSTIC,                    \
          "Turn on/off SATB barriers in Shenandoah")                        \
                                                                            \
  product(bool, ShenandoahIUBarrier, false, DIAGNOSTIC,                     \
          "Turn on/off I-U barriers barriers in Shenandoah")                \
                                                                            \
  product(bool, ShenandoahCASBarrier, true, DIAGNOSTIC,                     \
          "Turn on/off CAS barriers in Shenandoah")                         \
                                                                            \
  product(bool, ShenandoahCloneBarrier, true, DIAGNOSTIC,                   \
          "Turn on/off clone barriers in Shenandoah")                       \
                                                                            \
  product(bool, ShenandoahLoadRefBarrier, true, DIAGNOSTIC,                 \
          "Turn on/off load-reference barriers in Shenandoah")              \
                                                                            \
  product(bool, ShenandoahNMethodBarrier, true, DIAGNOSTIC,                 \
          "Turn on/off NMethod entry barriers in Shenandoah")               \
                                                                            \
  product(bool, ShenandoahStackWatermarkBarrier, true, DIAGNOSTIC,          \
          "Turn on/off stack watermark barriers in Shenandoah")             \
                                                                            \
  develop(bool, ShenandoahVerifyOptoBarriers, false,                        \
          "Verify no missing barriers in C2.")                              \
                                                                            \
  product(bool, ShenandoahLoopOptsAfterExpansion, true, DIAGNOSTIC,         \
          "Attempt more loop opts after barrier expansion.")                \
                                                                            \
  product(bool, ShenandoahSelfFixing, true, DIAGNOSTIC,                     \
          "Fix references with load reference barrier. Disabling this "     \
          "might degrade performance.")

// end of GC_SHENANDOAH_FLAGS

#endif // SHARE_GC_SHENANDOAH_SHENANDOAH_GLOBALS_HPP
