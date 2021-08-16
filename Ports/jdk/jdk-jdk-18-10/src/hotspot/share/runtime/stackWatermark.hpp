/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_STACKWATERMARK_HPP
#define SHARE_RUNTIME_STACKWATERMARK_HPP

#include "memory/allocation.hpp"
#include "runtime/mutex.hpp"
#include "runtime/stackWatermarkKind.hpp"

class frame;
class JavaThread;
class RegisterMap;
class StackWatermarkFramesIterator;

// The StackWatermark state is a tuple comprising the last epoch in which
// the watermark has been processed, and a boolean denoting whether the whole
// processing of the lazy snapshot has been processed or not. It is written
// in a way that can be used outside of locks, so that fast path checks can
// be performed without the need for any locking. The boolean can only be
// trusted if the epoch of the state is the same as the epoch_id() of the
// watermark. Incrementing the epoch_id() will implicitly initiate a new lazy
// stack snapshot, and trigger processing on it as needed, due to the cached
// epoch of the state being outdated. When the snapshot is_done for the current
// epoch_id(), there is no need to do anything further.
class StackWatermarkState : public AllStatic {
public:
  inline static bool is_done(uint32_t state) {
    return state & 1;
  }

  inline static uint32_t epoch(uint32_t state) {
    return state >> 1;
  }

  inline static uint32_t create(uint32_t epoch, bool is_done) {
    return (epoch << 1) | (is_done ? 1u : 0u);
  }
};

// The StackWatermark allows lazy incremental concurrent processing of a
// snapshot of a stack. The lazy and incremental nature is implemented by
// marking a frame (the watermark) from which returns (or other forms of
// unwinding) will take a slow path to perform additional processing
// required when exposing more frames that were part of the snapshot to
// the system. The watermark pointer always denotes the SP of the watermark.
// However, active frames can grow and shrink arbitrarily compared to the
// snapshot view that is being processed, due to things like c2i adapters,
// and various register saving techniques to get into the runtime. Therefore,
// in order to cope with the frames growing and shrinking, comparisons
// against the watermark are performed with the frame pointer of a given
// frame against the watermark (denoting the SP).
//
//  ----------
// |          |
// |  caller  |
// |          |
//  ----------
// |          | <-- frame fp  (always above the watermark of the same frame,
// |  callee  |                regardless of frame resizing)
// |          |
//  ----------  <-- watermark (callee SP from the snapshot, SP at the
//                             point of unwinding, might be above or below
//                             due to frame resizing)
class StackWatermark : public CHeapObj<mtInternal> {
  friend class StackWatermarkFramesIterator;
protected:
  volatile uint32_t _state;
  volatile uintptr_t _watermark;
  StackWatermark* _next;
  JavaThread* _jt;
  StackWatermarkFramesIterator* _iterator;
  Mutex _lock;
  StackWatermarkKind _kind;
  StackWatermark* _linked_watermark;

  void process_one();

  void update_watermark();
  void yield_processing();
  static bool has_barrier(const frame& f);
  void ensure_safe(const frame& f);
  void assert_is_frame_safe(const frame& f) NOT_DEBUG_RETURN;
  bool is_frame_safe(const frame& f);

  // API for consumers of the stack watermark barrier.
  // The rule for consumers is: do not perform thread transitions
  // or take locks of rank >= special. This is all very special code.
  virtual uint32_t epoch_id() const = 0;
  virtual void process(const frame& f, RegisterMap& register_map, void* context) = 0;
  virtual void start_processing_impl(void* context);

  // Set process_on_iteration to false if you don't want to move the
  // watermark when new frames are discovered from stack walkers, as
  // opposed to due to frames being unwound by the owning thread.
  virtual bool process_on_iteration() { return true; }

  bool processing_started(uint32_t state) const;
  bool processing_completed(uint32_t state) const;

public:
  StackWatermark(JavaThread* jt, StackWatermarkKind kind, uint32_t epoch);
  virtual ~StackWatermark();


  // StackWatermarkSet support
  StackWatermarkKind kind() const { return _kind; }
  StackWatermark* next() const { return _next; }
  void set_next(StackWatermark* n) { _next = n; }

  void link_watermark(StackWatermark* watermark);
  DEBUG_ONLY(StackWatermark* linked_watermark() const { return _linked_watermark; })

  uintptr_t watermark();
  uintptr_t last_processed();

  bool processing_started() const;
  bool processing_started_acquire() const;
  bool processing_completed() const;
  bool processing_completed_acquire() const;

  void before_unwind();
  void after_unwind();

  void on_iteration(const frame& f);
  void on_safepoint();
  void start_processing();
  void finish_processing(void* context);
};

#endif // SHARE_RUNTIME_STACKWATERMARK_HPP
