/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "logging/logConfiguration.hpp"
#include "logging/logDecorations.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "services/management.hpp"

const char* volatile LogDecorations::_host_name = NULL;
const int LogDecorations::_pid = os::current_process_id(); // This is safe to call during dynamic initialization.

const char* LogDecorations::host_name() {
  const char* host_name = Atomic::load_acquire(&_host_name);
  if (host_name == NULL) {
    char buffer[1024];
    if (os::get_host_name(buffer, sizeof(buffer))) {
      host_name = os::strdup_check_oom(buffer);
      const char* old_value = Atomic::cmpxchg(&_host_name, (const char*)NULL, host_name);
      if (old_value != NULL) {
        os::free((void *) host_name);
        host_name = old_value;
      }
    }
  }
  return host_name;
}

LogDecorations::LogDecorations(LogLevelType level, const LogTagSet &tagset, const LogDecorators &decorators) :
  // When constructing the LogDecorations we resolve values for the requested decorators.
  //
  // _millis: needed for "time", "utctime", "timemillis":
  _millis(
      (decorators.is_decorator(LogDecorators::time_decorator) ||
       decorators.is_decorator(LogDecorators::utctime_decorator) ||
       decorators.is_decorator(LogDecorators::timemillis_decorator)) ? os::javaTimeMillis() : 0),
  // _nanos: needed for "timenanos"
  _nanos(decorators.is_decorator(LogDecorators::timenanos_decorator) ? os::javaTimeNanos() : 0),
  // _elapsed_seconds: needed for "uptime", "uptimemillis", "uptimenanos"
  _elapsed_seconds(
      (decorators.is_decorator(LogDecorators::uptime_decorator) ||
       decorators.is_decorator(LogDecorators::uptimemillis_decorator) ||
       decorators.is_decorator(LogDecorators::uptimenanos_decorator)) ? os::elapsedTime() : 0),
  // tid
  _tid(decorators.is_decorator(LogDecorators::tid_decorator) ? os::current_thread_id() : 0),
  // the rest is handed down by the caller
  _level(level), _tagset(tagset)
#ifdef ASSERT
  , _decorators(decorators)
#endif
{
}

void LogDecorations::print_decoration(LogDecorators::Decorator decorator, outputStream* st) const {
  assert(_decorators.is_decorator(decorator), "decorator was not part of the decorator set specified at creation.");
  switch(decorator) {
#define DECORATOR(name, abbr) case LogDecorators:: name##_decorator: print_##name##_decoration(st); break;
  DECORATOR_LIST
#undef DECORATOR
    default: ShouldNotReachHere();
  }
}

const char* LogDecorations::decoration(LogDecorators::Decorator decorator, char* buf, size_t buflen) const {
  stringStream ss(buf, buflen);
  print_decoration(decorator, &ss);
  return buf;
}

void LogDecorations::print_time_decoration(outputStream* st) const {
  char buf[os::iso8601_timestamp_size];
  char* result = os::iso8601_time(_millis, buf, sizeof(buf), false);
  st->print_raw(result ? result : "");
}

void LogDecorations::print_utctime_decoration(outputStream* st) const {
  char buf[os::iso8601_timestamp_size];
  char* result = os::iso8601_time(_millis, buf, sizeof(buf), true);
  st->print_raw(result ? result : "");
}

void LogDecorations::print_uptime_decoration(outputStream* st) const {
  st->print("%.3fs", _elapsed_seconds);
}

void LogDecorations::print_timemillis_decoration(outputStream* st) const {
  st->print(INT64_FORMAT "ms", (int64_t)_millis);
}

void LogDecorations::print_uptimemillis_decoration(outputStream* st) const {
  st->print(INT64_FORMAT "ms", (int64_t)(_elapsed_seconds * MILLIUNITS));
}

void LogDecorations::print_timenanos_decoration(outputStream* st) const {
  st->print(INT64_FORMAT "ns", (int64_t)_nanos);
}

void LogDecorations::print_uptimenanos_decoration(outputStream* st) const {
  st->print(INT64_FORMAT "ns", (int64_t)(_elapsed_seconds * NANOUNITS));
}

void LogDecorations::print_pid_decoration(outputStream* st) const {
  st->print("%d", _pid);
}

void LogDecorations::print_tid_decoration(outputStream* st) const {
  st->print(INTX_FORMAT, _tid);
}

void LogDecorations::print_level_decoration(outputStream* st) const {
  st->print_raw(LogLevel::name(_level));
}

void LogDecorations::print_tags_decoration(outputStream* st) const {
  _tagset.label(st);
}

void LogDecorations::print_hostname_decoration(outputStream* st) const {
  st->print_raw(host_name());
}
