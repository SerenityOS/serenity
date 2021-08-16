/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHNUMBERSEQ_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHNUMBERSEQ_HPP

#include "utilities/numberSeq.hpp"

// HDR sequence stores the low-resolution high-dynamic-range values.
// It does so by maintaining the double array, where first array defines
// the magnitude of the value being stored, and the second array maintains
// the low resolution histogram within that magnitude. For example, storing
// 4.352819 * 10^3 increments the bucket _hdr[3][435]. This allows for
// memory efficient storage of huge amount of samples.
//
// Accepts positive numbers only.
class HdrSeq: public NumberSeq {
private:
  enum PrivateConstants {
    ValBuckets = 512,
    MagBuckets = 24,
    MagMinimum = -12
  };
  int** _hdr;

public:
  HdrSeq();
  ~HdrSeq();

  virtual void add(double val);
  double percentile(double level) const;
};

// Binary magnitude sequence stores the power-of-two histogram.
// It has very low memory requirements, and is thread-safe. When accuracy
// is not needed, it is preferred over HdrSeq.
class BinaryMagnitudeSeq {
private:
  size_t  _sum;
  size_t* _mags;

public:
  BinaryMagnitudeSeq();
  ~BinaryMagnitudeSeq();

  void add(size_t val);
  size_t num() const;
  size_t level(int level) const;
  size_t sum() const;
  int min_level() const;
  int max_level() const;
  void clear();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHNUMBERSEQ_HPP
