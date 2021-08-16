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

#ifndef SHARE_GC_SHARED_WORKERDATAARRAY_HPP
#define SHARE_GC_SHARED_WORKERDATAARRAY_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

class outputStream;

template <class T>
class WorkerDataArray  : public CHeapObj<mtGC> {
  friend class WDAPrinter;
public:
  static const uint MaxThreadWorkItems = 9;
private:
  T*          _data;
  uint        _length;
  const char* _short_name; // Short name for JFR
  const char* _title; // Title for logging.

  bool _is_serial;

  WorkerDataArray<size_t>* _thread_work_items[MaxThreadWorkItems];

 public:
  WorkerDataArray(const char* short_name, const char* title, uint length, bool is_serial = false);
  ~WorkerDataArray();

  // Create an integer sub-item at the given index to this WorkerDataArray. If length_override
  // is zero, use the same number of elements as this array, otherwise use the given
  // number.
  void create_thread_work_items(const char* title, uint index = 0, uint length_override = 0);

  void set_thread_work_item(uint worker_i, size_t value, uint index = 0);
  void add_thread_work_item(uint worker_i, size_t value, uint index = 0);
  void set_or_add_thread_work_item(uint worker_i, size_t value, uint index = 0);
  size_t get_thread_work_item(uint worker_i, uint index = 0);

  WorkerDataArray<size_t>* thread_work_items(uint index = 0) const {
    assert(index < MaxThreadWorkItems, "Tried to access thread work item %u max %u", index, MaxThreadWorkItems);
    return _thread_work_items[index];
  }

  static T uninitialized();

  void set(uint worker_i, T value);
  void set_or_add(uint worker_i, T value);
  T get(uint worker_i) const;

  void add(uint worker_i, T value);

  // The sum() and average() methods below consider uninitialized slots to be 0.
  double average() const;
  T sum() const;

  const char* title() const {
    return _title;
  }

  const char* short_name() const {
    return _short_name;
  }

  void reset();
  void set_all(T value);


 private:
  class WDAPrinter {
  public:
    static void summary(outputStream* out, double time);
    static void summary(outputStream* out, double min, double avg, double max, double diff, double sum, bool print_sum);
    static void summary(outputStream* out, size_t value);
    static void summary(outputStream* out, size_t min, double avg, size_t max, size_t diff, size_t sum, bool print_sum);

    static void details(const WorkerDataArray<double>* phase, outputStream* out);
    static void details(const WorkerDataArray<size_t>* phase, outputStream* out);
  };

 public:
  void print_summary_on(outputStream* out, bool print_sum = true) const;
  void print_details_on(outputStream* out) const;
};

#endif // SHARE_GC_SHARED_WORKERDATAARRAY_HPP
