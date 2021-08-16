/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_SUPPORT_JFRFLUSH_HPP
#define SHARE_JFR_SUPPORT_JFRFLUSH_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"

class Thread;

class JfrFlush : public StackObj {
 public:
  typedef JfrBuffer Type;
  JfrFlush(Type* old, size_t used, size_t requested, Thread* t);
  Type* result() const { return _result; }
 private:
  Type* _result;
};

void jfr_conditional_flush(JfrEventId id, size_t size, Thread* t);
bool jfr_is_event_enabled(JfrEventId id);
bool jfr_has_stacktrace_enabled(JfrEventId id);
bool jfr_save_stacktrace(Thread* t);
void jfr_clear_stacktrace(Thread* t);

template <typename Event>
class JfrConditionalFlush {
 protected:
  bool _enabled;
 public:
  typedef JfrBuffer Type;
  JfrConditionalFlush(Thread* t) : _enabled(jfr_is_event_enabled(Event::eventId)) {
    if (_enabled) {
      jfr_conditional_flush(Event::eventId, sizeof(Event), t);
    }
  }
};

template <typename Event>
class JfrConditionalFlushWithStacktrace : public JfrConditionalFlush<Event> {
  Thread* _t;
  bool _owner;
 public:
  JfrConditionalFlushWithStacktrace(Thread* t) : JfrConditionalFlush<Event>(t), _t(t), _owner(false) {
    if (this->_enabled && Event::has_stacktrace() && jfr_has_stacktrace_enabled(Event::eventId)) {
      _owner = jfr_save_stacktrace(t);
    }
  }
  ~JfrConditionalFlushWithStacktrace() {
    if (_owner) {
      jfr_clear_stacktrace(_t);
    }
  }
};

#endif // SHARE_JFR_SUPPORT_JFRFLUSH_HPP
