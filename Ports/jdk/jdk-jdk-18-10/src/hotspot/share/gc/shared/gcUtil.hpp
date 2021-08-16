/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCUTIL_HPP
#define SHARE_GC_SHARED_GCUTIL_HPP

#include "memory/allocation.hpp"
#include "runtime/timer.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

// Catch-all file for utility classes

// A weighted average maintains a running, weighted average
// of some float value (templates would be handy here if we
// need different types).
//
// The average is adaptive in that we smooth it for the
// initial samples; we don't use the weight until we have
// enough samples for it to be meaningful.
//
// This serves as our best estimate of a future unknown.
//
class AdaptiveWeightedAverage : public CHeapObj<mtGC> {
 private:
  float            _average;        // The last computed average
  unsigned         _sample_count;   // How often we've sampled this average
  unsigned         _weight;         // The weight used to smooth the averages
                                    //   A higher weight favors the most
                                    //   recent data.
  bool             _is_old;         // Has enough historical data

  const static unsigned OLD_THRESHOLD = 100;

 protected:
  float            _last_sample;    // The last value sampled.

  void  increment_count() {
    _sample_count++;
    if (!_is_old && _sample_count > OLD_THRESHOLD) {
      _is_old = true;
    }
  }

  void  set_average(float avg)  { _average = avg;        }

  // Helper function, computes an adaptive weighted average
  // given a sample and the last average
  float compute_adaptive_average(float new_sample, float average);

 public:
  // Input weight must be between 0 and 100
  AdaptiveWeightedAverage(unsigned weight, float avg = 0.0) :
    _average(avg), _sample_count(0), _weight(weight),
    _is_old(false), _last_sample(0.0) {
  }

  void clear() {
    _average = 0;
    _sample_count = 0;
    _last_sample = 0;
    _is_old = false;
  }

  // Useful for modifying static structures after startup.
  void  modify(size_t avg, unsigned wt, bool force = false)  {
    assert(force, "Are you sure you want to call this?");
    _average = (float)avg;
    _weight  = wt;
  }

  // Accessors
  float    average() const       { return _average;       }
  unsigned weight()  const       { return _weight;        }
  unsigned count()   const       { return _sample_count;  }
  float    last_sample() const   { return _last_sample;   }
  bool     is_old()  const       { return _is_old;        }

  // Update data with a new sample.
  void sample(float new_sample);

  static inline float exp_avg(float avg, float sample,
                               unsigned int weight) {
    assert(weight <= 100, "weight must be a percent");
    return (100.0F - weight) * avg / 100.0F + weight * sample / 100.0F;
  }
  static inline size_t exp_avg(size_t avg, size_t sample,
                               unsigned int weight) {
    // Convert to float and back to avoid integer overflow.
    return (size_t)exp_avg((float)avg, (float)sample, weight);
  }

  // Printing
  void print_on(outputStream* st) const;
  void print() const;
};


// A weighted average that includes a deviation from the average,
// some multiple of which is added to the average.
//
// This serves as our best estimate of an upper bound on a future
// unknown.
class AdaptivePaddedAverage : public AdaptiveWeightedAverage {
 private:
  float          _padded_avg;     // The last computed padded average
  float          _deviation;      // Running deviation from the average
  unsigned       _padding;        // A multiple which, added to the average,
                                  // gives us an upper bound guess.

 protected:
  void set_padded_average(float avg)  { _padded_avg = avg;  }
  void set_deviation(float dev)       { _deviation  = dev;  }

 public:
  AdaptivePaddedAverage() :
    AdaptiveWeightedAverage(0),
    _padded_avg(0.0), _deviation(0.0), _padding(0) {}

  AdaptivePaddedAverage(unsigned weight, unsigned padding) :
    AdaptiveWeightedAverage(weight),
    _padded_avg(0.0), _deviation(0.0), _padding(padding) {}

  // Placement support
  void* operator new(size_t ignored, void* p) throw() { return p; }
  // Allocator
  void* operator new(size_t size) throw();

  // Accessor
  float padded_average() const         { return _padded_avg; }
  float deviation()      const         { return _deviation;  }
  unsigned padding()     const         { return _padding;    }

  void clear() {
    AdaptiveWeightedAverage::clear();
    _padded_avg = 0;
    _deviation = 0;
  }

  // Override
  void  sample(float new_sample);

  // Printing
  void print_on(outputStream* st) const;
  void print() const;
};

// A weighted average that includes a deviation from the average,
// some multiple of which is added to the average.
//
// This serves as our best estimate of an upper bound on a future
// unknown.
// A special sort of padded average:  it doesn't update deviations
// if the sample is zero. The average is allowed to change. We're
// preventing the zero samples from drastically changing our padded
// average.
class AdaptivePaddedNoZeroDevAverage : public AdaptivePaddedAverage {
public:
  AdaptivePaddedNoZeroDevAverage(unsigned weight, unsigned padding) :
    AdaptivePaddedAverage(weight, padding)  {}
  // Override
  void  sample(float new_sample);

  // Printing
  void print_on(outputStream* st) const;
  void print() const;
};

// Use a least squares fit to a set of data to generate a linear
// equation.
//              y = intercept + slope * x

class LinearLeastSquareFit : public CHeapObj<mtGC> {
  double _sum_x;        // sum of all independent data points x
  double _sum_x_squared; // sum of all independent data points x**2
  double _sum_y;        // sum of all dependent data points y
  double _sum_xy;       // sum of all x * y.
  double _intercept;     // constant term
  double _slope;        // slope
  // The weighted averages are not currently used but perhaps should
  // be used to get decaying averages.
  AdaptiveWeightedAverage _mean_x; // weighted mean of independent variable
  AdaptiveWeightedAverage _mean_y; // weighted mean of dependent variable

 public:
  LinearLeastSquareFit(unsigned weight);
  void update(double x, double y);
  double y(double x);
  double slope() { return _slope; }
  // Methods to decide if a change in the dependent variable will
  // achieve a desired goal.  Note that these methods are not
  // complementary and both are needed.
  bool decrement_will_decrease();
  bool increment_will_decrease();
};

#endif // SHARE_GC_SHARED_GCUTIL_HPP
