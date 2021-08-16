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

#ifndef SHARE_UTILITIES_NUMBERSEQ_HPP
#define SHARE_UTILITIES_NUMBERSEQ_HPP

#include "memory/allocation.hpp"

/**
 **  This file contains a few classes that represent number sequence,
 **  x1, x2, x3, ..., xN, and can calculate their avg, max, and sd.
 **
 **  Here's a quick description of the classes:
 **
 **    AbsSeq: abstract superclass
 **    NumberSeq: the sequence is assumed to be very long and the
 **      maximum, avg, sd, davg, and dsd are calculated over all its elements
 **    TruncatedSeq: this class keeps track of the last L elements
 **      of the sequence and calculates avg, max, and sd only over them
 **/

#define DEFAULT_ALPHA_VALUE 0.3

class AbsSeq: public CHeapObj<mtInternal> {
private:
  void init(double alpha);

protected:
  int    _num; // the number of elements in the sequence
  double _sum; // the sum of the elements in the sequence
  double _sum_of_squares; // the sum of squares of the elements in the sequence

  double _davg; // decaying average
  double _dvariance; // decaying variance
  double _alpha; // factor for the decaying average / variance

  // This is what we divide with to get the average. In a standard
  // number sequence, this should just be the number of elements in it.
  virtual double total() const { return (double) _num; };

public:
  AbsSeq(double alpha = DEFAULT_ALPHA_VALUE);

  virtual void add(double val); // adds a new element to the sequence
  void add(unsigned val) { add((double) val); }
  virtual double maximum() const = 0; // maximum element in the sequence
  virtual double last() const = 0; // last element added in the sequence

  // the number of elements in the sequence
  int num() const { return _num; }
  // the sum of the elements in the sequence
  double sum() const { return _sum; }

  double avg() const; // the average of the sequence
  double variance() const; // the variance of the sequence
  double sd() const; // the standard deviation of the sequence

  double davg() const; // decaying average
  double dvariance() const; // decaying variance
  double dsd() const; // decaying "standard deviation"

  // Debugging/Printing
  virtual void dump();
  virtual void dump_on(outputStream* s);
};

class NumberSeq: public AbsSeq {
private:
  bool check_nums(NumberSeq* total, int n, NumberSeq** parts);

protected:
  double _last;
  double _maximum; // keep track of maximum value

public:
  NumberSeq(double alpha = DEFAULT_ALPHA_VALUE);

  virtual void add(double val);
  virtual double maximum() const { return _maximum; }
  virtual double last() const { return _last; }

  // Debugging/Printing
  virtual void dump_on(outputStream* s);
};

class TruncatedSeq: public AbsSeq {
private:
  enum PrivateConstants {
    DefaultSeqLength = 10
  };
  void init();
protected:
  double *_sequence; // buffers the last L elements in the sequence
  int     _length; // this is L
  int     _next;   // oldest slot in the array, i.e. next to be overwritten

public:
  // accepts a value for L
  TruncatedSeq(int length = DefaultSeqLength,
               double alpha = DEFAULT_ALPHA_VALUE);
  ~TruncatedSeq();
  virtual void add(double val);
  virtual double maximum() const;
  virtual double last() const; // the last value added to the sequence

  double oldest() const; // the oldest valid value in the sequence
  double predict_next() const; // prediction based on linear regression

  // Debugging/Printing
  virtual void dump_on(outputStream* s);
};

#endif // SHARE_UTILITIES_NUMBERSEQ_HPP
