/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1PREDICTIONS_HPP
#define SHARE_GC_G1_G1PREDICTIONS_HPP

#include "utilities/numberSeq.hpp"

// Utility class containing various helper methods for prediction.
class G1Predictions {
 private:
  double _sigma;

  // This function is used to estimate the stddev of sample sets. There is some
  // special consideration of small sample sets: the actual stddev for them is
  // not very useful, so we calculate some value based on the sample average.
  // Five or more samples yields zero (at that point we use the stddev); fewer
  // scale the sample set average linearly from two times the average to 0.5 times
  // it.
  double stddev_estimate(TruncatedSeq const* seq) const {
    double estimate = seq->dsd();
    int const samples = seq->num();
    if (samples < 5) {
      estimate = MAX2(seq->davg() * (5 - samples) / 2.0, estimate);
    }
    return estimate;
  }
 public:
  G1Predictions(double sigma) : _sigma(sigma) {
    assert(sigma >= 0.0, "Confidence must be larger than or equal to zero");
  }

  // Confidence factor.
  double sigma() const { return _sigma; }

  double predict(TruncatedSeq const* seq) const {
    return seq->davg() + _sigma * stddev_estimate(seq);
  }

  double predict_in_unit_interval(TruncatedSeq const* seq) const {
    return clamp(predict(seq), 0.0, 1.0);
  }

  double predict_zero_bounded(TruncatedSeq const* seq) const {
    return MAX2(predict(seq), 0.0);
  }
};

#endif // SHARE_GC_G1_G1PREDICTIONS_HPP
