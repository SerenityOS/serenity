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

#include "precompiled.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1Predictions.hpp"
#include "gc/g1/g1SurvRateGroup.hpp"
#include "gc/g1/heapRegion.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"

G1SurvRateGroup::G1SurvRateGroup() :
  _stats_arrays_length(0),
  _accum_surv_rate_pred(NULL),
  _last_pred(0.0),
  _surv_rate_predictors(NULL),
  _num_added_regions(0) {
  reset();
  start_adding_regions();
}

void G1SurvRateGroup::reset() {
  _last_pred = 0.0;
  // the following will set up the arrays with length 1
  _num_added_regions = 1;

  // The call to stop_adding_regions() will use "new" to refill
  // the _surv_rate_pred array, so we need to make sure to call
  // "delete".
  for (size_t i = 0; i < _stats_arrays_length; ++i) {
    delete _surv_rate_predictors[i];
  }
  _stats_arrays_length = 0;

  stop_adding_regions();

  // Seed initial _surv_rate_pred and _accum_surv_rate_pred values
  guarantee(_stats_arrays_length == 1, "invariant" );
  guarantee(_surv_rate_predictors[0] != NULL, "invariant" );
  const double initial_surv_rate = 0.4;
  _surv_rate_predictors[0]->add(initial_surv_rate);
  _last_pred = _accum_surv_rate_pred[0] = initial_surv_rate;

  _num_added_regions = 0;
}

void G1SurvRateGroup::start_adding_regions() {
  _num_added_regions = 0;
}

void G1SurvRateGroup::stop_adding_regions() {
  if (_num_added_regions > _stats_arrays_length) {
    _accum_surv_rate_pred = REALLOC_C_HEAP_ARRAY(double, _accum_surv_rate_pred, _num_added_regions, mtGC);
    _surv_rate_predictors = REALLOC_C_HEAP_ARRAY(TruncatedSeq*, _surv_rate_predictors, _num_added_regions, mtGC);

    for (size_t i = _stats_arrays_length; i < _num_added_regions; ++i) {
      _surv_rate_predictors[i] = new TruncatedSeq(10);
    }

    _stats_arrays_length = _num_added_regions;
  }
}

void G1SurvRateGroup::record_surviving_words(int age_in_group, size_t surv_words) {
  guarantee(0 <= age_in_group && (size_t)age_in_group < _num_added_regions,
            "age_in_group is %d not between 0 and " SIZE_FORMAT, age_in_group, _num_added_regions);

  double surv_rate = (double)surv_words / HeapRegion::GrainWords;
  _surv_rate_predictors[age_in_group]->add(surv_rate);
}

void G1SurvRateGroup::all_surviving_words_recorded(const G1Predictions& predictor, bool update_predictors) {
  if (update_predictors) {
    fill_in_last_surv_rates();
  }
  finalize_predictions(predictor);
}

void G1SurvRateGroup::fill_in_last_surv_rates() {
  if (_num_added_regions > 0) { // conservative
    double surv_rate = _surv_rate_predictors[_num_added_regions-1]->last();
    for (size_t i = _num_added_regions; i < _stats_arrays_length; ++i) {
      _surv_rate_predictors[i]->add(surv_rate);
    }
  }
}

void G1SurvRateGroup::finalize_predictions(const G1Predictions& predictor) {
  double accum = 0.0;
  double pred = 0.0;
  for (size_t i = 0; i < _stats_arrays_length; ++i) {
    pred = predictor.predict_in_unit_interval(_surv_rate_predictors[i]);
    accum += pred;
    _accum_surv_rate_pred[i] = accum;
  }
  _last_pred = pred;
}
