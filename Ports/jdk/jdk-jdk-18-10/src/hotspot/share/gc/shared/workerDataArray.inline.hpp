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

#ifndef SHARE_GC_SHARED_WORKERDATAARRAY_INLINE_HPP
#define SHARE_GC_SHARED_WORKERDATAARRAY_INLINE_HPP

#include "gc/shared/workerDataArray.hpp"

#include "memory/allocation.inline.hpp"
#include "utilities/ostream.hpp"

template <typename T>
WorkerDataArray<T>::WorkerDataArray(const char* short_name, const char* title, uint length, bool is_serial) :
 _data(NULL),
 _length(length),
 _short_name(short_name),
 _title(title),
 _is_serial(is_serial) {
  assert(length > 0, "Must have some workers to store data for");
  assert(!is_serial || length == 1, "Serial phase must only have a single entry.");
  _data = NEW_C_HEAP_ARRAY(T, _length, mtGC);
  for (uint i = 0; i < MaxThreadWorkItems; i++) {
    _thread_work_items[i] = NULL;
  }
  reset();
}

template <typename T>
void WorkerDataArray<T>::set(uint worker_i, T value) {
  assert(worker_i < _length, "Worker %d is greater than max: %d", worker_i, _length);
  assert(_data[worker_i] == uninitialized(), "Overwriting data for worker %d in %s", worker_i, _title);
  _data[worker_i] = value;
}

template <typename T>
void WorkerDataArray<T>::set_or_add(uint worker_i, T value) {
  assert(worker_i < _length, "Worker %d is greater than max: %d", worker_i, _length);
  if (_data[worker_i] == uninitialized()) {
    _data[worker_i] = value;
  } else {
    _data[worker_i] += value;
  }
}

template <typename T>
T WorkerDataArray<T>::get(uint worker_i) const {
  assert(worker_i < _length, "Worker %d is greater than max: %d", worker_i, _length);
  return _data[worker_i];
}

template <typename T>
WorkerDataArray<T>::~WorkerDataArray() {
  for (uint i = 0; i < MaxThreadWorkItems; i++) {
    delete _thread_work_items[i];
  }
  FREE_C_HEAP_ARRAY(T, _data);
}

template <typename T>
void WorkerDataArray<T>::create_thread_work_items(const char* title, uint index, uint length_override) {
  assert(index < MaxThreadWorkItems, "Tried to access thread work item %u (max %u)", index, MaxThreadWorkItems);
  assert(_thread_work_items[index] == NULL, "Tried to overwrite existing thread work item");
  uint length = length_override != 0 ? length_override : _length;
  _thread_work_items[index] = new WorkerDataArray<size_t>(NULL, title, length);
}

template <typename T>
void WorkerDataArray<T>::set_thread_work_item(uint worker_i, size_t value, uint index) {
  assert(index < MaxThreadWorkItems, "Tried to access thread work item %u (max %u)", index, MaxThreadWorkItems);
  assert(_thread_work_items[index] != NULL, "No sub count");
  _thread_work_items[index]->set(worker_i, value);
}

template <typename T>
void WorkerDataArray<T>::add_thread_work_item(uint worker_i, size_t value, uint index) {
  assert(index < MaxThreadWorkItems, "Tried to access thread work item %u (max %u)", index, MaxThreadWorkItems);
  assert(_thread_work_items[index] != NULL, "No sub count");
  _thread_work_items[index]->add(worker_i, value);
}

template <typename T>
void WorkerDataArray<T>::set_or_add_thread_work_item(uint worker_i, size_t value, uint index) {
  assert(index < MaxThreadWorkItems, "Tried to access thread work item %u (max %u)", index, MaxThreadWorkItems);
  assert(_thread_work_items[index] != NULL, "No sub count");
  if (_thread_work_items[index]->get(worker_i) == _thread_work_items[index]->uninitialized()) {
    _thread_work_items[index]->set(worker_i, value);
  } else {
    _thread_work_items[index]->add(worker_i, value);
  }
}

template <typename T>
size_t WorkerDataArray<T>::get_thread_work_item(uint worker_i, uint index) {
  assert(index < MaxThreadWorkItems, "Tried to access thread work item %u (max %u)", index, MaxThreadWorkItems);
  assert(_thread_work_items[index] != NULL, "No sub count");
  return _thread_work_items[index]->get(worker_i);
}

template <typename T>
void WorkerDataArray<T>::add(uint worker_i, T value) {
  assert(worker_i < _length, "Worker %d is greater than max: %d", worker_i, _length);
  assert(_data[worker_i] != uninitialized(), "No data to add to %s for worker %d", _title, worker_i);
  _data[worker_i] += value;
}

template <typename T>
double WorkerDataArray<T>::average() const {
  uint contributing_threads = 0;
  for (uint i = 0; i < _length; ++i) {
    if (get(i) != uninitialized()) {
      contributing_threads++;
    }
  }
  if (contributing_threads == 0) {
    return 0.0;
  }
  return sum() / (double) contributing_threads;
}

template <typename T>
T WorkerDataArray<T>::sum() const {
  T s = 0;
  for (uint i = 0; i < _length; ++i) {
    if (get(i) != uninitialized()) {
      s += get(i);
    }
  }
  return s;
}

template <typename T>
void WorkerDataArray<T>::set_all(T value) {
  for (uint i = 0; i < _length; i++) {
    _data[i] = value;
  }
}

template <class T>
void WorkerDataArray<T>::print_summary_on(outputStream* out, bool print_sum) const {
  if (_is_serial) {
    out->print("%s:", title());
  } else {
    out->print("%-30s", title());
  }

  uint start = 0;
  while (start < _length && get(start) == uninitialized()) {
    start++;
  }
  if (start < _length) {
    if (_is_serial) {
      WDAPrinter::summary(out, get(0));
    } else {
      T min = get(start);
      T max = min;
      T sum = 0;
      uint contributing_threads = 0;
      for (uint i = start; i < _length; ++i) {
        T value = get(i);
        if (value != uninitialized()) {
          max = MAX2(max, value);
          min = MIN2(min, value);
          sum += value;
          contributing_threads++;
        }
      }
      T diff = max - min;
      assert(contributing_threads != 0, "Must be since we found a used value for the start index");
      double avg = sum / (double) contributing_threads;
      WDAPrinter::summary(out, min, avg, max, diff, sum, print_sum);
      out->print_cr(", Workers: %d", contributing_threads);
    }
  } else {
    // No data for this phase.
    out->print_cr(" skipped");
  }
}

template <class T>
void WorkerDataArray<T>::print_details_on(outputStream* out) const {
  WDAPrinter::details(this, out);
}

template <typename T>
void WorkerDataArray<T>::reset() {
  set_all(uninitialized());
  for (uint i = 0; i < MaxThreadWorkItems; i++) {
    if (_thread_work_items[i] != NULL) {
      _thread_work_items[i]->reset();
    }
  }
}

#endif // SHARE_GC_SHARED_WORKERDATAARRAY_INLINE_HPP
