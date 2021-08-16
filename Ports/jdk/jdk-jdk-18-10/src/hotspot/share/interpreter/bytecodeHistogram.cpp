/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include "interpreter/bytecodeHistogram.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "utilities/growableArray.hpp"

// ------------------------------------------------------------------------------------------------
// Non-product code
#ifndef PRODUCT

// Implementation of BytecodeCounter

int   BytecodeCounter::_counter_value = 0;
jlong BytecodeCounter::_reset_time    = 0;


void BytecodeCounter::reset() {
  _counter_value = 0;
  _reset_time    = os::elapsed_counter();
}


double BytecodeCounter::elapsed_time() {
  return (double)(os::elapsed_counter() - _reset_time) / (double)os::elapsed_frequency();
}


double BytecodeCounter::frequency() {
  return (double)counter_value() / elapsed_time();
}


void BytecodeCounter::print() {
  tty->print_cr(
    "%d bytecodes executed in %.1fs (%.3fMHz)",
    counter_value(),
    elapsed_time(),
    frequency() / 1000000.0
  );
}


// Helper class for sorting

class HistoEntry: public ResourceObj {
 private:
  int             _index;
  int             _count;

 public:
  HistoEntry(int index, int count)                         { _index = index; _count = count; }
  int             index() const                            { return _index; }
  int             count() const                            { return _count; }

  static int      compare(HistoEntry** x, HistoEntry** y)  { return (*x)->count() - (*y)->count(); }
};


// Helper functions

static GrowableArray<HistoEntry*>* sorted_array(int* array, int length) {
  GrowableArray<HistoEntry*>* a = new GrowableArray<HistoEntry*>(length);
  int i = length;
  while (i-- > 0) a->append(new HistoEntry(i, array[i]));
  a->sort(HistoEntry::compare);
  return a;
}


static int total_count(GrowableArray<HistoEntry*>* profile) {
  int sum = 0;
  int i = profile->length();
  while (i-- > 0) sum += profile->at(i)->count();
  return sum;
}


static const char* name_for(int i) {
  return Bytecodes::is_defined(i) ? Bytecodes::name(Bytecodes::cast(i)) : "xxxunusedxxx";
}


// Implementation of BytecodeHistogram

int BytecodeHistogram::_counters[Bytecodes::number_of_codes];


void BytecodeHistogram::reset() {
  int i = Bytecodes::number_of_codes;
  while (i-- > 0) _counters[i] = 0;
}


void BytecodeHistogram::print(float cutoff) {
  ResourceMark rm;
  GrowableArray<HistoEntry*>* profile = sorted_array(_counters, Bytecodes::number_of_codes);
  // print profile
  int tot     = total_count(profile);
  int abs_sum = 0;
  tty->cr();   //0123456789012345678901234567890123456789012345678901234567890123456789
  tty->print_cr("Histogram of %d executed bytecodes:", tot);
  tty->cr();
  tty->print_cr("  absolute  relative  code    name");
  tty->print_cr("----------------------------------------------------------------------");
  int i = profile->length();
  while (i-- > 0) {
    HistoEntry* e = profile->at(i);
    int       abs = e->count();
    float     rel = abs * 100.0F / tot;
    if (cutoff <= rel) {
      tty->print_cr("%10d  %7.2f%%    %02x    %s", abs, rel, e->index(), name_for(e->index()));
      abs_sum += abs;
    }
  }
  tty->print_cr("----------------------------------------------------------------------");
  float rel_sum = abs_sum * 100.0F / tot;
  tty->print_cr("%10d  %7.2f%%    (cutoff = %.2f%%)", abs_sum, rel_sum, cutoff);
  tty->cr();
}


// Implementation of BytecodePairHistogram

int BytecodePairHistogram::_index;
int BytecodePairHistogram::_counters[BytecodePairHistogram::number_of_pairs];


void BytecodePairHistogram::reset() {
  _index = Bytecodes::_nop << log2_number_of_codes;

  int i = number_of_pairs;
  while (i-- > 0) _counters[i] = 0;
}


void BytecodePairHistogram::print(float cutoff) {
  ResourceMark rm;
  GrowableArray<HistoEntry*>* profile = sorted_array(_counters, number_of_pairs);
  // print profile
  int tot     = total_count(profile);
  int abs_sum = 0;
  tty->cr();   //0123456789012345678901234567890123456789012345678901234567890123456789
  tty->print_cr("Histogram of %d executed bytecode pairs:", tot);
  tty->cr();
  tty->print_cr("  absolute  relative    codes    1st bytecode        2nd bytecode");
  tty->print_cr("----------------------------------------------------------------------");
  int i = profile->length();
  while (i-- > 0) {
    HistoEntry* e = profile->at(i);
    int       abs = e->count();
    float     rel = abs * 100.0F / tot;
    if (cutoff <= rel) {
      int   c1 = e->index() % number_of_codes;
      int   c2 = e->index() / number_of_codes;
      tty->print_cr("%10d   %6.3f%%    %02x %02x    %-19s %s", abs, rel, c1, c2, name_for(c1), name_for(c2));
      abs_sum += abs;
    }
  }
  tty->print_cr("----------------------------------------------------------------------");
  float rel_sum = abs_sum * 100.0F / tot;
  tty->print_cr("%10d   %6.3f%%    (cutoff = %.3f%%)", abs_sum, rel_sum, cutoff);
  tty->cr();
}

#endif
