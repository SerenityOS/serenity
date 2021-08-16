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

#include "precompiled.hpp"

#include "gc/shenandoah/shenandoahNumberSeq.hpp"
#include "runtime/atomic.hpp"

HdrSeq::HdrSeq() {
  _hdr = NEW_C_HEAP_ARRAY(int*, MagBuckets, mtInternal);
  for (int c = 0; c < MagBuckets; c++) {
    _hdr[c] = NULL;
  }
}

HdrSeq::~HdrSeq() {
  for (int c = 0; c < MagBuckets; c++) {
    int* sub = _hdr[c];
    if (sub != NULL) {
      FREE_C_HEAP_ARRAY(int, sub);
    }
  }
  FREE_C_HEAP_ARRAY(int*, _hdr);
}

void HdrSeq::add(double val) {
  if (val < 0) {
    assert (false, "value (%8.2f) is not negative", val);
    val = 0;
  }

  NumberSeq::add(val);

  double v = val;
  int mag;
  if (v > 0) {
    mag = 0;
    while (v > 1) {
      mag++;
      v /= 10;
    }
    while (v < 0.1) {
      mag--;
      v *= 10;
    }
  } else {
    mag = MagMinimum;
  }

  int bucket = -MagMinimum + mag;
  int sub_bucket = (int) (v * ValBuckets);

  // Defensively saturate for product bits:
  if (bucket < 0) {
    assert (false, "bucket index (%d) underflow for value (%8.2f)", bucket, val);
    bucket = 0;
  }

  if (bucket >= MagBuckets) {
    assert (false, "bucket index (%d) overflow for value (%8.2f)", bucket, val);
    bucket = MagBuckets - 1;
  }

  if (sub_bucket < 0) {
    assert (false, "sub-bucket index (%d) underflow for value (%8.2f)", sub_bucket, val);
    sub_bucket = 0;
  }

  if (sub_bucket >= ValBuckets) {
    assert (false, "sub-bucket index (%d) overflow for value (%8.2f)", sub_bucket, val);
    sub_bucket = ValBuckets - 1;
  }

  int* b = _hdr[bucket];
  if (b == NULL) {
    b = NEW_C_HEAP_ARRAY(int, ValBuckets, mtInternal);
    for (int c = 0; c < ValBuckets; c++) {
      b[c] = 0;
    }
    _hdr[bucket] = b;
  }
  b[sub_bucket]++;
}

double HdrSeq::percentile(double level) const {
  // target should be non-zero to find the first sample
  int target = MAX2(1, (int) (level * num() / 100));
  int cnt = 0;
  for (int mag = 0; mag < MagBuckets; mag++) {
    if (_hdr[mag] != NULL) {
      for (int val = 0; val < ValBuckets; val++) {
        cnt += _hdr[mag][val];
        if (cnt >= target) {
          return pow(10.0, MagMinimum + mag) * val / ValBuckets;
        }
      }
    }
  }
  return maximum();
}

BinaryMagnitudeSeq::BinaryMagnitudeSeq() {
  _mags = NEW_C_HEAP_ARRAY(size_t, BitsPerSize_t, mtInternal);
  clear();
}

BinaryMagnitudeSeq::~BinaryMagnitudeSeq() {
  FREE_C_HEAP_ARRAY(size_t, _mags);
}

void BinaryMagnitudeSeq::clear() {
  for (int c = 0; c < BitsPerSize_t; c++) {
    _mags[c] = 0;
  }
  _sum = 0;
}

void BinaryMagnitudeSeq::add(size_t val) {
  Atomic::add(&_sum, val);

  int mag = log2i_graceful(val) + 1;

  // Defensively saturate for product bits:
  if (mag < 0) {
    assert (false, "bucket index (%d) underflow for value (" SIZE_FORMAT ")", mag, val);
    mag = 0;
  }

  if (mag >= BitsPerSize_t) {
    assert (false, "bucket index (%d) overflow for value (" SIZE_FORMAT ")", mag, val);
    mag = BitsPerSize_t - 1;
  }

  Atomic::add(&_mags[mag], (size_t)1);
}

size_t BinaryMagnitudeSeq::level(int level) const {
  if (0 <= level && level < BitsPerSize_t) {
    return _mags[level];
  } else {
    return 0;
  }
}

size_t BinaryMagnitudeSeq::num() const {
  size_t r = 0;
  for (int c = 0; c < BitsPerSize_t; c++) {
    r += _mags[c];
  }
  return r;
}

size_t BinaryMagnitudeSeq::sum() const {
  return _sum;
}

int BinaryMagnitudeSeq::min_level() const {
  for (int c = 0; c < BitsPerSize_t; c++) {
    if (_mags[c] != 0) {
      return c;
    }
  }
  return BitsPerSize_t - 1;
}

int BinaryMagnitudeSeq::max_level() const {
  for (int c = BitsPerSize_t - 1; c > 0; c--) {
    if (_mags[c] != 0) {
      return c;
    }
  }
  return 0;
}
