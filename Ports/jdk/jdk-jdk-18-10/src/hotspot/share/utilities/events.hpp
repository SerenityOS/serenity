/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_EVENTS_HPP
#define SHARE_UTILITIES_EVENTS_HPP

#include "memory/allocation.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "utilities/vmError.hpp"

// Events and EventMark provide interfaces to log events taking place in the vm.
// This facility is extremly useful for post-mortem debugging. The eventlog
// often provides crucial information about events leading up to the crash.
//
// Abstractly the logs can record whatever they way but normally they
// would record at least a timestamp and the current Thread, along
// with whatever data they need in a ring buffer.  Commonly fixed
// length text messages are recorded for simplicity but other
// strategies could be used.  Several logs are provided by default but
// new instances can be created as needed.

// The base event log dumping class that is registered for dumping at
// crash time.  This is a very generic interface that is mainly here
// for completeness.  Normally the templated EventLogBase would be
// subclassed to provide different log types.
class EventLog : public CHeapObj<mtInternal> {
  friend class Events;

 private:
  EventLog* _next;

  EventLog* next() const { return _next; }

 public:
  // Automatically registers the log so that it will be printed during
  // crashes.
  EventLog();

  // Print log to output stream.
  virtual void print_log_on(outputStream* out, int max = -1) = 0;

  // Returns true if s matches either the log name or the log handle.
  virtual bool matches_name_or_handle(const char* s) const = 0;

  // Print log names (for help output of VM.events).
  virtual void print_names(outputStream* out) const = 0;

};


// A templated subclass of EventLog that provides basic ring buffer
// functionality.  Most event loggers should subclass this, possibly
// providing a more featureful log function if the existing copy
// semantics aren't appropriate.  The name is used as the label of the
// log when it is dumped during a crash.
template <class T> class EventLogBase : public EventLog {
  template <class X> class EventRecord : public CHeapObj<mtInternal> {
   public:
    double  timestamp;
    Thread* thread;
    X       data;
  };

 protected:
  Mutex           _mutex;
  // Name is printed out as a header.
  const char*     _name;
  // Handle is a short specifier used to select this particular event log
  // for printing (see VM.events command).
  const char*     _handle;
  int             _length;
  int             _index;
  int             _count;
  EventRecord<T>* _records;

 public:
  EventLogBase<T>(const char* name, const char* handle, int length = LogEventsBufferEntries):
    _mutex(Mutex::event, name, true, Mutex::_safepoint_check_never),
    _name(name),
    _handle(handle),
    _length(length),
    _index(0),
    _count(0) {
    _records = new EventRecord<T>[length];
  }

  double fetch_timestamp() {
    return os::elapsedTime();
  }

  // move the ring buffer to next open slot and return the index of
  // the slot to use for the current message.  Should only be called
  // while mutex is held.
  int compute_log_index() {
    int index = _index;
    if (_count < _length) _count++;
    _index++;
    if (_index >= _length) _index = 0;
    return index;
  }

  bool should_log() {
    // Don't bother adding new entries when we're crashing.  This also
    // avoids mutating the ring buffer when printing the log.
    return !VMError::is_error_reported();
  }

  // Print the contents of the log
  void print_log_on(outputStream* out, int max = -1);

  // Returns true if s matches either the log name or the log handle.
  bool matches_name_or_handle(const char* s) const;

  // Print log names (for help output of VM.events).
  void print_names(outputStream* out) const;

 private:
  void print_log_impl(outputStream* out, int max = -1);

  // Print a single element.  A templated implementation might need to
  // be declared by subclasses.
  void print(outputStream* out, T& e);

  void print(outputStream* out, EventRecord<T>& e) {
    out->print("Event: %.3f ", e.timestamp);
    if (e.thread != NULL) {
      out->print("Thread " INTPTR_FORMAT " ", p2i(e.thread));
    }
    print(out, e.data);
  }
};

// A simple wrapper class for fixed size text messages.
template <size_t bufsz>
class FormatStringLogMessage : public FormatBuffer<bufsz> {
};
typedef FormatStringLogMessage<256> StringLogMessage;
typedef FormatStringLogMessage<512> ExtendedStringLogMessage;

// A simple ring buffer of fixed size text messages.
template <size_t bufsz>
class FormatStringEventLog : public EventLogBase< FormatStringLogMessage<bufsz> > {
 public:
  FormatStringEventLog(const char* name, const char* short_name, int count = LogEventsBufferEntries)
   : EventLogBase< FormatStringLogMessage<bufsz> >(name, short_name, count) {}

  void logv(Thread* thread, const char* format, va_list ap) ATTRIBUTE_PRINTF(3, 0) {
    if (!this->should_log()) return;

    double timestamp = this->fetch_timestamp();
    MutexLocker ml(&this->_mutex, Mutex::_no_safepoint_check_flag);
    int index = this->compute_log_index();
    this->_records[index].thread = thread;
    this->_records[index].timestamp = timestamp;
    this->_records[index].data.printv(format, ap);
  }

  void log(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(3, 4) {
    va_list ap;
    va_start(ap, format);
    this->logv(thread, format, ap);
    va_end(ap);
  }
};
typedef FormatStringEventLog<256> StringEventLog;
typedef FormatStringEventLog<512> ExtendedStringEventLog;

class InstanceKlass;

// Event log for class unloading events to materialize the class name in place in the log stream.
class UnloadingEventLog : public EventLogBase<StringLogMessage> {
 public:
  UnloadingEventLog(const char* name, const char* short_name, int count = LogEventsBufferEntries)
   : EventLogBase<StringLogMessage>(name, short_name, count) {}

  void log(Thread* thread, InstanceKlass* ik);
};

// Event log for exceptions
class ExceptionsEventLog : public ExtendedStringEventLog {
 public:
  ExceptionsEventLog(const char* name, const char* short_name, int count = LogEventsBufferEntries)
   : ExtendedStringEventLog(name, short_name, count) {}

  void log(Thread* thread, Handle h_exception, const char* message, const char* file, int line);
};


class Events : AllStatic {
  friend class EventLog;

 private:
  static EventLog* _logs;

  // A log for generic messages that aren't well categorized.
  static StringEventLog* _messages;

  // A log for VM Operations
  static StringEventLog* _vm_operations;

  // A log for internal exception related messages, like internal
  // throws and implicit exceptions.
  static ExceptionsEventLog* _exceptions;

  // Deoptization related messages
  static StringEventLog* _deopt_messages;

  // Redefinition related messages
  static StringEventLog* _redefinitions;

  // Class unloading events
  static UnloadingEventLog* _class_unloading;
 public:

  // Print all event logs; limit number of events per event log to be printed with max
  // (max == -1 prints all events).
  static void print_all(outputStream* out, int max = -1);

  // Print a single event log specified by name or handle.
  static void print_one(outputStream* out, const char* log_name, int max = -1);

  // Dump all events to the tty
  static void print();

  // Logs a generic message with timestamp and format as printf.
  static void log(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(2, 3);

  static void log_vm_operation(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(2, 3);

  // Log exception related message
  static void log_exception(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  static void log_exception(Thread* thread, Handle h_exception, const char* message, const char* file, int line);

  static void log_redefinition(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(2, 3);

  static void log_class_unloading(Thread* thread, InstanceKlass* ik);

  static void log_deopt_message(Thread* thread, const char* format, ...) ATTRIBUTE_PRINTF(2, 3);

  // Register default loggers
  static void init();
};

inline void Events::log(Thread* thread, const char* format, ...) {
  if (LogEvents && _messages != NULL) {
    va_list ap;
    va_start(ap, format);
    _messages->logv(thread, format, ap);
    va_end(ap);
  }
}

inline void Events::log_vm_operation(Thread* thread, const char* format, ...) {
  if (LogEvents && _vm_operations != NULL) {
    va_list ap;
    va_start(ap, format);
    _vm_operations->logv(thread, format, ap);
    va_end(ap);
  }
}

inline void Events::log_exception(Thread* thread, const char* format, ...) {
  if (LogEvents && _exceptions != NULL) {
    va_list ap;
    va_start(ap, format);
    _exceptions->logv(thread, format, ap);
    va_end(ap);
  }
}

inline void Events::log_exception(Thread* thread, Handle h_exception, const char* message, const char* file, int line) {
  if (LogEvents && _exceptions != NULL) {
    _exceptions->log(thread, h_exception, message, file, line);
  }
}

inline void Events::log_redefinition(Thread* thread, const char* format, ...) {
  if (LogEvents && _redefinitions != NULL) {
    va_list ap;
    va_start(ap, format);
    _redefinitions->logv(thread, format, ap);
    va_end(ap);
  }
}

inline void Events::log_class_unloading(Thread* thread, InstanceKlass* ik) {
  if (LogEvents && _class_unloading != NULL) {
    _class_unloading->log(thread, ik);
  }
}

inline void Events::log_deopt_message(Thread* thread, const char* format, ...) {
  if (LogEvents && _deopt_messages != NULL) {
    va_list ap;
    va_start(ap, format);
    _deopt_messages->logv(thread, format, ap);
    va_end(ap);
  }
}

template <class T>
inline void EventLogBase<T>::print_log_on(outputStream* out, int max) {
  struct MaybeLocker {
    Mutex* const _mutex;
    bool         _proceed;
    bool         _locked;

    MaybeLocker(Mutex* mutex) : _mutex(mutex), _proceed(false), _locked(false) {
      if (Thread::current_or_null() == NULL) {
        _proceed = true;
      } else if (VMError::is_error_reported()) {
        if (_mutex->try_lock_without_rank_check()) {
          _proceed = _locked = true;
        }
      } else {
        _mutex->lock_without_safepoint_check();
        _proceed = _locked = true;
      }
    }
    ~MaybeLocker() {
      if (_locked) {
        _mutex->unlock();
      }
    }
  };

  MaybeLocker ml(&_mutex);

  if (ml._proceed) {
    print_log_impl(out, max);
  } else {
    out->print_cr("%s (%d events):", _name, _count);
    out->print_cr("No events printed - crash while holding lock");
    out->cr();
  }
}

template <class T>
inline bool EventLogBase<T>::matches_name_or_handle(const char* s) const {
  return ::strcasecmp(s, _name) == 0 ||
         ::strcasecmp(s, _handle) == 0;
}

template <class T>
inline void EventLogBase<T>::print_names(outputStream* out) const {
  out->print("\"%s\" : %s", _handle, _name);
}

// Dump the ring buffer entries that current have entries.
template <class T>
inline void EventLogBase<T>::print_log_impl(outputStream* out, int max) {
  out->print_cr("%s (%d events):", _name, _count);
  if (_count == 0) {
    out->print_cr("No events");
    out->cr();
    return;
  }

  int printed = 0;
  if (_count < _length) {
    for (int i = 0; i < _count; i++) {
      if (max > 0 && printed == max) {
        break;
      }
      print(out, _records[i]);
      printed ++;
    }
  } else {
    for (int i = _index; i < _length; i++) {
      if (max > 0 && printed == max) {
        break;
      }
      print(out, _records[i]);
      printed ++;
    }
    for (int i = 0; i < _index; i++) {
      if (max > 0 && printed == max) {
        break;
      }
      print(out, _records[i]);
      printed ++;
    }
  }

  if (printed == max) {
    out->print_cr("...(skipped)");
  }

  out->cr();
}

// Implement a printing routine for the StringLogMessage
template <>
inline void EventLogBase<StringLogMessage>::print(outputStream* out, StringLogMessage& lm) {
  out->print_raw(lm);
  out->cr();
}

// Implement a printing routine for the ExtendedStringLogMessage
template <>
inline void EventLogBase<ExtendedStringLogMessage>::print(outputStream* out, ExtendedStringLogMessage& lm) {
  out->print_raw(lm);
  out->cr();
}

typedef void (*EventLogFunction)(Thread* thread, const char* format, ...);

class EventMarkBase : public StackObj {
  EventLogFunction _log_function;
  StringLogMessage _buffer;

  NONCOPYABLE(EventMarkBase);

 protected:
  void log_start(const char* format, va_list argp) ATTRIBUTE_PRINTF(2, 0);
  void log_end();

  EventMarkBase(EventLogFunction log_function);
};

// Place markers for the beginning and end up of a set of events.
template <EventLogFunction log_function>
class EventMarkWithLogFunction : public EventMarkBase {
  StringLogMessage _buffer;

 public:
  // log a begin event, format as printf
  EventMarkWithLogFunction(const char* format, ...) ATTRIBUTE_PRINTF(2, 3) :
      EventMarkBase(log_function) {
    if (LogEvents) {
      va_list ap;
      va_start(ap, format);
      log_start(format, ap);
      va_end(ap);
    }
  }
  // log an end event
  ~EventMarkWithLogFunction() {
    if (LogEvents) {
      log_end();
    }
  }
};

// These end up in the default log.
typedef EventMarkWithLogFunction<Events::log> EventMark;

// These end up in the vm_operation log.
typedef EventMarkWithLogFunction<Events::log_vm_operation> EventMarkVMOperation;

#endif // SHARE_UTILITIES_EVENTS_HPP
