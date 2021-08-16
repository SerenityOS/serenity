/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSADAPTIVESIZEPOLICY_HPP
#define SHARE_GC_PARALLEL_PSADAPTIVESIZEPOLICY_HPP

#include "gc/shared/adaptiveSizePolicy.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcStats.hpp"
#include "gc/shared/gcUtil.hpp"
#include "utilities/align.hpp"

// This class keeps statistical information and computes the
// optimal free space for both the young and old generation
// based on current application characteristics (based on gc cost
// and application footprint).
//
// It also computes an optimal tenuring threshold between the young
// and old generations, so as to equalize the cost of collections
// of those generations, as well as optimal survivor space sizes
// for the young generation.
//
// While this class is specifically intended for a generational system
// consisting of a young gen (containing an Eden and two semi-spaces)
// and a tenured gen, as well as a perm gen for reflective data, it
// makes NO references to specific generations.
//
// 05/02/2003 Update
// The 1.5 policy makes use of data gathered for the costs of GC on
// specific generations.  That data does reference specific
// generation.  Also diagnostics specific to generations have
// been added.

// Forward decls
class elapsedTimer;

class PSAdaptiveSizePolicy : public AdaptiveSizePolicy {
 friend class PSGCAdaptivePolicyCounters;
 private:
  // These values are used to record decisions made during the
  // policy.  For example, if the young generation was decreased
  // to decrease the GC cost of minor collections the value
  // decrease_young_gen_for_throughput_true is used.

  // Last calculated sizes, in bytes, and aligned
  // NEEDS_CLEANUP should use sizes.hpp,  but it works in ints, not size_t's

  // Time statistics
  AdaptivePaddedAverage* _avg_major_pause;

  // Footprint statistics
  AdaptiveWeightedAverage* _avg_base_footprint;

  // Statistical data gathered for GC
  GCStats _gc_stats;

  const double _collection_cost_margin_fraction;

  // Variable for estimating the major and minor pause times.
  // These variables represent linear least-squares fits of
  // the data.
  //   major pause time vs. old gen size
  LinearLeastSquareFit* _major_pause_old_estimator;
  //   major pause time vs. young gen size
  LinearLeastSquareFit* _major_pause_young_estimator;


  // These record the most recent collection times.  They
  // are available as an alternative to using the averages
  // for making ergonomic decisions.
  double _latest_major_mutator_interval_seconds;

  const size_t _space_alignment; // alignment for eden, survivors

  const double _gc_minor_pause_goal_sec;    // goal for maximum minor gc pause

  // The amount of live data in the heap at the last full GC, used
  // as a baseline to help us determine when we need to perform the
  // next full GC.
  size_t _live_at_last_full_gc;

  // decrease/increase the old generation for minor pause time
  int _change_old_gen_for_min_pauses;

  // increase/decrease the young generation for major pause time
  int _change_young_gen_for_maj_pauses;

  // To facilitate faster growth at start up, supplement the normal
  // growth percentage for the young gen eden and the
  // old gen space for promotion with these value which decay
  // with increasing collections.
  uint _young_gen_size_increment_supplement;
  uint _old_gen_size_increment_supplement;

 private:

  // Accessors
  AdaptivePaddedAverage* avg_major_pause() const { return _avg_major_pause; }
  double gc_minor_pause_goal_sec() const { return _gc_minor_pause_goal_sec; }

  void adjust_eden_for_minor_pause_time(bool is_full_gc,
                                   size_t* desired_eden_size_ptr);
  // Change the generation sizes to achieve a GC pause time goal
  // Returned sizes are not necessarily aligned.
  void adjust_promo_for_pause_time(bool is_full_gc,
                         size_t* desired_promo_size_ptr,
                         size_t* desired_eden_size_ptr);
  void adjust_eden_for_pause_time(bool is_full_gc,
                         size_t* desired_promo_size_ptr,
                         size_t* desired_eden_size_ptr);
  // Change the generation sizes to achieve an application throughput goal
  // Returned sizes are not necessarily aligned.
  void adjust_promo_for_throughput(bool is_full_gc,
                             size_t* desired_promo_size_ptr);
  void adjust_eden_for_throughput(bool is_full_gc,
                             size_t* desired_eden_size_ptr);
  // Change the generation sizes to achieve minimum footprint
  // Returned sizes are not aligned.
  size_t adjust_promo_for_footprint(size_t desired_promo_size,
                                    size_t desired_total);
  size_t adjust_eden_for_footprint(size_t desired_promo_size,
                                   size_t desired_total);

  // Size in bytes for an increment or decrement of eden.
  virtual size_t eden_increment(size_t cur_eden, uint percent_change);
  virtual size_t eden_decrement(size_t cur_eden);
  size_t eden_decrement_aligned_down(size_t cur_eden);
  size_t eden_increment_with_supplement_aligned_up(size_t cur_eden);

  // Size in bytes for an increment or decrement of the promotion area
  virtual size_t promo_increment(size_t cur_promo, uint percent_change);
  virtual size_t promo_decrement(size_t cur_promo);
  size_t promo_decrement_aligned_down(size_t cur_promo);
  size_t promo_increment_with_supplement_aligned_up(size_t cur_promo);

  // Returns a change that has been scaled down.  Result
  // is not aligned.  (If useful, move to some shared
  // location.)
  size_t scale_down(size_t change, double part, double total);

 protected:
  // Time accessors

  // Footprint accessors
  size_t live_space() const {
    return (size_t)(avg_base_footprint()->average() +
                    avg_young_live()->average() +
                    avg_old_live()->average());
  }
  size_t free_space() const {
    return _eden_size + _promo_size;
  }

  void set_promo_size(size_t new_size) {
    _promo_size = new_size;
  }
  void set_survivor_size(size_t new_size) {
    _survivor_size = new_size;
  }

  // Update estimators
  void update_minor_pause_old_estimator(double minor_pause_in_ms);

  virtual GCPolicyKind kind() const { return _gc_ps_adaptive_size_policy; }

 public:
  virtual size_t eden_increment(size_t cur_eden);
  virtual size_t promo_increment(size_t cur_promo);

  // Accessors for use by performance counters
  AdaptivePaddedNoZeroDevAverage*  avg_promoted() const {
    return _gc_stats.avg_promoted();
  }
  AdaptiveWeightedAverage* avg_base_footprint() const {
    return _avg_base_footprint;
  }

  // Input arguments are initial free space sizes for young and old
  // generations, the initial survivor space size, the
  // alignment values and the pause & throughput goals.
  //
  // NEEDS_CLEANUP this is a singleton object
  PSAdaptiveSizePolicy(size_t init_eden_size,
                       size_t init_promo_size,
                       size_t init_survivor_size,
                       size_t space_alignment,
                       double gc_pause_goal_sec,
                       double gc_minor_pause_goal_sec,
                       uint gc_time_ratio);

  // Methods indicating events of interest to the adaptive size policy,
  // called by GC algorithms. It is the responsibility of users of this
  // policy to call these methods at the correct times!
  void major_collection_begin();
  void major_collection_end(size_t amount_live, GCCause::Cause gc_cause);

  void tenured_allocation(size_t size) {
    _avg_pretenured->sample(size);
  }

  // Accessors
  // NEEDS_CLEANUP   should use sizes.hpp

  static size_t calculate_free_based_on_live(size_t live, uintx ratio_as_percentage);

  size_t calculated_old_free_size_in_bytes() const;

  size_t average_old_live_in_bytes() const {
    return (size_t) avg_old_live()->average();
  }

  size_t average_promoted_in_bytes() const {
    return (size_t)avg_promoted()->average();
  }

  size_t padded_average_promoted_in_bytes() const {
    return (size_t)avg_promoted()->padded_average();
  }

  int change_young_gen_for_maj_pauses() {
    return _change_young_gen_for_maj_pauses;
  }
  void set_change_young_gen_for_maj_pauses(int v) {
    _change_young_gen_for_maj_pauses = v;
  }

  int change_old_gen_for_min_pauses() {
    return _change_old_gen_for_min_pauses;
  }
  void set_change_old_gen_for_min_pauses(int v) {
    _change_old_gen_for_min_pauses = v;
  }

  // Return true if the old generation size was changed
  // to try to reach a pause time goal.
  bool old_gen_changed_for_pauses() {
    bool result = _change_old_gen_for_maj_pauses != 0 ||
                  _change_old_gen_for_min_pauses != 0;
    return result;
  }

  // Return true if the young generation size was changed
  // to try to reach a pause time goal.
  bool young_gen_changed_for_pauses() {
    bool result = _change_young_gen_for_min_pauses != 0 ||
                  _change_young_gen_for_maj_pauses != 0;
    return result;
  }
  // end flags for pause goal

  // Return true if the old generation size was changed
  // to try to reach a throughput goal.
  bool old_gen_changed_for_throughput() {
    bool result = _change_old_gen_for_throughput != 0;
    return result;
  }

  // Return true if the young generation size was changed
  // to try to reach a throughput goal.
  bool young_gen_changed_for_throughput() {
    bool result = _change_young_gen_for_throughput != 0;
    return result;
  }

  int decrease_for_footprint() { return _decrease_for_footprint; }


  // Accessors for estimators.  The slope of the linear fit is
  // currently all that is used for making decisions.

  LinearLeastSquareFit* major_pause_old_estimator() {
    return _major_pause_old_estimator;
  }

  LinearLeastSquareFit* major_pause_young_estimator() {
    return _major_pause_young_estimator;
  }


  virtual void clear_generation_free_space_flags();

  float major_pause_old_slope() { return _major_pause_old_estimator->slope(); }
  float major_pause_young_slope() {
    return _major_pause_young_estimator->slope();
  }
  float major_collection_slope() { return _major_collection_estimator->slope();}

  // Given the amount of live data in the heap, should we
  // perform a Full GC?
  bool should_full_GC(size_t live_in_old_gen);

  // Calculates optimal (free) space sizes for both the young and old
  // generations.  Stores results in _eden_size and _promo_size.
  // Takes current used space in all generations as input, as well
  // as an indication if a full gc has just been performed, for use
  // in deciding if an OOM error should be thrown.
  void compute_generations_free_space(size_t young_live,
                                      size_t eden_live,
                                      size_t old_live,
                                      size_t cur_eden,  // current eden in bytes
                                      size_t max_old_gen_size,
                                      size_t max_eden_size,
                                      bool   is_full_gc);

  void compute_eden_space_size(size_t young_live,
                               size_t eden_live,
                               size_t cur_eden,  // current eden in bytes
                               size_t max_eden_size,
                               bool   is_full_gc);

  void compute_old_gen_free_space(size_t old_live,
                                             size_t cur_eden,  // current eden in bytes
                                             size_t max_old_gen_size,
                                             bool   is_full_gc);

  // Calculates new survivor space size;  returns a new tenuring threshold
  // value. Stores new survivor size in _survivor_size.
  uint compute_survivor_space_size_and_threshold(bool   is_survivor_overflow,
                                                 uint    tenuring_threshold,
                                                 size_t survivor_limit);

  // Return the maximum size of a survivor space if the young generation were of
  // size gen_size.
  size_t max_survivor_size(size_t gen_size) {
    // Never allow the target survivor size to grow more than MinSurvivorRatio
    // of the young generation size.  We cannot grow into a two semi-space
    // system, with Eden zero sized.  Even if the survivor space grows, from()
    // might grow by moving the bottom boundary "down" -- so from space will
    // remain almost full anyway (top() will be near end(), but there will be a
    // large filler object at the bottom).
    const size_t sz = gen_size / MinSurvivorRatio;
    const size_t alignment = _space_alignment;
    return sz > alignment ? align_down(sz, alignment) : alignment;
  }

  size_t live_at_last_full_gc() {
    return _live_at_last_full_gc;
  }

  // Update averages that are always used (even
  // if adaptive sizing is turned off).
  void update_averages(bool is_survivor_overflow,
                       size_t survived,
                       size_t promoted);

  // Printing support
  virtual bool print() const;

  // Decay the supplemental growth additive.
  void decay_supplemental_growth(bool is_full_gc);
};

#endif // SHARE_GC_PARALLEL_PSADAPTIVESIZEPOLICY_HPP
