/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_SAMPLING_OBJECTSAMPLE_HPP
#define SHARE_JFR_LEAKPROFILER_SAMPLING_OBJECTSAMPLE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrBlob.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "oops/weakHandle.hpp"
#include "utilities/ticks.hpp"

/*
 * Handle for diagnosing Java memory leaks.
 *
 * The class tracks the time the object was
 * allocated, the thread and the stack trace.
 */
class ObjectSample : public JfrCHeapObj {
  friend class ObjectSampler;
  friend class SampleList;
 private:
  ObjectSample* _next;
  ObjectSample* _previous;
  JfrBlobHandle _stacktrace;
  JfrBlobHandle _thread;
  JfrBlobHandle _type_set;
  WeakHandle    _object;
  Ticks _allocation_time;
  traceid _stack_trace_id;
  traceid _thread_id;
  int _index;
  size_t _span;
  size_t _allocated;
  size_t _heap_used_at_last_gc;
  unsigned int _stack_trace_hash;

  void release_references() {
    _stacktrace.~JfrBlobHandle();
    _thread.~JfrBlobHandle();
    _type_set.~JfrBlobHandle();
  }

  void reset();

 public:
  ObjectSample() : _next(NULL),
                   _previous(NULL),
                   _stacktrace(),
                   _thread(),
                   _type_set(),
                   _allocation_time(),
                   _stack_trace_id(0),
                   _thread_id(0),
                   _index(0),
                   _span(0),
                   _allocated(0),
                   _heap_used_at_last_gc(0),
                   _stack_trace_hash(0) {}

  ObjectSample* next() const {
    return _next;
  }

  void set_next(ObjectSample* next) {
    _next = next;
  }

  ObjectSample* prev() const {
    return _previous;
  }

  void set_prev(ObjectSample* prev) {
    _previous = prev;
  }

  bool is_dead() const;

  const oop object() const;
  void set_object(oop object);

  const oop* object_addr() const;

  void release();

  int index() const {
    return _index;
  }

  void set_index(int index) {
    _index = index;
  }

  size_t span() const {
    return _span;
  }

  void set_span(size_t span) {
    _span = span;
  }

  void add_span(size_t span) {
    _span += span;
  }

  size_t allocated() const {
    return _allocated;
  }

  void set_allocated(size_t size) {
    _allocated = size;
  }

  const Ticks& allocation_time() const {
    return _allocation_time;
  }

  const void set_allocation_time(const JfrTicks& time) {
    _allocation_time = Ticks(time.value());
  }

  void set_heap_used_at_last_gc(size_t heap_used) {
    _heap_used_at_last_gc = heap_used;
  }

  size_t heap_used_at_last_gc() const {
    return _heap_used_at_last_gc;
  }

  bool has_stack_trace_id() const {
    return stack_trace_id() != 0;
  }

  traceid stack_trace_id() const {
    return _stack_trace_id;
  }

  void set_stack_trace_id(traceid id) {
    _stack_trace_id = id;
  }

  unsigned int stack_trace_hash() const {
    return _stack_trace_hash;
  }

  void set_stack_trace_hash(unsigned int hash) {
    _stack_trace_hash = hash;
  }

  traceid thread_id() const {
    return _thread_id;
  }

  void set_thread_id(traceid id) {
    _thread_id = id;
  }

  bool is_alive_and_older_than(jlong time_stamp) const {
    return !is_dead() && (JfrTime::is_ft_enabled() ?
      _allocation_time.ft_value() : _allocation_time.value()) < time_stamp;
  }

  const JfrBlobHandle& stacktrace() const {
    return _stacktrace;
  }

  bool has_stacktrace() const {
    return _stacktrace.valid();
  }

  // JfrBlobHandle assignment operator
  // maintains proper reference counting
  void set_stacktrace(const JfrBlobHandle& ref) {
    if (_stacktrace != ref) {
      _stacktrace = ref;
    }
  }

  const JfrBlobHandle& thread() const {
    return _thread;
  }

  bool has_thread() const {
    return _thread.valid();
  }

  void set_thread(const JfrBlobHandle& ref) {
    if (_thread != ref) {
      _thread = ref;
    }
  }

  const JfrBlobHandle& type_set() const {
    return _type_set;
  }

  bool has_type_set() const {
    return _type_set.valid();
  }

  void set_type_set(const JfrBlobHandle& ref) {
    if (_type_set != ref) {
      if (_type_set.valid()) {
        _type_set->set_next(ref);
        return;
      }
      _type_set = ref;
    }
  }
};

#endif // SHARE_JFR_LEAKPROFILER_SAMPLING_OBJECTSAMPLE_HPP
