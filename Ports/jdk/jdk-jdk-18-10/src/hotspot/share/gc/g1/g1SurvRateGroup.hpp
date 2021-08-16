/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1SURVRATEGROUP_HPP
#define SHARE_GC_G1_G1SURVRATEGROUP_HPP

#include "gc/g1/g1Predictions.hpp"
#include "utilities/numberSeq.hpp"

// A survivor rate group tracks survival ratios of objects allocated in the
// heap regions associated to a set of regions (a "space", i.e. eden or survivor)
// on a time basis to predict future survival rates of regions of the same "age".
//
// Every time a new heap region associated with a survivor rate group is retired
// (i.e. the time basis), it gets associated the next "age" entry in that group.
//
// During garbage collection G1 keeps track how much of total data is copied out
// of a heap region (i.e. survives), to update the survivor rate predictor of that age.
//
// This information is used to predict, given a particular age of a heap region,
// how much of its contents will likely survive to determine young generation sizes.
//
// The age index associated with a heap region is incremented from 0 (retired first)
// to N (retired just before the GC).
//
// To avoid copying around data all the time when the total amount of regions in
// a survivor rate group changes, this class organizes the arrays containing the
// predictors in reverse chronological order as returned by age_in_group(). I.e.
// index 0 contains the rate information for the region retired most recently.
class G1SurvRateGroup : public CHeapObj<mtGC> {
  size_t  _stats_arrays_length;
  double* _accum_surv_rate_pred;
  double  _last_pred;
  TruncatedSeq** _surv_rate_predictors;

  size_t _num_added_regions;   // The number of regions in this survivor rate group.

  void fill_in_last_surv_rates();
  void finalize_predictions(const G1Predictions& predictor);

public:
  static const int InvalidAgeIndex = -1;
  static bool is_valid_age_index(int age) { return age >= 0; }

  G1SurvRateGroup();
  void reset();
  void start_adding_regions();
  void stop_adding_regions();
  void record_surviving_words(int age_in_group, size_t surv_words);
  void all_surviving_words_recorded(const G1Predictions& predictor, bool update_predictors);

  double accum_surv_rate_pred(int age) const {
    assert(_stats_arrays_length > 0, "invariant" );
    assert(is_valid_age_index(age), "must be");
    if ((size_t)age < _stats_arrays_length)
      return _accum_surv_rate_pred[age];
    else {
      double diff = (double)(age - _stats_arrays_length + 1);
      return _accum_surv_rate_pred[_stats_arrays_length - 1] + diff * _last_pred;
    }
  }

  double surv_rate_pred(G1Predictions const& predictor, int age) const {
    assert(is_valid_age_index(age), "must be");

    age = MIN2(age, (int)_stats_arrays_length - 1);

    return predictor.predict_in_unit_interval(_surv_rate_predictors[age]);
  }

  int next_age_index() {
    return (int)++_num_added_regions;
  }

  int age_in_group(int age_index) const {
    int result = (int)(_num_added_regions - age_index);
    assert(is_valid_age_index(result), "invariant" );
    return result;
  }
};

#endif // SHARE_GC_G1_G1SURVRATEGROUP_HPP
