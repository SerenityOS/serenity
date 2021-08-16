/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_SERVICE_JFREVENT_HPP
#define SHARE_JFR_RECORDER_SERVICE_JFREVENT_HPP

#include "jfr/recorder/jfrEventSetting.inline.hpp"
#include "jfr/recorder/service/jfrEventThrottler.hpp"
#include "jfr/recorder/stacktrace/jfrStackTraceRepository.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "jfr/writers/jfrNativeEventWriter.hpp"
#include "runtime/thread.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/ticks.hpp"
#ifdef ASSERT
#include "utilities/bitMap.hpp"
#endif

#ifdef ASSERT
class JfrEventVerifier {
  template <typename>
  friend class JfrEvent;
 private:
  // verification of event fields
  BitMap::bm_word_t _verification_storage[1];
  BitMapView _verification_bit_map;
  bool _committed;

  JfrEventVerifier();
  void check(BitMap::idx_t field_idx) const;
  void set_field_bit(size_t field_idx);
  bool verify_field_bit(size_t field_idx) const;
  void set_committed();
  void clear_committed();
  bool committed() const;
};
#endif // ASSERT

template <typename T>
class JfrEvent {
 private:
  jlong _start_time;
  jlong _end_time;
  bool _started;
  bool _untimed;
  bool _should_commit;
  bool _evaluated;

 protected:
  JfrEvent(EventStartTime timing=TIMED) : _start_time(0), _end_time(0),
                                          _started(false), _untimed(timing == UNTIMED),
                                          _should_commit(false), _evaluated(false)
#ifdef ASSERT
  , _verifier()
#endif
  {
    if (T::is_enabled()) {
      _started = true;
      if (TIMED == timing && !T::isInstant) {
        set_starttime(JfrTicks::now());
      }
    }
  }

  void commit() {
    assert(!_verifier.committed(), "event already committed");
    if (!should_write()) {
      return;
    }
    write_event();
    DEBUG_ONLY(_verifier.set_committed();)
  }

 public:
  void set_starttime(const JfrTicks& time) {
    _start_time = time.value();
  }

  void set_endtime(const JfrTicks& time) {
    _end_time = time.value();
  }

  void set_starttime(const Ticks& time) {
    _start_time = JfrTime::is_ft_enabled() ? time.ft_value() : time.value();
  }

  void set_endtime(const Ticks& time) {
    _end_time = JfrTime::is_ft_enabled() ? time.ft_value() : time.value();
  }

  static bool is_enabled() {
    return JfrEventSetting::is_enabled(T::eventId);
  }

  static bool is_stacktrace_enabled() {
    return JfrEventSetting::has_stacktrace(T::eventId);
  }

  static bool is_large() {
    return JfrEventSetting::is_large(T::eventId);
  }

  static void set_large() {
    JfrEventSetting::set_large(T::eventId);
  }

  static JfrEventId id() {
    return T::eventId;
  }

  static bool is_instant() {
    return T::isInstant;
  }

  static bool is_requestable() {
    return T::isRequestable;
  }

  static bool has_thread() {
    return T::hasThread;
  }

  static bool has_stacktrace() {
    return T::hasStackTrace;
  }

  bool is_started() const {
    return _started;
  }

  bool should_commit() {
    if (!_started) {
      return false;
    }
    if (_untimed) {
      return true;
    }
    if (_evaluated) {
      return _should_commit;
    }
    _should_commit = evaluate();
    _evaluated = true;
    return _should_commit;
  }

 private:
  bool should_write() {
    return _started && (_evaluated ? _should_commit : evaluate());
  }

  bool evaluate() {
    assert(_started, "invariant");
    if (_start_time == 0) {
      set_starttime(JfrTicks::now());
    } else if (_end_time == 0) {
      set_endtime(JfrTicks::now());
    }
    if (T::isInstant || T::isRequestable) {
      return T::hasThrottle ? JfrEventThrottler::accept(T::eventId, _untimed ? 0 : _start_time) : true;
    }
    if (_end_time - _start_time < JfrEventSetting::threshold(T::eventId)) {
      return false;
    }
    return T::hasThrottle ? JfrEventThrottler::accept(T::eventId, _untimed ? 0 : _end_time) : true;
  }

  void write_event() {
    DEBUG_ONLY(assert_precondition();)
    Thread* const event_thread = Thread::current();
    JfrThreadLocal* const tl = event_thread->jfr_thread_local();
    JfrBuffer* const buffer = tl->native_buffer();
    if (buffer == NULL) {
      // most likely a pending OOM
      return;
    }
    bool large = is_large();
    if (write_sized_event(buffer, event_thread, tl, large)) {
      // Event written succesfully
      return;
    }
    if (!large) {
      // Try large size
      if (write_sized_event(buffer, event_thread, tl, true)) {
        // Event written succesfully, use large size from now on
        set_large();
      }
    }
  }

  bool write_sized_event(JfrBuffer* const buffer, Thread* const event_thread, JfrThreadLocal* const tl, bool large_size) {
    JfrNativeEventWriter writer(buffer, event_thread);
    writer.begin_event_write(large_size);
    writer.write<u8>(T::eventId);
    assert(_start_time != 0, "invariant");
    writer.write(_start_time);
    if (!(T::isInstant || T::isRequestable) || T::hasCutoff) {
      assert(_end_time != 0, "invariant");
      writer.write(_end_time - _start_time);
    }
    if (T::hasThread) {
      writer.write(tl->thread_id());
    }
    if (T::hasStackTrace) {
      if (is_stacktrace_enabled()) {
        if (tl->has_cached_stack_trace()) {
          writer.write(tl->cached_stack_trace_id());
        } else {
          writer.write(JfrStackTraceRepository::record(event_thread));
        }
      } else {
        writer.write<traceid>(0);
      }
    }
    // payload
    static_cast<T*>(this)->writeData(writer);
    return writer.end_event_write(large_size) > 0;
  }

#ifdef ASSERT
 private:
  // verification of event fields
  JfrEventVerifier _verifier;

  void assert_precondition() {
    assert(T::eventId >= FIRST_EVENT_ID, "event id underflow invariant");
    assert(T::eventId <= LAST_EVENT_ID, "event id overflow invariant");
    DEBUG_ONLY(static_cast<T*>(this)->verify());
  }

 protected:
  void set_field_bit(size_t field_idx) {
    _verifier.set_field_bit(field_idx);
    // it is ok to reuse an already committed event
    // granted you provide new informational content
    _verifier.clear_committed();
  }

  bool verify_field_bit(size_t field_idx) const {
    return _verifier.verify_field_bit(field_idx);
  }
#endif // ASSERT
};

#endif // SHARE_JFR_RECORDER_SERVICE_JFREVENT_HPP
